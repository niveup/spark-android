import { syncTelegramUpdates } from "../src/lib/telegram";

async function loop() {
  console.log("Telegram cover sync daemon active. Listening for photos...");
  while (true) {
    try {
      const res = await syncTelegramUpdates();
      if (res.processed > 0) {
        console.log(`[Telegram] Processed ${res.processed} update(s):`, res.logs);
      }
    } catch (err) {
      console.error("[Telegram] Error syncing:", err);
    }
    // Poll every 3 seconds
    await new Promise((resolve) => setTimeout(resolve, 3000));
  }
}

loop();
