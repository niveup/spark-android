import "dotenv/config";
import { db } from "../src/lib/db";

async function main() {
  const books = await db.book.findMany({
    select: { id: true, title: true, coverUrl: true, audioUrl: true }
  });
  console.log("Total books:", books.length);
  for (const b of books) {
    console.log(b.id, "=>", b.title, "| coverUrl:", b.coverUrl);
  }
}

main().finally(() => db.$disconnect());
