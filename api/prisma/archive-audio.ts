// Archive (or restore) the audiobook cards so they leave / re-enter the feed.
//
// Why: the LibriVox chapter cards were pure boilerplate ("Section 3 of the
// public-domain LibriVox reading of …") with one shared action line, and they
// made up roughly a third of the deck. The owner asked for them to be removed.
//
// Safe by design:
//   - Nothing is deleted. Cards keep their rows, their content hash and their
//     saved/liked state; only `status` changes (published <-> archived).
//   - The feed only serves status = 'published', so archived cards disappear
//     from Reels, but a card the owner already saved can come back with
//     --restore.
//   - Additive only: no schema change, no migration, no destructive SQL.
//
// Usage:
//   npx tsx prisma/archive-audio.ts            # archive (default)
//   npx tsx prisma/archive-audio.ts --restore  # publish them again
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

if (process.env.NODE_ENV === "production") {
  throw new Error("archive-audio refuses to run in production (dev DB only).");
}

const restore = process.argv.includes("--restore");
const to = restore ? "published" : "archived";
const from = restore ? "archived" : "published";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  const before = await db.feedItem.groupBy({
    by: ["type", "status"],
    where: { type: "audio" },
    _count: { _all: true },
  });
  console.log("audio cards before:");
  for (const row of before) console.log(`  ${row.status}: ${row._count._all}`);

  const result = await db.feedItem.updateMany({
    where: { type: "audio", status: from },
    data: { status: to },
  });
  console.log(`\n${result.count} card(s) moved ${from} -> ${to}.`);

  const after = await db.feedItem.groupBy({
    by: ["type", "status"],
    where: { type: "audio" },
    _count: { _all: true },
  });
  console.log("audio cards after:");
  for (const row of after) console.log(`  ${row.status}: ${row._count._all}`);

  const deck = await db.feedItem.groupBy({
    by: ["type"],
    where: { status: "published" },
    _count: { _all: true },
  });
  console.log("\npublished deck by type:");
  for (const row of deck.sort((a, b) => b._count._all - a._count._all)) {
    console.log(`  ${row.type}: ${row._count._all}`);
  }
}

await main();
await db.$disconnect();
