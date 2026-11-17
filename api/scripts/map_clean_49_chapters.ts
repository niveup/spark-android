import "dotenv/config";
import { db } from "../src/lib/db";
import fs from "fs";
import path from "path";

const BOOK_ID = "cmv_69aa5debeffde2a100b0";
const MANIFEST_PATH = path.join(process.cwd(), "src", "lib", "chapters_48laws_manifest.json");

interface ChapterManifest {
  chapter_num: number;
  title: string;
  duration_sec: number;
  duration_ms: number;
  audio_key: string;
  audio_url: string;
  telegram_file_ids: string[];
  parts_count: number;
  sizes_mb: number[];
}

async function main() {
  console.log("=== CLEANING & MAPPING 48 LAWS OF POWER (49 CLEAN CHAPTERS) ===");

  if (!fs.existsSync(MANIFEST_PATH)) {
    throw new Error(`Manifest not found at ${MANIFEST_PATH}`);
  }

  const manifest: ChapterManifest[] = JSON.parse(
    fs.readFileSync(MANIFEST_PATH, "utf-8")
  );

  console.log(`Loaded ${manifest.length} clean chapters from manifest.`);

  // 1. Find all old FeedItems for this book
  const existingItems = await db.feedItem.findMany({
    where: { bookId: BOOK_ID },
    select: { id: true },
  });

  const existingIds = existingItems.map((it) => it.id);
  console.log(`Found ${existingIds.length} existing FeedItems for this book to clean.`);

  // Clean up any item states referencing old items
  if (existingIds.length > 0) {
    const deletedStates = await db.itemState.deleteMany({
      where: { itemId: { in: existingIds } },
    });
    console.log(`Deleted ${deletedStates.count} orphaned item states.`);

    const deletedItems = await db.feedItem.deleteMany({
      where: { bookId: BOOK_ID },
    });
    console.log(`Deleted ${deletedItems.count} old FeedItems.`);
  }

  // 2. Insert exactly 49 clean, pristine chapter items
  const baseTime = new Date("2026-11-16T08:00:00Z").getTime();

  for (let i = 0; i < manifest.length; i++) {
    const ch = manifest[i];
    const num = ch.chapter_num;
    const padNum = String(num).padStart(2, "0");
    const id = `cmv_48laws_ch_${padNum}`;
    const contentHash = `48-laws-of-power-clean-ch-${padNum}`;
    const createdAt = new Date(baseTime + i * 1000); // exactly 1 second apart for stable order

    const mins = Math.floor(ch.duration_sec / 60);
    const secs = ch.duration_sec % 60;
    const bodyMd = `### ${ch.title}\n\nAudiobook chapter from *The 48 Laws of Power* by Robert Greene.\n\n- **Duration**: ${mins}m ${secs}s\n- **Focus**: Strategic rules and real-world historical dynamics.\n- **Audio**: High-fidelity streaming directly from Telegram cloud storage.`;

    const actionLine = num === 0
      ? "Master the foundational philosophy of power and strategic awareness."
      : `Apply the wisdom of Law ${num} carefully in your daily decisions and interactions.`;

    await db.feedItem.create({
      data: {
        id,
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
        audioUrl: ch.audio_url,
        durationMs: ch.duration_ms,
        status: "published",
        publishedAt: createdAt,
        createdAt,
      },
    });

    console.log(
      `[${i + 1}/${manifest.length}] Created Ch ${padNum}: "${ch.title}" (${mins}m ${secs}s) -> ${ch.audio_url}`
    );
  }

  // 3. Update Book audioUrl to Preface
  await db.book.update({
    where: { id: BOOK_ID },
    data: {
      audioUrl: manifest[0].audio_url,
    },
  });

  console.log(`\nSUCCESS: Mapped all ${manifest.length} chapters cleanly! Zero duplicates! Zero Part 1/2 in frontend!`);
}

main()
  .catch((e) => {
    console.error("FATAL ERROR:", e);
    process.exit(1);
  })
  .finally(() => db.$disconnect());
