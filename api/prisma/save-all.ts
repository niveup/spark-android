import "dotenv/config";
import { db } from "../src/lib/db";

async function saveAllBooks() {
  const tokens = await db.deviceToken.findMany({ where: { deviceLabel: { contains: "RMX" } } });
  const books = await db.book.findMany({
    where: { coverUrl: { startsWith: "/api/mobile/v1/covers/" } },
    include: { items: true },
  });

  console.log(`Books with Telegram covers: ${books.length}`);
  for (const t of tokens) {
    for (const b of books) {
      const firstItem = b.items[0];
      if (firstItem) {
        await db.itemState.upsert({
          where: { tokenHash_itemId: { tokenHash: t.tokenHash, itemId: firstItem.id } },
          create: { tokenHash: t.tokenHash, itemId: firstItem.id, saved: true, seenAt: new Date() },
          update: { saved: true },
        });
        console.log(`Saved ${firstItem.title} for ${t.deviceLabel} (${t.tokenHash.substring(0, 8)})`);
      }
    }
  }
  console.log("All books marked saved for active devices!");
}

saveAllBooks();
