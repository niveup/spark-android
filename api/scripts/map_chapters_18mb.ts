import "dotenv/config";
import { db } from "../src/lib/db";
import fs from "fs";
import path from "path";

const BOOK_ID = "cmv_69aa5debeffde2a100b0";

interface Chapter18Mb {
  id: string;
  chapter_num: number;
  part: number;
  title: string;
  duration_sec: number;
  telegram_file_id?: string;
  size_mb: number;
}

export async function map18MbChapters(items: Chapter18Mb[]) {
  const baseTime = new Date("2026-11-16T08:00:00Z").getTime();

  for (let i = 0; i < items.length; i++) {
    const ch = items[i];
    if (!ch.telegram_file_id) continue;

    const contentHash = `48-laws-of-power-part-${ch.id}`;
    const createdAt = new Date(baseTime + i * 1000);
    const audioUrl = `/api/mobile/v1/audio/${ch.telegram_file_id}`;
    const durationMs = ch.duration_sec * 1000;

    const actionLine = `Understand and apply ${ch.title.replace(/\s*\(Part\s*\d+\)/i, "")} in strategic interactions.`;
    const bodyMd = `### ${ch.title}\n\nAudiobook chapter from *The 48 Laws of Power* by Robert Greene.\n\n- **Duration**: ${Math.floor(ch.duration_sec / 60)}m ${ch.duration_sec % 60}s\n- **File size**: ${ch.size_mb} MB (strictly <= 18 MB)\n- **Focus**: Strategic rules and real-world historical dynamics.`;

    await db.feedItem.upsert({
      where: { contentHash },
      create: {
        id: `cmv_48laws_${String(i).padStart(2, "0")}`,
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
        bodyMd,
        audioUrl,
        durationMs,
        estSeconds: ch.duration_sec,
        status: "published",
      },
    });

    console.log(`Mapped [${i+1}/${items.length}] ${ch.title} (${ch.size_mb} MB) -> ${audioUrl}`);
  }

  // Update book audioUrl to first available chapter
  const firstWithAudio = items.find((c) => !!c.telegram_file_id);
  if (firstWithAudio) {
    await db.book.update({
      where: { id: BOOK_ID },
      data: {
        audioUrl: `/api/mobile/v1/audio/${firstWithAudio.telegram_file_id}`,
      },
    });
  }
}

async function main() {
  const jsonPath = path.join("D:", "books", "48-laws-of-power", "chapters_18mb_progress.json");
  if (!fs.existsSync(jsonPath)) {
    console.log("No progress file yet:", jsonPath);
    return;
  }
  const raw = JSON.parse(fs.readFileSync(jsonPath, "utf-8"));
  await map18MbChapters(raw);
}

if (process.argv[1]?.includes("map_chapters_18mb.ts")) {
  main()
    .catch(console.error)
    .finally(() => process.exit(0));
}
