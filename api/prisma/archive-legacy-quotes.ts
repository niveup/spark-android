// Archive the legacy quote cards (Bartlett's 1905 anthology, the hand-written
// seed quotes, and the "wikiquote" cards whose text was typed from memory
// rather than fetched).
//
// Owner decision (Oct 2026): quotes now come from the books SPARK recommends
// (see prisma/data/growth-quotes.json + ingest-quotes.ts). The old cards are
// archived, not deleted — status only, nothing removed, saved state kept.
//
// Safe by design:
//   - Nothing is deleted. Cards keep their rows, content hash and saved state;
//     only `status` changes.
//   - The feed serves status = 'published' only, so archived quotes disappear
//     from Reels and from the Saved list.
//   - --restore puts them back.
//
// Usage:
//   npm run content:archive-legacy-quotes              # archive (default)
//   npm run content:archive-legacy-quotes -- --restore # publish them again
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

if (process.env.NODE_ENV === "production") {
  throw new Error("archive-legacy-quotes refuses to run in production (dev DB only).");
}

const restore = process.argv.includes("--restore");
const to = restore ? "published" : "archived";
const from = restore ? "archived" : "published";
// The new deck lives under sourceType 'gutenberg-book' and is never touched.
const legacy = { type: "quote", NOT: { sourceType: "gutenberg-book" } };

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  const before = await db.feedItem.groupBy({
    by: ["sourceType", "status"],
    where: { type: "quote" },
    _count: { _all: true },
  });
  console.log("quote cards before:");
  for (const row of before) console.log(`  ${row.sourceType} / ${row.status}: ${row._count._all}`);

  const result = await db.feedItem.updateMany({
    where: { ...legacy, status: from },
    data: { status: to },
  });
  console.log(`\n${result.count} legacy quote card(s) moved ${from} -> ${to}.`);

  const deck = await db.feedItem.groupBy({
    by: ["type", "sourceType"],
    where: { status: "published" },
    _count: { _all: true },
  });
  console.log("\npublished deck by type/source:");
  for (const row of deck.sort((a, b) => b._count._all - a._count._all)) {
    console.log(`  ${row.type} / ${row.sourceType}: ${row._count._all}`);
  }
}

await main();
await db.$disconnect();
