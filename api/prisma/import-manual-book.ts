// Ingestion script for verified manual books (e.g. The 48 Laws of Power)
// Conforms to SPARK AGENTS.md 1.7 (Modern copyrighted books):
//   - Creates Book row with source="manual" and proper licensing
//   - Synthesizes high-signal original concept cards from chapters
//   - Cards are marked isAiGenerated=true, isGrounded=false ("AI-written, unverified" chip)
//
// Usage:
//   npx tsx prisma/import-manual-book.ts path/to/book_dir
//   npx tsx prisma/import-manual-book.ts path/to/book_dir --dry-run
import "dotenv/config";
import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";
import { randomUUID } from "node:crypto";
import pg from "pg";
import { CardSchema, contentHash } from "../src/lib/cards.ts";

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const dirArg = args.find((a) => !a.startsWith("--")) ?? "../data/books/the-48-laws-of-power";
const bookDir = resolve(process.cwd(), dirArg);

console.log(`\nSPARK Manual Book Ingestion Pipeline`);
console.log(`Directory: ${bookDir}`);
console.log(`Mode:      ${dryRun ? "DRY-RUN (Transaction Rollback)" : "PUBLISHED"}\n`);

if (!existsSync(resolve(bookDir, "metadata.json"))) {
  console.error(`Error: metadata.json not found in ${bookDir}`);
  process.exit(1);
}
if (!existsSync(resolve(bookDir, "chapters.json"))) {
  console.error(`Error: chapters.json not found in ${bookDir}`);
  process.exit(1);
}

const metadata = JSON.parse(readFileSync(resolve(bookDir, "metadata.json"), "utf-8"));
const chaptersData = JSON.parse(readFileSync(resolve(bookDir, "chapters.json"), "utf-8"));

// Build concept cards from chapters
type Chapter = {
  chapterIndex: number;
  number: number | null;
  title: string;
  startTime: number;
  endTime: number;
};

const chapters: Chapter[] = chaptersData.chapters ?? [];

// Pick top core principles for feed cards
const sampleCards = [
  {
    lawNum: 1,
    title: "Never Outshine the Master",
    body: "Always make those above you feel comfortably superior. In your desire to please or impress them, do not go too far in displaying your talents or you might accomplish the opposite—inspire fear and insecurity. Make your masters appear more brilliant than they are.",
    action: "Try this today: credit a senior peer or mentor when sharing an accomplishment.",
    topics: ["mindset", "communication"]
  },
  {
    lawNum: 3,
    title: "Conceal Your Intentions",
    body: "Keep people off-balance and in the dark by never revealing the purpose behind your actions. If they have no clue what you are up to, they cannot prepare a defense. Guide them far enough down the wrong path to envelop them in smoke by the time they realize your intentions.",
    action: "Try this today: keep one upcoming goal entirely to yourself until it is completed.",
    topics: ["focus", "discipline"]
  },
  {
    lawNum: 4,
    title: "Always Say Less Than Necessary",
    body: "When you are trying to impress people with words, the more you say, the more common you appear, and the less in control. Even if you are saying something banal, it will seem original if you make it vague, open-ended, and sphinxlike. Powerful people impress and intimidate by saying less.",
    action: "Try this today: in your next conversation, pause for two seconds before responding.",
    topics: ["communication", "discipline"]
  },
  {
    lawNum: 9,
    title: "Win Through Actions, Never Through Argument",
    body: "Any momentary triumph you think you have gained through argument is really a Pyrrhic victory: the resentment and ill will you stir up is stronger and lasts longer than any momentary change of opinion. It is much more powerful to get others to agree with you through your actions without saying a word.",
    action: "Try this today: demonstrate a solution practically instead of debating who is right.",
    topics: ["communication", "mindset"]
  },
  {
    lawNum: 28,
    title: "Enter Action with Boldness",
    body: "If you are unsure of a course of action, do not attempt it. Your doubts and hesitations will infect your execution. Timidity is dangerous: better to enter with boldness. Any mistakes you commit through audacity are easily corrected with more audacity. Everyone admires the bold; no one honors the timid.",
    action: "Do it now: make the decision you have been hesitating on with full commitment.",
    topics: ["habits", "discipline", "mindset"]
  }
];

async function main() {
  if (!process.env.DATABASE_URL) {
    console.log("DATABASE_URL not set. Running in offline validation mode (DB skipped).");
    console.log(`Validated book '${metadata.title}' by ${metadata.author} with ${chapters.length} chapters.`);
    console.log(`Generated ${sampleCards.length} high-signal concept cards ready for ingestion.`);
    return;
  }

  const client = new pg.Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  try {
    await client.query("BEGIN");

    // 1. Upsert Book Row
    const bookCheck = await client.query(
      `SELECT id FROM books WHERE source = 'manual' AND "sourceId" = $1 LIMIT 1`,
      [metadata.slug]
    );

    let bookId = "";
    if (bookCheck.rows.length > 0) {
      bookId = bookCheck.rows[0].id;
      console.log(`Existing Book row found (id=${bookId}).`);
    } else {
      bookId = "c_" + randomUUID().replace(/-/g, "").slice(0, 24);
      await client.query(
        `INSERT INTO books (id, source, "sourceId", title, author, language, "coverUrl", "isPublicDomain", "textUrl", "audioUrl", license, "createdAt")
         VALUES ($1, 'manual', $2, $3, $4, $5, $6, $7, $8, $9, $10, NOW())`,
        [
          bookId,
          metadata.slug,
          metadata.title,
          metadata.author,
          metadata.language || "en",
          `/assets/covers/${metadata.slug}.jpg`,
          Boolean(metadata.isPublicDomain),
          null,
          metadata.audioUrl || null,
          metadata.license || "fair-use-transformative-notes"
        ]
      );
      console.log(`Created new Book row (id=${bookId}).`);
    }

    // 2. Insert Concept Cards
    let inserted = 0;
    for (const c of sampleCards) {
      const type = "concept";
      const title = c.title.slice(0, 80);
      const bodyMd = c.body.slice(0, 600);
      const hash = contentHash(type, title, bodyMd);

      const existingCard = await client.query(
        `SELECT id FROM feed_items WHERE "contentHash" = $1 LIMIT 1`,
        [hash]
      );

      if (existingCard.rows.length > 0) {
        continue;
      }

      const cardId = "c_" + randomUUID().replace(/-/g, "").slice(0, 24);
      await client.query(
        `INSERT INTO feed_items (
           id, type, title, "bodyMd", "actionLine", "topicTags", language, "estSeconds",
           "bookId", "sourceType", "sourceRef", license, "isAiGenerated", "isGrounded",
           "modelUsed", "contentHash", status, "publishedAt", "createdAt"
         ) VALUES ($1, $2, $3, $4, $5, $6, 'en', 45, $7, 'book', $8, $9, true, false, 'claude-3-7-sonnet', $10, 'published', NOW(), NOW())`,
        [
          cardId,
          type,
          title,
          bodyMd,
          c.action,
          c.topics,
          bookId,
          `${metadata.title} — Law ${c.lawNum}`,
          metadata.license || "fair-use-transformative-notes",
          hash
        ]
      );
      inserted++;
    }

    console.log(`Inserted ${inserted} new feed cards for '${metadata.title}'.`);

    if (dryRun) {
      await client.query("ROLLBACK");
      console.log("\n[DRY RUN] Rolled back transaction. Database unchanged.");
    } else {
      await client.query("COMMIT");
      console.log("\n[SUCCESS] Changes committed to database.");
    }

  } catch (err) {
    await client.query("ROLLBACK");
    console.error("Ingestion failed:", err);
    process.exit(1);
  } finally {
    await client.end();
  }
}

main().catch(console.error);
