// One-off: verify published growth library counts.
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

const byType = await db.feedItem.groupBy({
  by: ["type"],
  where: { status: "published" },
  _count: { type: true },
  _sum: { estSeconds: true },
});
console.log("Published by type:");
for (const t of byType) {
  console.log(`  ${t.type}: ${t._count.type} (total est ${Math.round((t._sum.estSeconds ?? 0) / 60)} min)`);
}
const bySource = await db.feedItem.groupBy({
  by: ["sourceType"],
  where: { status: "published" },
  _count: { sourceType: true },
});
console.log("Published by sourceType:");
for (const s of bySource) console.log(`  ${s.sourceType}: ${s._count.sourceType}`);
const books = await db.book.count();
const booksWithAudio = await db.book.count({ where: { audioUrl: { not: null } } });
console.log(`Books: ${books} (${booksWithAudio} with LibriVox audio)`);
const audioWithUrl = await db.feedItem.count({ where: { status: "published", type: "audio" } });
const audioGood = await db.feedItem.count({ where: { status: "published", type: "audio", audioUrl: { not: null } } });
console.log(`Audio cards: ${audioWithUrl}, with stream URL: ${audioGood}`);
await db.$disconnect();
