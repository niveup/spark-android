import "dotenv/config";
import { db } from "../src/lib/db";

const token = process.env.TELEGRAM_BOT_TOKEN;
const chatId = 5356318710;

const hdCovers = [
  {
    title: "Meditations",
    imageUrl: "https://standardebooks.org/ebooks/marcus-aurelius/meditations/george-long/downloads/cover.jpg",
  },
  {
    title: "The Autobiography of Benjamin Franklin",
    imageUrl: "https://standardebooks.org/ebooks/benjamin-franklin/the-autobiography-of-benjamin-franklin/downloads/cover.jpg",
  },
  {
    title: "Enchiridion of Epictetus",
    imageUrl: "https://standardebooks.org/ebooks/epictetus/the-enchiridion/elizabeth-carter/downloads/cover.jpg",
  },
  {
    title: "Frankenstein",
    imageUrl: "https://standardebooks.org/ebooks/mary-shelley/frankenstein/downloads/cover.jpg",
  },
  {
    title: "Pride and Prejudice",
    imageUrl: "https://standardebooks.org/ebooks/jane-austen/pride-and-prejudice/downloads/cover.jpg",
  },
  {
    title: "How to Live on Twenty-Four Hours a Day",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/7/7f/How_to_Live_on_24_Hours_a_Day.png",
  },
];

async function run() {
  if (!token) throw new Error("TELEGRAM_BOT_TOKEN missing");

  for (const item of hdCovers) {
    console.log(`Uploading Ultra-HD cover to Telegram for: ${item.title}`);
    try {
      const res = await fetch(`https://api.telegram.org/bot${token}/sendPhoto`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          chat_id: chatId,
          photo: item.imageUrl,
          caption: `✨ HD Cover: ${item.title}`,
        }),
      });

      const json = (await res.json()) as any;
      if (json.ok && json.result?.photo) {
        const highestRes = json.result.photo[json.result.photo.length - 1];
        const fileId = highestRes.file_id;
        console.log(`✅ Uploaded to Telegram (${highestRes.width}x${highestRes.height})! file_id: ${fileId}`);

        const book = await db.book.findFirst({
          where: { title: { contains: item.title, mode: "insensitive" } },
        });

        if (book) {
          await db.book.update({
            where: { id: book.id },
            data: { coverUrl: `/api/mobile/v1/covers/${fileId}` },
          });
          console.log(`Updated DB coverUrl for: ${book.title}`);
        }
      } else {
        console.error(`Telegram sendPhoto error for ${item.title}:`, json.description);
      }
    } catch (e: any) {
      console.error(`Failed ${item.title}:`, e.message);
    }
    await new Promise((r) => setTimeout(r, 1200));
  }

  console.log("\n🚀 All Ultra-HD covers uploaded to Telegram and updated in database!");
}

run();
