// AI -> Supabase card importer. This is the "no code after setup" path.
//
// The loop it closes:
//   1. a good AI fills in api/prisma/inbox/cards.json (format + prompt: docs/AI_CONTENT_PROMPT.md)
//   2. this script validates it, verifies the claims it can, and inserts it
//   3. the phone shows it (the feed already reads status='published' from Supabase)
//
// One rule it enforces for you: an AI may CLAIM a quote but it may not CERTIFY
// one. A quote card must carry `gutenbergId` + `match`; the sentence is pulled
// from the real Gutenberg text and a miss is a rejection. Everything the AI
// writes on its own is stored as isAiGenerated=true / isGrounded=false, which
// is what makes the app show the "AI-written, unverified" chip.
//
// Usage (from api/):
//   node prisma/import-cards.ts                      # imports prisma/inbox/cards.json
//   node prisma/import-cards.ts path/to/cards.json   # any other JSON file
//   node prisma/import-cards.ts --draft              # hold as drafts instead of publishing
//   node prisma/import-cards.ts --dry-run            # full run, then ROLLBACK (nothing saved)
//   node prisma/import-cards.ts --summary out.json   # machine-readable result (CI/Telegram)
//
// Plain .ts run with plain `node` (Node 24 strips the types) so it works
// without the tsx/esbuild runner; `npx tsx prisma/import-cards.ts` works too.
import "dotenv/config";
import { readFileSync, writeFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import pg from "pg";
import { CardSchema, contentHash } from "../src/lib/cards.ts";
import { gutenbergBody, sleep, withRetry } from "../src/lib/adapters.ts";
import { findQuoteSentence, normalizeWhitespace } from "../src/lib/gutenbergQuotes.ts";

if (process.env.NODE_ENV === "production") {
  // Intentional: production imports go through the deployed API, not a laptop
  // script holding DATABASE_URL. Remove only when a real worker owns this.
  throw new Error("import-cards refuses to run in production (dev DB only).");
}

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const asDraft = args.includes("--draft");
// --summary takes a value, so it must be removed before the file path is picked.
const summaryAt = args.indexOf("--summary");
const summaryPath = summaryAt >= 0 ? args[summaryAt + 1] : undefined;
const file =
  args.find((a, i) => !a.startsWith("--") && (summaryAt < 0 || i !== summaryAt + 1)) ??
  "prisma/inbox/cards.json";

const STATUS = asDraft ? "draft" : "published";

type InboxCard = {
  type?: string;
  title?: string;
  body?: string;
  action?: string;
  topics?: string[];
  person?: string;
  work?: string;
  chapter?: string;
  gutenbergId?: number;
  match?: string;
};

type Accepted = { card: Record<string, unknown>; bookGutenbergId: number | null };
type Rejected = { title: string; reason: string };
type SummaryCard = {
  status: "stored" | "duplicate" | "rejected" | "error";
  type: string;
  title: string;
  reason?: string;
  body?: string;
  grounded?: boolean;
};

const db = new pg.Client({ connectionString: process.env.DATABASE_URL });

function clip(value: string | undefined, max: number): string {
  return (value ?? "").trim().slice(0, max);
}

/** Verify a claimed quote against the actual Gutenberg text. */
async function verifyQuote(input: InboxCard): Promise<{ sentence: string } | { reason: string }> {
  const id = input.gutenbergId;
  if (typeof id !== "number" || !Number.isInteger(id) || id <= 0) {
    return { reason: "quote without a gutenbergId cannot be verified — add the Project Gutenberg id" };
  }
  if (typeof input.match !== "string" || input.match.trim().length < 8) {
    return { reason: "quote without a `match` prefix cannot be verified — give the first words of the sentence" };
  }
  const raw = await withRetry(() => gutenbergBody(id, 900000));
  if (raw === null) return { reason: `Project Gutenberg #${id} not found` };
  const sentence = findQuoteSentence(normalizeWhitespace(raw), input.match);
  if (sentence === null) {
    return { reason: `"${input.match.slice(0, 40)}…" is not verbatim in Gutenberg #${id} — invented or misquoted` };
  }
  return { sentence };
}

async function accept(input: InboxCard): Promise<Accepted | Rejected> {
  const type = String(input.type ?? "").trim();
  const topics = (input.topics ?? []).filter((t) => typeof t === "string" && t.trim().length > 0);
  const label = clip(input.title, 80) || clip(input.person, 80) || "(untitled)";

  if (type === "quote") {
    const verified = await verifyQuote(input);
    if ("reason" in verified) return { title: label, reason: verified.reason };
    const work = clip(input.work, 120) || "a public-domain text";
    const person = clip(input.person, 80) || "Unknown";
    const chapter = clip(input.chapter, 80);
    const card = {
      type: "quote",
      title: person,
      bodyMd: verified.sentence,
      actionLine: clip(input.action, 200),
      topicTags: topics,
      language: "en",
      estSeconds: 45,
      sourceType: "gutenberg-book",
      sourceRef: `${work} (${person})${chapter ? `, ${chapter}` : ""} — Gutenberg #${input.gutenbergId}`,
      license: "public domain",
      // The sentence is verbatim (grounded); the action line is written, so the
      // card is still flagged AI-assisted — same convention as ingest-quotes.ts.
      isAiGenerated: true,
      isGrounded: true,
      modelUsed: null,
      bookGutenbergId: input.gutenbergId ?? null,
    };
    return check(card, label, topics);
  }

  // Everything else is original AI text: no author, no source, no pretending.
  const card = {
    type,
    title: clip(input.title, 80),
    bodyMd: clip(input.body, 600),
    actionLine: clip(input.action, 200),
    topicTags: topics,
    language: "en",
    estSeconds: 60,
    sourceType: "ai-original",
    sourceRef: "AI-written original card",
    license: "original",
    isAiGenerated: true,
    isGrounded: false,
    modelUsed: null,
    bookGutenbergId: null,
  };
  return check(card, label, topics);
}

/** Shared validation: the same CardSchema the rest of the pipeline enforces. */
function check(
  raw: Record<string, unknown> & { bookGutenbergId: number | null },
  label: string,
  topics: string[],
): Accepted | Rejected {
  const { bookGutenbergId, ...card } = raw;
  const parsed = CardSchema.safeParse(card);
  if (!parsed.success) {
    const issue = parsed.error.issues[0];
    const where = issue?.path.join(".") || "card";
    return { title: label, reason: `${where}: ${issue?.message ?? "invalid card"}` };
  }
  if (topics.length === 0) return { title: label, reason: "topics: at least one topic tag is required" };
  return { card: parsed.data as Record<string, unknown>, bookGutenbergId };
}

async function main(): Promise<void> {
  const payload = JSON.parse(readFileSync(file, "utf-8")) as InboxCard[] | { cards: InboxCard[] };
  const cards: InboxCard[] = Array.isArray(payload) ? payload : payload.cards;
  if (!Array.isArray(cards) || cards.length === 0) {
    console.error(`${file}: expected a non-empty array, or { "cards": [ … ] }`);
    process.exit(1);
  }

  await db.connect();
  if (dryRun) await db.query("BEGIN");
  console.log(`${file}: ${cards.length} card(s) — mode ${STATUS}${dryRun ? ", DRY RUN (rolled back)" : ""}\n`);

  const created: string[] = [];
  const rejected: Rejected[] = [];
  const duplicates: string[] = [];
  const errors: Rejected[] = [];
  const details: SummaryCard[] = [];

  for (const input of cards) {
    const outcome = await accept(input);
    if ("reason" in outcome) {
      rejected.push(outcome);
      details.push({
        status: "rejected",
        type: String(input.type ?? ""),
        title: outcome.title,
        reason: outcome.reason,
      });
      console.log(`  REJECTED  ${outcome.title}\n            ${outcome.reason}`);
      continue;
    }
    const card = outcome.card;
    const hash = contentHash(String(card.type), String(card.title), String(card.bodyMd));

    // Inside the dry-run transaction one failed statement would poison every
    // later one; a savepoint keeps each card independent.
    if (dryRun) await db.query("SAVEPOINT card");
    try {
      const dupe = await db.query(`select 1 from feed_items where "contentHash" = $1 limit 1`, [hash]);
      if (dupe.rowCount !== null && dupe.rowCount > 0) {
        duplicates.push(String(card.title));
        details.push({ status: "duplicate", type: String(card.type), title: String(card.title) });
        console.log(`  DUPLICATE ${card.title} (already in the deck)`);
        continue;
      }

      let bookId: string | null = null;
      if (outcome.bookGutenbergId !== null) {
        bookId = await ensureBook(outcome.bookGutenbergId, String(card.title), input);
      }

      const id = randomUUID();
      // "on conflict do nothing" is the safety net under the check above: a
      // concurrent run or a retried dispatch skips the row instead of aborting
      // the whole batch on the unique contentHash.
      const inserted = await db.query(
        `insert into feed_items
           (id, type, title, "bodyMd", "actionLine", "topicTags", language, "estSeconds",
            "bookId", "sourceType", "sourceRef", license, "isAiGenerated", "isGrounded",
            "modelUsed", "contentHash", status, "publishedAt")
         values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)
         on conflict ("contentHash") do nothing
         returning id`,
        [
          id, card.type, card.title, card.bodyMd, card.actionLine, card.topicTags, card.language,
          card.estSeconds, bookId, card.sourceType, card.sourceRef, card.license,
          card.isAiGenerated, card.isGrounded, card.modelUsed, hash, STATUS,
          STATUS === "published" ? new Date() : null,
        ],
      );
      if (inserted.rowCount === 0) {
        duplicates.push(String(card.title));
        details.push({ status: "duplicate", type: String(card.type), title: String(card.title) });
        console.log(`  DUPLICATE ${card.title} (claimed by a parallel run)`);
        continue;
      }
      created.push(id);
      details.push({
        status: "stored",
        type: String(card.type),
        title: String(card.title),
        body: String(card.bodyMd).slice(0, 180),
        grounded: Boolean(card.isGrounded),
      });
      const mark = card.isGrounded ? "verified quote " : "original       ";
      console.log(`  ACCEPTED  ${mark}${card.title} → ${STATUS}`);
    } catch (err) {
      if (dryRun) await db.query("ROLLBACK TO SAVEPOINT card").catch(() => undefined);
      // One bad card must not abort the batch — but it must fail the run, so a
      // dead connection can never look like a successful import.
      const reason = `database error: ${String(err).slice(0, 200)}`;
      errors.push({ title: String(card.title), reason });
      details.push({ status: "error", type: String(card.type), title: String(card.title), reason });
      console.log(`  ERROR     ${card.title}\n            ${reason}`);
    }
  }

  // Job log, mirroring runIngestJob so the history stays in one place.
  try {
    await db.query(
      `insert into ingest_jobs (id, source, status, "itemsCreated", "startedAt", "finishedAt")
       values ($1, $2, 'done', $3, now(), now())`,
      [randomUUID(), `ai-inbox:${dryRun ? "dry-run" : STATUS}`, created.length],
    );
  } catch (err) {
    console.log(`job log failed (not fatal): ${String(err).slice(0, 120)}`);
  }

  console.log(
    `\nsummary: ${created.length} stored, ${rejected.length} rejected, ` +
      `${duplicates.length} duplicate(s), ${errors.length} error(s)${dryRun ? ", rolled back" : ""}`,
  );
  if (rejected.length > 0) {
    console.log("rejected cards were NOT dropped silently — fix the JSON and re-run; duplicates are skipped automatically.");
  }

  if (summaryPath !== undefined) {
    writeFileSync(
      summaryPath,
      JSON.stringify(
        {
          received: cards.length,
          stored: created.length,
          rejected: rejected.length,
          duplicates: duplicates.length,
          errors: errors.length,
          mode: STATUS,
          dryRun,
          rejectedCards: rejected,
          duplicateTitles: duplicates,
          errorCards: errors,
          details,
        },
        null,
        2,
      ),
    );
    console.log(`summary written: ${summaryPath}`);
  }

  if (dryRun) await db.query("ROLLBACK");
  await db.end();
  if (errors.length > 0) process.exitCode = 1;
}

/** Find (or create) the books row a verified quote points at. */
async function ensureBook(gutenbergId: number, author: string, input: InboxCard): Promise<string> {
  const existing = await db.query(
    `select id from books where source = 'gutenberg' and "sourceId" = $1 limit 1`,
    [String(gutenbergId)],
  );
  if (existing.rowCount !== null && existing.rowCount > 0) return existing.rows[0].id as string;
  const id = randomUUID();
  await db.query(
    `insert into books (id, source, "sourceId", title, author, language, "isPublicDomain", "textUrl", license, "createdAt")
     values ($1, 'gutenberg', $2, $3, $4, 'en', true, $5, 'public domain', now())`,
    [
      id,
      String(gutenbergId),
      clip(input.work, 200) || `Gutenberg #${gutenbergId}`,
      author,
      `https://www.gutenberg.org/cache/epub/${gutenbergId}/pg${gutenbergId}.txt`,
    ],
  );
  return id;
}

await main().catch(async (err) => {
  console.error(`import failed: ${String(err)}`);
  await db.end().catch(() => undefined);
  process.exitCode = 1;
});
