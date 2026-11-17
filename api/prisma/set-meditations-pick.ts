import "dotenv/config";
import { db } from "../src/lib/db";

async function makePick() {
  const tokens = await db.deviceToken.findMany({ where: { deviceLabel: { contains: "RMX" } } });
  const item = await db.feedItem.findFirst({
    where: {
      type: "book_idea",
      book: { title: { contains: "Meditations" } },
    },
  });

  if (!item) {
    console.log("Item not found");
    return;
  }

  console.log(`Setting Today's Pick to: ${item.title}`);
  for (const t of tokens) {
    await db.itemState.upsert({
      where: { tokenHash_itemId: { tokenHash: t.tokenHash, itemId: item.id } },
      create: { tokenHash: t.tokenHash, itemId: item.id, srsDueAt: new Date(Date.now() - 120000), saved: true },
      update: { srsDueAt: new Date(Date.now() - 120000), saved: true },
    });
  }
  console.log("Done!");
}

makePick();
