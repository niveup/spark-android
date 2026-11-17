import { syncTelegramUpdates } from "../src/lib/telegram";

async function main() {
  console.log("Polling Telegram for new book covers...");
  const res = await syncTelegramUpdates();
  console.log(`Processed ${res.processed} update(s):`, res.logs);
}

main().catch((err) => {
  console.error("Telegram sync error:", err);
  process.exit(1);
});
