// Phase 3b dev bootstrap for the growth library (legacy fiction/GATE removed).
// Real library loads come from api/prisma/ingest-growth.ts (pinned IDs +
// live LibriVox/Bartlett's/Wikipedia pulls). This script stays as a search-
// based fallback bootstrap: it resolves growth classics via Gutendex, then
// ingests the same hand-written deck as seed.ts (./seed-deck.ts) so the two
// paths cannot drift into near-duplicate cards. DEV only.
// Usage: npx tsx prisma/seed-pipeline.ts  (needs DATABASE_URL in api/.env)
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { gutendexSearch } from "../src/lib/adapters";
import { runIngestJob } from "../src/lib/ingest";
import { SEED_DECK } from "./seed-deck";
import { contentHash } from "../src/lib/cards";

if (process.env.NODE_ENV === "production") {
  throw new Error("Pipeline seed refuses to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

const BOOK_QUERIES = [
  "as a man thinketh",
  "how to live on 24 hours a day",
  "meditations",
  "self help samuel smiles",
  "the game of life and how to play it",
];

async function main(): Promise<void> {
  // 1. Resolve growth classics via Gutendex (search fallback; pipeline pins IDs).
  for (const q of BOOK_QUERIES) {
    const found = await gutendexSearch(q, 3);
    const pick = found.find((b) => b.textUrl) ?? found[0];
    if (!pick) {
      console.log(`No book for "${q}", skipped.`);
      continue;
    }
    const existing = await db.book.findFirst({
      where: { source: "gutenberg", sourceId: String(pick.gutenbergId) },
    });
    if (!existing) {
      await db.book.create({
        data: {
          source: "gutenberg",
          sourceId: String(pick.gutenbergId),
          title: pick.title,
          author: pick.author,
          language: pick.language,
          coverUrl: pick.coverUrl,
          isPublicDomain: true,
          textUrl: pick.textUrl,
          license: "public domain",
        },
      });
      console.log(`Book added: ${pick.title}`);
    }
  }

  // 2. Same hand-written deck as seed.ts, through the ingest runner so cards
  // are validated and deduped. Provenance travels with each card; nothing is
  // flattened to "manual" any more.
  const candidates = SEED_DECK.map((item) => ({
    type: item.type,
    title: item.title,
    bodyMd: item.bodyMd,
    actionLine: item.actionLine,
    topicTags: item.topicTags,
    language: "en",
    estSeconds: 45,
    sourceType: item.sourceType,
    sourceRef: item.sourceRef,
    license: item.license,
    isAiGenerated: item.isAiGenerated,
    isGrounded: item.isGrounded,
    modelUsed: null,
    quiz: item.quizJson ?? null,
    audioUrl: item.audioUrl ?? null,
    durationMs: item.durationMs ?? null,
  }));
  const result = await runIngestJob("growth-manual-v1", candidates);
  console.log(`Growth ingest: ${result.created} created, ${result.duplicates} dupes, ${result.rejected} rejected.`);

  // 3. Publish only the rows this run created. Previously this published every
  // draft carrying the "growth deck v1" sourceRef, including unrelated rows.
  const hashes = candidates.map((c) => contentHash(c.type, c.title, c.bodyMd));
  const published = await db.feedItem.updateMany({
    where: { status: "draft", contentHash: { in: hashes } },
    data: { status: "published", publishedAt: new Date() },
  });
  console.log(`Published ${published.count} growth cards.`);
}

await main();
await db.$disconnect();
