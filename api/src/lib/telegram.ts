import { db } from "./db";

interface TelegramPhotoSize {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  file_size?: number;
}

interface TelegramMessage {
  message_id: number;
  from?: { id: number; first_name?: string; username?: string };
  chat: { id: number; type: string };
  date: number;
  photo?: TelegramPhotoSize[];
  caption?: string;
  text?: string;
}

interface TelegramUpdate {
  update_id: number;
  message?: TelegramMessage;
  channel_post?: TelegramMessage;
}

export async function sendTelegramMessage(chatId: number, text: string) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) return;

  await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      chat_id: chatId,
      text,
      parse_mode: "Markdown",
    }),
  }).catch((err) => console.error("Telegram send error:", err));
}

export async function processTelegramMessage(msg: TelegramMessage): Promise<string | null> {
  const chatId = msg.chat.id;

  if (msg.text?.startsWith("/start") || msg.text?.startsWith("/help")) {
    const allBooks = await db.book.findMany({ select: { title: true, author: true } });
    const bookList = allBooks.map((b) => `• *${b.title}* (${b.author || "Unknown"})`).join("\n");
    const reply =
      `👋 *Welcome to SPARK Book Covers!*\n\n` +
      `Send me any book cover photo with the *book title* in the caption, and I will instantly link it to your app!\n\n` +
      `📚 *Current Books in your App:*\n${bookList}`;
    await sendTelegramMessage(chatId, reply);
    return "Handled /start";
  }

  if (!msg.photo || msg.photo.length === 0) {
    if (msg.text) {
      await sendTelegramMessage(
        chatId,
        `📷 Please send a *photo* of the book cover with the book title in the caption!`
      );
    }
    return "No photo in message";
  }

  const caption = (msg.caption || "").trim();
  if (!caption) {
    await sendTelegramMessage(
      chatId,
      `⚠️ You sent a photo without a caption!\n\nPlease send the photo again and type the *book title* in the caption.`
    );
    return "Missing caption";
  }

  // Get highest resolution photo (last in array)
  const highestRes = msg.photo[msg.photo.length - 1];
  const fileId = highestRes.file_id;

  // Find matching book
  const allBooks = await db.book.findMany();
  const search = caption.toLowerCase();

  const matched =
    allBooks.find((b) => b.title.toLowerCase() === search) ||
    allBooks.find((b) => b.title.toLowerCase().includes(search) || search.includes(b.title.toLowerCase()));

  if (!matched) {
    const titles = allBooks.map((b) => `• ${b.title}`).join("\n");
    await sendTelegramMessage(
      chatId,
      `❌ Could not find a book matching: *${caption}*\n\nAvailable books:\n${titles}`
    );
    return `No book matching: ${caption}`;
  }

  // Update book coverUrl in Postgres
  // Stored as relative path /api/mobile/v1/covers/<fileId> so it works seamlessly on any domain
  const coverPath = `/api/mobile/v1/covers/${fileId}`;
  await db.book.update({
    where: { id: matched.id },
    data: { coverUrl: coverPath },
  });

  await sendTelegramMessage(
    chatId,
    `🎉 *Cover Updated!*\n\n📖 *${matched.title}*\n👤 ${matched.author || "Unknown"}\n\nThe new cover is now live and will appear in your SPARK app!`
  );

  return `Updated ${matched.title} with fileId ${fileId}`;
}

export async function syncTelegramUpdates(): Promise<{ processed: number; logs: string[] }> {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) return { processed: 0, logs: ["TELEGRAM_BOT_TOKEN not set"] };

  const res = await fetch(`https://api.telegram.org/bot${token}/getUpdates`);
  const data = (await res.json()) as { ok: boolean; result?: TelegramUpdate[] };

  if (!data.ok || !data.result || data.result.length === 0) {
    return { processed: 0, logs: ["No pending updates"] };
  }

  const logs: string[] = [];
  let maxUpdateId = 0;

  for (const update of data.result) {
    if (update.update_id > maxUpdateId) maxUpdateId = update.update_id;
    const msg = update.message || update.channel_post;
    if (msg) {
      const log = await processTelegramMessage(msg);
      if (log) logs.push(log);
    }
  }

  // Acknowledge updates up to maxUpdateId
  if (maxUpdateId > 0) {
    await fetch(`https://api.telegram.org/bot${token}/getUpdates?offset=${maxUpdateId + 1}`);
  }

  return { processed: data.result.length, logs };
}
