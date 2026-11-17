import "dotenv/config";
import { db } from "../src/lib/db";

async function main() {
  const fileId = "AgACAgUAAxkDAAIBRWr6wZ6SDv7F4R_BQgUkl_9-6iCAAAKtFWsbNXbZVwTyUbhtGG9BAQADAgADeAADPQQ";
  const coverUrl = `/api/mobile/v1/covers/${fileId}`;

  const updated = await db.book.update({
    where: { id: "cmv_the_laws_of_human_nature" },
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
