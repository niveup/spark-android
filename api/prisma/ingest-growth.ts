// Phase 3b ingest: builds the growth library from LIVE public sources.
// Sources (all free + legal, AGENTS.md 1.7):
//   - Books:      pinned Gutenberg/LibriVox IDs from data/growth-books.json
//                 (IDs cross-verified live via LibriVox url_text_source links)
//   - Audio:      LibriVox extended API -> per-chapter MP3 URLs (streamed by
//                 the app later; nothing audio-shaped is stored here)
//   - Quotes:     Bartlett's Familiar Quotations 1905 (PG #27889) plain text,
//                 parsed to author->quote pairs (transient, in-memory only)
//   - Concepts:   Wikipedia REST summaries (CC BY-SA, attribution in sourceRef)
//   - Book ideas: Gutenberg plain-text openings (transient, in-memory only)
// All output goes through runIngestJob (zod -> dedupe -> drafts -> job log).
// No big files are saved anywhere: text is fetched, used, and thrown away.
//
// Usage: npx tsx prisma/ingest-growth.ts  (needs DATABASE_URL in api/.env)
import "dotenv/config";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import {
  contentHash,
} from "../src/lib/cards";
import { runIngestJob } from "../src/lib/ingest";
import {
  gutenbergBody,
  librivoxBook,
  sleep,
  wikiSummary,
  withRetry,
} from "../src/lib/adapters";
import { parseBartlettQuotes } from "../src/lib/bartlett";
import {
  bookIdeaCandidates,
  chapterCandidates,
  conceptCandidates,
  quoteCandidates,
  type GrowthCandidate,
} from "../src/lib/growth";
import type { BookEntry, GrowthConfig } from "./growth-types";

if (process.env.NODE_ENV === "production") {
  throw new Error("Growth ingest refuses to run in production (dev DB only).");
}

const here = dirname(fileURLToPath(import.meta.url));
const booksPath = resolve(here, "data/growth-books.json");
const configPath = resolve(here, "data/growth-config.json");
const entries = (JSON.parse(readFileSync(booksPath, "utf-8")) as { entries: BookEntry[] }).entries;
const config = JSON.parse(readFileSync(configPath, "utf-8")) as GrowthConfig;

const AUDIO_CHAPTERS_PER_BOOK = Math.max(1, Number(process.env.AUDIO_CHAPTERS_PER_BOOK ?? 4));
const QUOTES_PER_AUTHOR = Math.max(1, Number(process.env.QUOTES_PER_AUTHOR ?? 3));
const BARTLETT_URL = "https://www.gutenberg.org/cache/epub/27889/pg27889.txt";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

function report(label: string, r: { created: number; duplicates: number; rejected: number }): void {
  console.log(`${label}: created=${r.created} dupes=${r.duplicates} rejected=${r.rejected}`);
}

async function linkBookIds(candidates: GrowthCandidate[], bookIdBySlug: Map<string, string>): Promise<void> {
  for (const c of candidates) {
    if (c.bookSlug === null) continue;
    const bookId = bookIdBySlug.get(c.bookSlug);
    if (bookId === undefined) continue;
    const card = c.card as { type: string; title: string; bodyMd: string };
    const hash = contentHash(card.type, card.title, card.bodyMd);
    const item = await db.feedItem.findUnique({ where: { contentHash: hash } });
    if (item && item.bookId === null) {
      await db.feedItem.update({ where: { id: item.id }, data: { bookId } });
    }
  }
}

async function runGroup(
  source: string,
  candidates: GrowthCandidate[],
  bookIdBySlug: Map<string, string>,
): Promise<void> {
  const cards = candidates.map((c) => c.card);
  report(source, await runIngestJob(source, cards));
  await linkBookIds(candidates, bookIdBySlug);
}

async function main(): Promise<void> {
  // ---- 1. Book rows (create-if-missing; additive only) ----
  const bookIdBySlug = new Map<string, string>();
  for (const e of entries) {
    if (e.gutenbergId === null) {
      console.log(`book ${e.slug}: no gutenberg id pinned, skipping book row.`);
      continue;
    }
    const existing = await db.book.findFirst({
      where: { source: "gutenberg", sourceId: String(e.gutenbergId) },
    });
    if (existing) {
      bookIdBySlug.set(e.slug, existing.id);
      continue;
    }
    const created = await db.book.create({
      data: {
        source: "gutenberg",
        sourceId: String(e.gutenbergId),
        title: e.title,
        author: e.author,
        language: "en",
        isPublicDomain: true,
        textUrl: `https://www.gutenberg.org/cache/epub/${e.gutenbergId}/pg${e.gutenbergId}.txt`,
        license: "public domain",
      },
    });
    bookIdBySlug.set(e.slug, created.id);
    console.log(`book created: ${e.title} (#${e.gutenbergId})`);
  }

  // ---- 2. Audiobook chapters (streamed MP3 URLs from LibriVox) ----
  for (const e of entries) {
    if (e.librivoxId === null) continue;
    let lvx = null;
    try {
      lvx = await withRetry(() => librivoxBook(e.librivoxId ?? ""));
    } catch (err) {
      console.log(`librivox ${e.librivoxId} (${e.slug}) failed: ${String(err).slice(0, 120)}`);
      continue;
    }
    if (lvx === null) {
      console.log(`librivox ${e.librivoxId} (${e.slug}): no data.`);
      continue;
    }
    const bookId = bookIdBySlug.get(e.slug);
    if (bookId !== undefined && lvx.zipUrl !== null) {
      await db.book.update({ where: { id: bookId }, data: { audioUrl: lvx.zipUrl } });
    }
    const candidates = chapterCandidates(e, lvx, AUDIO_CHAPTERS_PER_BOOK);
    await runGroup(`growth-chapters:${e.slug}`, candidates, bookIdBySlug);
    await sleep(900); // polite to LibriVox between books
  }

  // ---- 3. Quotes from Bartlett's 1905 (transient fetch, in-memory parse) ----
  try {
    const res = await fetch(BARTLETT_URL, {
      headers: { "User-Agent": "spark-dev-ingest/0.1 (personal app; contact local)" },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const text = await res.text();
    const quotes = parseBartlettQuotes(
      text,
      config.quotes.map((q) => q.match),
      QUOTES_PER_AUTHOR,
    );
    console.log(`bartletts-1905: parsed ${quotes.length} quotes for ${config.quotes.length} targets.`);
    await runGroup("growth-quotes", quoteCandidates(config.quotes, quotes, QUOTES_PER_AUTHOR), bookIdBySlug);
  } catch (err) {
    console.log(`bartletts-1905 failed: ${String(err).slice(0, 160)}`);
  }

  // ---- 4. Concept cards from Wikipedia summaries ----
  const summaries = [];
  for (const c of config.concepts) {
    try {
      const s = await withRetry(() => wikiSummary(c.wiki));
      summaries.push(s);
      console.log(`wiki ${c.wiki}: ${s === null ? "NOT FOUND — skipped" : "ok"}`);
    } catch (err) {
      console.log(`wiki ${c.wiki} failed: ${String(err).slice(0, 120)}`);
      summaries.push(null);
    }
    await sleep(600); // Wikimedia etiquette
  }
  await runGroup("growth-concepts", conceptCandidates(config.concepts, summaries), bookIdBySlug);

  // ---- 5. Book-idea cards from Gutenberg openings (transient) ----
  for (const e of entries) {
    if (e.gutenbergId === null) continue;
    try {
      const opening = await withRetry(() => gutenbergBody(e.gutenbergId ?? 0, 1400));
      await runGroup(`growth-ideas:${e.slug}`, bookIdeaCandidates(e, opening), bookIdBySlug);
    } catch (err) {
      console.log(`gutenberg ${e.gutenbergId} (${e.slug}) failed: ${String(err).slice(0, 120)}`);
    }
    await sleep(700); // Gutenberg etiquette
  }

  // ---- 6. Draft summary for owner eyeballing ----
  const drafts = await db.feedItem.groupBy({
    by: ["type"],
    where: { status: "draft", sourceType: { in: ["librivox", "bartletts-1905", "wikipedia", "gutenberg"] } },
    _count: { type: true },
  });
  console.log("\nDraft counts by type:");
  for (const d of drafts) console.log(`  ${d.type}: ${d._count.type}`);
  const samples = await db.feedItem.findMany({
    where: { status: "draft", sourceType: { in: ["librivox", "bartletts-1905", "wikipedia", "gutenberg"] } },
    orderBy: { createdAt: "desc" },
    take: 12,
  });
  console.log("\nSample drafts (newest 12):");
  for (const s of samples) {
    console.log(`  [${s.type}] ${s.title} (${s.estSeconds}s, tags=${s.topicTags.join(",")})`);
  }
}

await main();
await db.$disconnect();
