// One-off: spot-check that published audiobook stream URLs are still live.
// Read-only; HTTP HEAD against Archive.org/LibriVox CDN links stored on cards.
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

const rows = await db.feedItem.findMany({
  where: { status: "published", type: "audio", audioUrl: { not: null } },
  select: { title: true, audioUrl: true },
  orderBy: { id: "asc" },
});

let ok = 0;
let bad = 0;
for (const r of rows) {
  try {
    const res = await fetch(r.audioUrl!, { method: "HEAD" });
    if (res.ok) {
      ok++;
    } else {
      bad++;
      console.log(`HTTP ${res.status} — ${r.title}`);
    }
  } catch (e) {
    bad++;
    console.log(`FETCH ERROR — ${r.title}: ${(e as Error).message}`);
  }
  await new Promise((s) => setTimeout(s, 300));
}
console.log(`Stream URL spot-check: ${ok}/${rows.length} live, ${bad} dead`);
await db.$disconnect();
