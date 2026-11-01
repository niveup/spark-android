// One-off polish for published growth rows after builder fixes:
//  - bartletts quotes: unwrap edge quotes defensively (idempotent)
//  - gutenberg book_idea: strip `_` emphasis markers + end at the last
//    sentence boundary (also for stubs/slug tails like "power of thought. I")
// Text-only, additive, dev-only. Idempotent — safe to re-run.
// Run: npx tsx prisma/polish-growth-text.ts
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { stripEdgeQuotes } from "../src/lib/text";

if (process.env.NODE_ENV === "production") {
  throw new Error("Refusing to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

// End mid-sentence (or even mid-word)? Cut back to the last completed
// sentence inside the text. Falls back to the trimmed text when nothing
// sensible is found.
function sentenceTrim(text: string): string {
  const t = text.trim();
  if (/[.!?]$/.test(t)) return t;
  const end = Math.max(t.lastIndexOf(". "), t.lastIndexOf("! "), t.lastIndexOf("? "));
  if (end > 100) return t.slice(0, end + 1);
  return t;
}

const HEADER = "Opening lines of this public-domain classic:";

const quotes = await db.feedItem.findMany({
  where: { status: "published", sourceType: "bartletts-1905" },
  select: { id: true, bodyMd: true },
});
let qFixed = 0;
for (const q of quotes) {
  const cleaned = stripEdgeQuotes(q.bodyMd);
  const body = `"${cleaned}"`;
  if (body !== q.bodyMd) {
    await db.feedItem.update({ where: { id: q.id }, data: { bodyMd: body } });
    qFixed++;
  }
}

const ideas = await db.feedItem.findMany({
  where: { status: "published", sourceType: "gutenberg" },
  select: { id: true, bodyMd: true },
});
let iFixed = 0;
for (const i of ideas) {
  const idx = i.bodyMd.indexOf(HEADER);
  if (idx < 0) continue;
  const excerpt = i.bodyMd
    .slice(idx + HEADER.length)
    .replace(/\s+/g, " ")
    .trim()
    .replace(/_/g, "");
  const fixed = `${HEADER}\n\n${sentenceTrim(excerpt)}`;
  if (fixed !== i.bodyMd) {
    await db.feedItem.update({ where: { id: i.id }, data: { bodyMd: fixed } });
    iFixed++;
  }
}

console.log(`Quotes rewrapped: ${qFixed}/${quotes.length}`);
console.log(`Book-idea excerpts polished: ${iFixed}/${ideas.length}`);
await db.$disconnect();
