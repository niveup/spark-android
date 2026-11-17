import "dotenv/config";
import { db } from "../src/lib/db";

const token = process.env.TELEGRAM_BOT_TOKEN;
const chatId = 5356318710;

const booksToUpload = [
  {
    title: "Science of Getting Rich",
    imageUrl: "https://covers.openlibrary.org/b/id/854989-L.jpg",
  },
  {
    title: "As a Man Thinketh",
    imageUrl: "https://covers.openlibrary.org/b/id/6268048-L.jpg",
  },
  {
    title: "The Autobiography of Benjamin Franklin",
    imageUrl: "https://covers.openlibrary.org/b/id/5647361-L.jpg",
  },
];

async function run() {
  if (!token) {
    throw new Error("TELEGRAM_BOT_TOKEN is not set in environment");
  }

  for (const item of booksToUpload) {
    console.log(`Sending photo to Telegram for: ${item.title}`);
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
      }
    } else {
      console.error("Failed to send photo to Telegram:", json);
    }
  }
}

run()
  .then(() => {
    console.log("All sample book covers sent to Telegram and saved in database!");
    process.exit(0);
  })
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
