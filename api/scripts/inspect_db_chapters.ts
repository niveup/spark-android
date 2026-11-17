import "dotenv/config";
import { db } from "../src/lib/db";

const BOOK_ID = "cmv_69aa5debeffde2a100b0";

async function main() {
  const items = await db.feedItem.findMany({
    where: { bookId: BOOK_ID },
    orderBy: { createdAt: "asc" },
  });

  console.log(`Total FeedItems for 48 Laws of Power: ${items.length}`);
  for (const it of items) {
    console.log(`[${it.id}] hash: "${it.contentHash}" | title: "${it.title}" | audio: ${it.audioUrl ? it.audioUrl.substring(0, 45) : 'NULL'}`);
  }
}

main().finally(() => db.$disconnect());
