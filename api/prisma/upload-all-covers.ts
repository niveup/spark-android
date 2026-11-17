import "dotenv/config";
import { db } from "../src/lib/db";

const token = process.env.TELEGRAM_BOT_TOKEN;
const chatId = 5356318710;

const booksToUpload = [
  {
    title: "Meditations",
    imageUrl: "https://covers.openlibrary.org/b/id/211529-L.jpg",
  },
  {
    title: "How to Live on Twenty-Four Hours a Day",
    imageUrl: "https://covers.openlibrary.org/b/id/1760143-L.jpg",
  },
  {
    title: "Acres of Diamonds",
    imageUrl: "https://covers.openlibrary.org/b/id/5833160-L.jpg",
  },
  {
    title: "Self Help; with Illustrations of Conduct and Perseverance",
    imageUrl: "https://covers.openlibrary.org/b/id/5587999-L.jpg",
  },
  {
    title: "Enchiridion of Epictetus",
    imageUrl: "https://covers.openlibrary.org/b/id/12622080-L.jpg",
  },
  {
    title: "Pride and Prejudice",
    imageUrl: "https://covers.openlibrary.org/b/id/14348537-L.jpg",
  },
  {
    title: "Frankenstein",
    imageUrl: "https://covers.openlibrary.org/b/id/12356249-L.jpg",
  },
];

async function run() {
  if (!token) {
    throw new Error("TELEGRAM_BOT_TOKEN is not set in environment");
  }

  for (const item of booksToUpload) {
    console.log(`Sending photo to Telegram for: ${item.title}`);
    try {
      const res = await fetch(`https://api.telegram.org/bot${token}/sendPhoto`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          chat_id: chatId,
          photo: item.imageUrl,
          caption: `📖 ${item.title}`,
        }),
      });

      const json = (await res.json()) as {
        ok: boolean;
        result?: { photo?: { file_id: string }[] };
        description?: string;
      };

      if (json.ok && json.result?.photo) {
        const highestRes = json.result.photo[json.result.photo.length - 1];
        const fileId = highestRes.file_id;
        console.log(`Uploaded to Telegram! file_id: ${fileId}`);

        const book = await db.book.findFirst({
          where: { title: { contains: item.title, mode: "insensitive" } },
        });

        if (book) {
          await db.book.update({
            where: { id: book.id },
            data: { coverUrl: `/api/mobile/v1/covers/${fileId}` },
          });
          console.log(`✅ Linked cover to database: ${book.title}`);
        } else {
          console.log(`⚠️ Book not found in database: ${item.title}`);
        }
      } else {
        console.error(`❌ Telegram sendPhoto failed for ${item.title}:`, json.description);
      }
    } catch (e: any) {
      console.error(`Error uploading ${item.title}:`, e.message);
    }

    // Small delay between Telegram sends to prevent rate limit
    await new Promise((r) => setTimeout(r, 1000));
  }

  console.log("\n🎉 ALL BOOK COVERS UPLOADED TO TELEGRAM AND LINKED!");
}

run();
