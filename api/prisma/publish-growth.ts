// Owner-approved publish step for growth-library drafts (Phase 3 gate).
// Only touches drafts whose sourceType belongs to the verified-phase sources.
// Usage: npx tsx prisma/publish-growth.ts   (prints, then publishes)
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

const SOURCES = ["librivox", "bartletts-1905", "wikipedia", "gutenberg"];

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  const drafts = await db.feedItem.findMany({
    where: { status: "draft", sourceType: { in: SOURCES } },
    orderBy: [{ sourceType: "asc" }, { createdAt: "asc" }],
  });
  if (drafts.length === 0) {
    console.log("No growth drafts to publish.");
    return;
  }
  console.log(`Publishing ${drafts.length} drafts:`);
  for (const d of drafts) {
    console.log(`  [${d.sourceType}/${d.type}] ${d.title}`);
  }
  const published = await db.feedItem.updateMany({
    where: { id: { in: drafts.map((d) => d.id) }, status: "draft" },
    data: { status: "published", publishedAt: new Date() },
  });
  console.log(`Published ${published.count} items.`);
}

await main();
await db.$disconnect();
