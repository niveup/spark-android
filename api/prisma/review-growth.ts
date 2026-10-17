// Owner review helper: prints every pending growth draft with its data cells
// so the human can eyeball before publishing (Phase 3 gate). Read-only.
// Usage: npx tsx prisma/review-growth.ts
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

if (process.env.NODE_ENV === "production") {
  console.log("NOTE: running against production DB — careful.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  const drafts = await db.feedItem.findMany({
    where: { status: "draft" },
    orderBy: [{ sourceType: "asc" }, { createdAt: "asc" }],
  });
  console.log(`Pending drafts: ${drafts.length}\n`);
  for (const d of drafts) {
    const audio = d.audioUrl !== null ? ` | audio=${d.audioUrl.slice(0, 60)}…` : "";
    console.log(
      `[${d.sourceType}/${d.type}] ${d.title}` +
        `${audio}\n    tags=${d.topicTags.join(",")} est=${d.estSeconds}s dur=${d.durationMs}\n    src=${d.sourceRef}\n    body: ${d.bodyMd.slice(0, 140).replace(/\n/g, " ")}`,
    );
  }
}

await main();
await db.$disconnect();
