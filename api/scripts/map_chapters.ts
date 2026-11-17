import "dotenv/config";
import { db } from "../src/lib/db";
import fs from "fs";
import path from "path";

const BOOK_ID = "cmv_69aa5debeffde2a100b0";

interface ChapterData {
  chapter_num: number;
  title: string;
  duration_sec: number;
  telegram_file_id: string;
  action_line?: string;
  body_md?: string;
}

export async function mapChaptersToDb(chapters: ChapterData[]) {
  const baseTime = new Date("2026-11-16T08:00:00Z").getTime();

  for (const ch of chapters) {
    if (!ch.telegram_file_id) continue;

    const contentHash = `48-laws-of-power-chapter-${ch.chapter_num}`;
    const createdAt = new Date(baseTime + ch.chapter_num * 1000);
    const audioUrl = `/api/mobile/v1/audio/${ch.telegram_file_id}`;
    const durationMs = ch.duration_sec * 1000;

    const actionLine = ch.action_line || "Understand this law and observe its dynamics in daily interactions.";
    const bodyMd = ch.body_md || `### ${ch.title}\n\nFull chapter audiobook recording from Robert Greene's classic work *The 48 Laws of Power*.`;

    await db.feedItem.upsert({
      where: { contentHash },
      create: {
        id: `cmv_48laws_ch_${String(ch.chapter_num).padStart(2, "0")}`,
        type: "audio",
        title: ch.title.length > 80 ? ch.title.slice(0, 77) + "..." : ch.title,
        bodyMd,
        actionLine,
        topicTags: ["power", "strategy", "psychology", "48-laws"],
        language: "en",
        estSeconds: ch.duration_sec,
        bookId: BOOK_ID,
        sourceType: "audiobook",
        sourceRef: "The 48 Laws of Power by Robert Greene",
        license: "Personal Study",
        isAiGenerated: false,
        isGrounded: true,
        contentHash,
        audioUrl,
        durationMs,
        status: "published",
        publishedAt: createdAt,
        createdAt,
      },
      update: {
        title: ch.title.length > 80 ? ch.title.slice(0, 77) + "..." : ch.title,
        audioUrl,
        durationMs,
        estSeconds: ch.duration_sec,
        status: "published",
      },
    });

    console.log(`Mapped chapter ${ch.chapter_num}: ${ch.title} -> ${audioUrl}`);
  }

  // Update book audioUrl to chapter 1 (or 0)
  const firstWithAudio = chapters.find((c) => !!c.telegram_file_id);
  if (firstWithAudio) {
    await db.book.update({
      where: { id: BOOK_ID },
      data: {
        audioUrl: `/api/mobile/v1/audio/${firstWithAudio.telegram_file_id}`,
      },
    });
    console.log(`Updated Book.audioUrl to: /api/mobile/v1/audio/${firstWithAudio.telegram_file_id}`);
  }
}

async function main() {
  const jsonPath = path.join("D:", "books", "48-laws-of-power", "chapters_progress.json");
  if (!fs.existsSync(jsonPath)) {
    console.log("No progress file yet:", jsonPath);
    return;
  }
  const raw = JSON.parse(fs.readFileSync(jsonPath, "utf-8"));
  await mapChaptersToDb(raw);
}

if (process.argv[1]?.includes("map_chapters.ts")) {
  main()
    .catch(console.error)
    .finally(() => process.exit(0));
}
