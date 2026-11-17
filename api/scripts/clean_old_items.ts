import "dotenv/config";
import { db } from "../src/lib/db";

async function main() {
  const BOOK_ID = "cmv_69aa5debeffde2a100b0";
  const deleted = await db.feedItem.deleteMany({
    where: { bookId: BOOK_ID },
  });
  console.log(`Cleaned up ${deleted.count} old feed items for 48 Laws.`);
}

main().finally(() => process.exit(0));
