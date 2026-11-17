/**
 * Step 5: Map Clean Chapters to SPARK Database
 * Pre-assembles multi-part audio on the backend and maps clean chapters into Supabase/Prisma.
 * Frontend displays ONLY single chapters (zero "Part 1/2" in UI).
 *
 * Usage:
 *   npx tsx 05_map_to_spark.ts --book-id "cmv_..." --title "..." --author "..." --progress-file "..."
 */

import "dotenv/config";
import { db } from "../../src/lib/db";
import fs from "fs";
import path from "path";
import { execSync } from "child_process";

const CACHE_DIR = path.join(process.cwd(), "public", "audio");
const FFMPEG = "C:\\Users\\Lenovo\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-8.1.1-full_build\\bin\\ffmpeg.exe";

interface SliceItem {
  id: string;
  chapter_num: number;
  part: number;
  title: string;
  start_sec: number;
  end_sec: number;
  duration_sec: number;
  file_path: string;
  size_mb: number;
  telegram_file_id?: string;
}

export async function mapBookToSpark(options: {
  bookId: string;
  bookTitle: string;
  author: string;
  progressFile: string;
  coverTelegramFileId?: string;
  transcriptTelegramFileId?: string;
}) {
  const { bookId, bookTitle, author, progressFile } = options;

  if (!fs.existsSync(progressFile)) {
    throw new Error(`Progress file not found: ${progressFile}`);
  }

  const rawSlices: SliceItem[] = JSON.parse(fs.readFileSync(progressFile, "utf-8"));
  const bookSlug = bookTitle.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");

  let effectiveBookId = bookId;

  // Auto-resolve or create book if 'auto'
  if (effectiveBookId === "auto" || !effectiveBookId) {
    let book = await db.book.findFirst({
      where: { title: { contains: bookTitle, mode: "insensitive" } }
    });
    if (!book) {
      const slug = bookTitle.toLowerCase().replace(/[^a-z0-9]+/g, "-");
      book = await db.book.create({
        data: {
          id: `cmv_${slug.slice(0, 10)}_${Date.now().toString(36)}`,
          title: bookTitle,
          author: author || "Unknown",
          source: "manual",
          license: "Personal Study",
          isPublicDomain: false,
        }
      });
      console.log(`[+] Created new Book entry: [${book.id}] "${bookTitle}"`);
    } else {
      console.log(`[+] Found existing Book entry: [${book.id}] "${book.title}"`);
    }
    effectiveBookId = book.id;
  }

  // 1. Group by chapter_num
  const byChapter = new Map<number, SliceItem[]>();
  for (const s of rawSlices) {
    if (!s.telegram_file_id) continue;
    const list = byChapter.get(s.chapter_num) || [];
    list.push(s);
    byChapter.set(s.chapter_num, list);
  }

  const sortedChapterNums = Array.from(byChapter.keys()).sort((a, b) => a - b);
  console.log(`[*] Mapping ${sortedChapterNums.length} clean chapters for "${bookTitle}"...`);

  // Ensure CACHE_DIR exists
  if (!fs.existsSync(CACHE_DIR)) {
    fs.mkdirSync(CACHE_DIR, { recursive: true });
  }

  // 2. Pre-assemble multi-part chapters on the backend
  const chaptersData = [];

  for (const cNum of sortedChapterNums) {
    const parts = byChapter.get(cNum)!;
    parts.sort((a, b) => a.part - b.part);

    const cleanTitle = parts[0].title.split(" (Part")[0].trim();
    const totalSec = parts.reduce((sum, p) => sum + p.duration_sec, 0);
    const audioKey = `${bookSlug}_ch_${String(cNum).padStart(2, "0")}`;
    const outMp3 = path.join(CACHE_DIR, `${audioKey}.mp3`);

    if (parts.length === 1) {
      // Single part
      const src = parts[0].file_path;
      if (fs.existsSync(src) && !fs.existsSync(outMp3)) {
        fs.copyFileSync(src, outMp3);
      }
    } else {
      // Multi-part: stitch with ffmpeg on backend
      const concatArg = "concat:" + parts.map((p) => p.file_path).join("|");
      try {
        execSync(`"${FFMPEG}" -y -i "${concatArg}" -c copy "${outMp3}"`, { stdio: "ignore" });
        console.log(`  [+] Stitched ${parts.length} parts -> ${audioKey}.mp3 (${totalSec}s)`);
      } catch (err) {
        console.error(`  [!] Concat error for Ch ${cNum}:`, err);
      }
    }

    chaptersData.push({
      chapterNum: cNum,
      title: cleanTitle,
      durationSec: totalSec,
      durationMs: totalSec * 1000,
      audioKey,
      audioUrl: `/api/mobile/v1/audio/${audioKey}`,
      telegramFileIds: parts.map((p) => p.telegram_file_id!),
      partsCount: parts.length
    });
  }

  // 3. Clean up old/duplicate FeedItems for this book
  const existingItems = await db.feedItem.findMany({
    where: { bookId: effectiveBookId },
    select: { id: true }
  });
  const existingIds = existingItems.map((it) => it.id);

  if (existingIds.length > 0) {
    await db.itemState.deleteMany({ where: { itemId: { in: existingIds } } });
    await db.feedItem.deleteMany({ where: { bookId: effectiveBookId } });
    console.log(`[+] Cleaned ${existingIds.length} old/duplicate items for book.`);
  }

  // 4. Insert clean 1-N chapters into Supabase
  const baseTime = new Date("2026-11-16T08:00:00Z").getTime();

  for (let i = 0; i < chaptersData.length; i++) {
    const ch = chaptersData[i];
    const padNum = String(ch.chapterNum).padStart(2, "0");
    const id = `cmv_${bookSlug.slice(0, 10)}_ch_${padNum}`;
    const contentHash = `${bookSlug}-ch-${padNum}`;
    const createdAt = new Date(baseTime + i * 1000);

    const mins = Math.floor(ch.durationSec / 60);
    const secs = ch.durationSec % 60;
    const bodyMd = `### ${ch.title}\n\nAudiobook chapter from *${bookTitle}* by ${author}.\n\n- **Duration**: ${mins}m ${secs}s\n- **Focus**: Key concepts and audio listening.\n- **Audio**: High-fidelity streaming from Telegram cloud.`;

    await db.feedItem.create({
      data: {
        id,
        type: "audio",
        title: ch.title.length > 80 ? ch.title.slice(0, 77) + "..." : ch.title,
        bodyMd,
        actionLine: `Apply insights from ${ch.title} in your daily thinking.`,
        topicTags: ["audio", "book", bookSlug],
        language: "en",
        estSeconds: ch.durationSec,
        bookId: effectiveBookId,
        sourceType: "audiobook",
        sourceRef: `${bookTitle} by ${author}`,
        license: "Personal Study",
        isAiGenerated: false,
        isGrounded: true,
        contentHash,
        audioUrl: ch.audioUrl,
        durationMs: ch.durationMs,
        status: "published",
        publishedAt: createdAt,
        createdAt
      }
    });

    console.log(`  [${i + 1}/${chaptersData.length}] Created: "${ch.title}" (${mins}m ${secs}s) -> ${ch.audioUrl}`);
  }

  // 5. Update Book record
  await db.book.update({
    where: { id: effectiveBookId },
    data: {
      audioUrl: chaptersData[0]?.audioUrl || null
    }
  });

  console.log(`\n[+] SUCCESS: Mapped all ${chaptersData.length} chapters cleanly!`);
}

// CLI entry
if (require.main === module) {
  const args = process.argv.slice(2);
  const getArg = (name: string) => {
    const idx = args.indexOf(name);
    return idx !== -1 ? args[idx + 1] : "";
  };

  const bookId = getArg("--book-id");
  const bookTitle = getArg("--title");
  const author = getArg("--author") || "Unknown";
  const progressFile = getArg("--progress-file");

  if (!bookId || !bookTitle || !progressFile) {
    console.error("Usage: npx tsx 05_map_to_spark.ts --book-id <ID> --title <TITLE> --progress-file <PATH> [--author <AUTHOR>]");
    process.exit(1);
  }

  mapBookToSpark({ bookId, bookTitle, author, progressFile })
    .catch((err) => {
      console.error("[!] Fatal mapping error:", err);
      process.exit(1);
    })
    .finally(() => db.$disconnect());
}
