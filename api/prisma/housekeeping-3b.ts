// Phase 3b housekeeping (DEV only, reviewed):
// 1. Deletes REJECTED growth drafts: book-idea extracts that captured
//    Gutenberg front-matter junk, and quote drafts still carrying footnote
//    markers. These are pre-publish pipeline junk (never user-visible).
// 2. Archives legacy theme-pivot demo rows (GATE thermo/SOM deck, fiction
//    classics with their pre-3b audio cards) so the feed becomes pure growth
//    content. Archive = status "archived", reversible, nothing is deleted.
// Usage: npx tsx prisma/housekeeping-3b.ts
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

if (process.env.NODE_ENV === "production") {
  throw new Error("Housekeeping-3b refuses to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

async function main(): Promise<void> {
  // --- 1. Junk drafts (status must still be "draft") ---
  const selfHelpJunk = await db.feedItem.findMany({
    where: {
      status: "draft",
      sourceType: "gutenberg",
      title: "Why read Self Help; with Illustrations of Conduct and Perseverance",
      bodyMd: { contains: "Project Gutenberg eBook" },
    },
  });
  const quoteDrafts = await db.feedItem.findMany({
    where: { status: "draft", sourceType: "bartletts-1905" },
  });
  const refJunk = quoteDrafts.filter((d) => /\[\s*\d+(?:\s*-\s*\d+)?\s*\]/.test(d.bodyMd));
  const junk = [...selfHelpJunk, ...refJunk];
  if (junk.length > 0) {
    await db.feedItem.deleteMany({
      where: { id: { in: junk.map((d) => d.id) }, status: "draft" },
    });
    console.log(`Deleted ${junk.length} junk drafts (pre-publish, pipeline-rejected):`);
    for (const d of junk) console.log(`  - ${d.sourceType}/${d.type}: ${d.title}`);
  } else {
    console.log("No junk drafts found.");
  }

  // --- 2. Draft-version dedupe: keep the NEWEST attempt per book_idea
  // (title encodes the single book; extractor improvements created variants).
  const ideaVersions = await db.feedItem.findMany({
    where: { status: "draft", type: "book_idea", sourceType: "gutenberg" },
    orderBy: { createdAt: "asc" },
  });
  const keep = new Map<string, string>();
  const staleIds: string[] = [];
  for (const d of ideaVersions) {
    const older = keep.get(d.title);
    if (older !== undefined) staleIds.push(older);
    keep.set(d.title, d.id);
  }
  if (staleIds.length > 0) {
    await db.feedItem.deleteMany({
      where: { id: { in: staleIds }, status: "draft" },
    });
    console.log(`Deleted ${staleIds.length} superseded book_idea draft versions.`);
  } else {
    console.log("No superseded book_idea drafts.");
  }

  // --- 3. Archive legacy GATE/fiction demo rows (reversible) ---
  const legacy = await db.feedItem.findMany({
    where: {
      status: "published",
      OR: [
        { sourceRef: { contains: "GATE" } },
        { sourceRef: "P&P" },
        { sourceRef: "Moby Dick" },
        { sourceType: "gutenberg", sourceRef: { in: ["ebook #1342", "ebook #84", "ebook #1661"] } },
      ],
    },
  });
  if (legacy.length > 0) {
    await db.feedItem.updateMany({
      where: { id: { in: legacy.map((d) => d.id) } },
      data: { status: "archived" },
    });
    console.log(`Archived ${legacy.length} legacy demo items (reversible):`);
    for (const d of legacy) console.log(`  ~ ${d.type}: ${d.title}`);
  } else {
    console.log("No legacy demo items found.");
  }
}

await main();
await db.$disconnect();
