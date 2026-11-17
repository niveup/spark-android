import "dotenv/config";
import { db } from "../../src/lib/db";

async function verify() {
  const book = await db.book.findUnique({
    where: { id: "cmv_the_laws_of_human_nature" },
    include: {
      items: {
        orderBy: { createdAt: "asc" }
      }
    }
  });

  if (!book) {
    console.error("Book not found!");
    process.exit(1);
  }

  console.log(`=== BOOK VERIFICATION ===`);
  console.log(`ID: ${book.id}`);
  console.log(`Title: ${book.title}`);
  console.log(`Author: ${book.author}`);
  console.log(`Cover URL: ${book.coverUrl}`);
  console.log(`Total Chapters: ${book.items.length}`);

  console.log("\n=== CHAPTERS IN DATABASE ===");
  book.items.forEach((it, idx) => {
    const mins = Math.floor((it.durationMs || 0) / 60000);
    const secs = Math.floor(((it.durationMs || 0) % 60000) / 1000);
    console.log(`  [${String(idx).padStart(2, "0")}] "${it.title}" (${mins}m ${secs}s) -> ${it.audioUrl}`);
  });
}

verify()
  .catch(console.error)
  .finally(() => db.$disconnect());
