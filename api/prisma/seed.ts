// DEV-ONLY seed: published feed items for an empty DB. Refuses to run in
// production. The real library is loaded by the Phase 3b pipeline
// (api/prisma/ingest-growth.ts) from live public sources; the hand-written
// deck itself lives in ./seed-deck.ts and is shared with seed-pipeline.ts so
// the two bootstraps cannot drift into near-duplicates again.
// Usage: npm run db:seed  (DATABASE_URL in api/.env)
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { SEED_BOOKS, SEED_DECK } from "./seed-deck";
import { contentHash } from "../src/lib/cards";

if (process.env.NODE_ENV === "production") {
  throw new Error("Seed refuses to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  const existing = await db.feedItem.count();
  if (existing > 0) {
    console.log(`Seed skipped: ${existing} items already present.`);
    return;
  }

  // Idempotent per-book too: a user may have books but no cards (or vice versa).
  const books: { id: string }[] = [];
  for (const seedBook of SEED_BOOKS) {
    const found = await db.book.findFirst({
      where: { source: seedBook.source, sourceId: seedBook.sourceId },
    });
    books.push(found ?? (await db.book.create({ data: seedBook })));
  }

  const now = new Date();
  await db.feedItem.createMany({
    data: SEED_DECK.map((item, i) => ({
      type: item.type,
      title: item.title.slice(0, 80),
      bodyMd: item.bodyMd.slice(0, 600),
      actionLine: item.actionLine,
      topicTags: item.topicTags,
      estSeconds: 30 + ((i * 7) % 31),
      bookId: item.bookIdx === undefined ? null : books[item.bookIdx]?.id ?? null,
      sourceType: item.sourceType,
      sourceRef: item.sourceRef,
      license: item.license,
      isAiGenerated: item.isAiGenerated,
      isGrounded: item.isGrounded,
      modelUsed: item.isAiGenerated ? "seed-notes-v1" : null,
      // Real dedupe hashes, not placeholder `dev-seed-N` keys, so a later
      // pipeline run cannot insert a second copy of the same card.
      contentHash: contentHash(item.type, item.title, item.bodyMd),
      quizJson: item.quizJson ?? undefined,
      audioUrl: item.audioUrl ?? null,
      durationMs: item.durationMs ?? null,
      status: "published",
      publishedAt: now,
    })),
  });
  const count = await db.feedItem.count();
  console.log(`Seed done: ${count} published items.`);
}

await main();
await db.$disconnect();
