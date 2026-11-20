import "dotenv/config";
import { db } from "../src/lib/db";

async function main() {
  const fileId = "AgACAgUAAxkDAAIBcGr_owl3BGuh7DLdEJ0gToqDsfZDAAJ1Emsb9u4BVCdmIwnFN5whAQADAgADeQADPQQ";
  const coverUrl = `/api/mobile/v1/covers/${fileId}`;

  const updated = await db.book.update({
    where: { id: "cmv_the_art_of_war" },
    data: { coverUrl }
  });

  console.log(`[+] Successfully updated coverUrl for ${updated.title}:`);
  console.log(`    ${updated.coverUrl}`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => db.$disconnect());
