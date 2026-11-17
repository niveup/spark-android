import "dotenv/config";
import { db } from "../src/lib/db";

async function main() {
  const books = await db.book.findMany({
    where: {
      title: {
        contains: "48 Laws",
        mode: "insensitive",
      },
    },
    include: {
      _count: {
        select: { items: true },
      },
    },
  });

  console.log("Books matching '48 Laws':", JSON.stringify(books, null, 2));
}

main()
  .catch(console.error)
  .finally(() => process.exit(0));
