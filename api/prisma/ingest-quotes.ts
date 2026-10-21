// Ingest the curated quote deck (owner decision, Oct 2026 — option A).
//
// Every quote card comes from a book SPARK already recommends. The quote text
// is pulled VERBATIM from that book's Gutenberg text at ingest time: this file
// only holds a `match` prefix, so a quote can never drift away from the book
// it claims to come from. A missing match is a hard error — the ingest fails
// instead of publishing a paraphrase.
//
// The action line is written per quote (not per author), so the "Try this
// today" line always matches the sentence above it.
//
// Usage: npx tsx prisma/ingest-quotes.ts [--publish]
import "dotenv/config";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { gutenbergBody, sleep, withRetry } from "../src/lib/adapters";
import { findQuoteSentence, normalizeWhitespace } from "../src/lib/gutenbergQuotes";
import { runIngestJob } from "../src/lib/ingest";
import type { BookEntry } from "./growth-types";

if (process.env.NODE_ENV === "production") {
  throw new Error("ingest-quotes refuses to run in production (dev DB only).");
}

const here = dirname(fileURLToPath(import.meta.url));
const booksPath = resolve(here, "data/growth-books.json");
const quotesPath = resolve(here, "data/growth-quotes.json");
const entries = (JSON.parse(readFileSync(booksPath, "utf-8")) as { entries: BookEntry[] }).entries;
const quotes = (
  JSON.parse(readFileSync(quotesPath, "utf-8")) as {
    quotes: {
      slug: string;
      authorName: string;
      chapter: string;
      match: string;
      actionLine: string;
      topicTags: string[];
    }[];
  }
).quotes;

const publishNow = process.argv.includes("--publish");

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

type Card = {
  type: string;
  title: string;
  bodyMd: string;
  actionLine: string;
  topicTags: string[];
  language: string;
  estSeconds: number;
  sourceType: string;
  sourceRef: string;
  license: string;
  isAiGenerated: boolean;
  isGrounded: boolean;
  modelUsed: string | null;
  quiz: null;
  audioUrl: null;
  durationMs: null;
};

async function main(): Promise<void> {
  const bySlug = new Map(entries.map((e) => [e.slug, e]));
  const cards: Card[] = [];
  const failures: string[] = [];
  const bookIdBySlug = new Map<string, string>();

  for (const slug of [...new Set(quotes.map((q) => q.slug))]) {
    const entry = bySlug.get(slug);
    if (entry === undefined || entry.gutenbergId === null) {
      failures.push(`${slug}: book not in growth-books.json`);
      continue;
    }
    const raw = await withRetry(() => gutenbergBody(entry.gutenbergId ?? 0, 700000));
    if (raw === null) {
      failures.push(`${slug}: Gutenberg #${entry.gutenbergId} unreachable`);
      continue;
    }
    const normalized = normalizeWhitespace(raw);

    for (const q of quotes.filter((x) => x.slug === slug)) {
      const sentence = findQuoteSentence(normalized, q.match);
      if (sentence === null) {
        failures.push(`${slug}: no verbatim sentence starting "${q.match}" in Gutenberg #${entry.gutenbergId}`);
        continue;
      }
      cards.push({
        type: "quote",
        title: q.authorName.slice(0, 80),
        bodyMd: sentence.slice(0, 600),
        actionLine: q.actionLine.slice(0, 200),
        topicTags: q.topicTags.slice(0, 5),
        language: "en",
        estSeconds: 45,
        sourceType: "gutenberg-book",
        sourceRef:
          `${entry.title} (${entry.author}), ${q.chapter} — Gutenberg #${entry.gutenbergId}`.slice(0, 300),
        license: "public domain",
        // The quote itself is verbatim from the book (grounded); the action
        // line is written, so the card is flagged as AI-assisted.
        isAiGenerated: true,
        isGrounded: true,
        modelUsed: null,
        quiz: null,
        audioUrl: null,
        durationMs: null,
      });
    }
    await sleep(700); // Gutenberg etiquette
  }

  if (failures.length > 0) {
    console.error("FAILED to source every quote:");
    for (const f of failures) console.error(`  - ${f}`);
    await db.$disconnect();
    process.exit(1);
  }

  console.log(`sourced ${cards.length}/${quotes.length} quotes verbatim from their books.`);
  for (const c of cards) console.log(`  [${c.sourceType}] ${c.title}: ${c.bodyMd.slice(0, 90)}…`);

  const result = await runIngestJob("growth-quotes:gutenberg-book", cards);
  console.log(
    `ingest: created=${result.created} dupes=${result.duplicates} rejected=${result.rejected}`,
  );

  // Link each quote to its Book row so the app's "Why this?" shows the book.
  // Match on the quote text (unique per card) — matching on the author name
  // would be wrong as soon as one author has two books.
  for (const c of cards) {
    const q = quotes.find((x) => x.actionLine === c.actionLine);
    if (q === undefined) continue;
    const entry = bySlug.get(q.slug);
    if (entry === undefined) continue;
    const existing = await db.book.findFirst({
      where: { source: "gutenberg", sourceId: String(entry.gutenbergId) },
    });
    if (existing === null) continue;
    const item = await db.feedItem.findFirst({
      where: { sourceType: "gutenberg-book", bodyMd: c.bodyMd },
    });
    if (item === null || item.bookId !== null) continue;
    await db.feedItem.update({ where: { id: item.id }, data: { bookId: existing.id } });
  }

  if (publishNow) {
    const drafts = await db.feedItem.findMany({
      where: { status: "draft", sourceType: "gutenberg-book" },
    });
    const published = await db.feedItem.updateMany({
      where: { id: { in: drafts.map((d) => d.id) }, status: "draft" },
      data: { status: "published", publishedAt: new Date() },
    });
    console.log(`published ${published.count} quote card(s).`);
  } else {
    console.log("drafts created — run npm run ingest:publish (or pass --publish) to publish them.");
  }
}

await main();
await db.$disconnect();
