/**
 * Step 5: Map Clean Chapters to SPARK Database
 * Pre-assembles multi-part audio on the backend and maps clean chapters into Supabase/Prisma.
 * Frontend displays ONLY single chapters (zero "Part 1/2" in UI).
 * Writes dynamic multi-part fallback manifests to src/lib/audio_manifests/<slug>.json
 *
 * Usage:
 *   npx tsx scripts/book_pipeline/05_map_to_spark.ts --title "..." --author "..." --progress-file "..." [--book-id "cmv_..."]
 */

import "dotenv/config";
import { db } from "../../src/lib/db";
import fs from "fs";
import path from "path";
import { execSync } from "child_process";

const CACHE_DIR = path.join(process.cwd(), "public", "audio");
const MANIFESTS_DIR = path.join(process.cwd(), "src", "lib", "audio_manifests");
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

function cleanString(str: string): string {
  return str.replace(/[^a-zA-Z0-9_\-]/g, "");
}

export async function mapBookToSpark(options: {
  bookTitle: string;
  author: string;
  progressFile: string;
  bookId?: string;
  coverTelegramFileId?: string;
  transcriptTelegramFileId?: string;
  isEditorsChoice?: boolean;
  category?: string;
}) {
  const { bookTitle, author, progressFile } = options;

  if (!fs.existsSync(progressFile)) {
    throw new Error(`Progress file not found: ${progressFile}`);
  }

  const workDir = path.dirname(path.resolve(progressFile));
  const coverPath = path.join(workDir, "cover_file_id.txt");
  const transcriptPath = path.join(workDir, "transcript_file_id.txt");

  const coverFileId = options.coverTelegramFileId || (fs.existsSync(coverPath) ? cleanString(fs.readFileSync(coverPath, "utf-8")) : "");
  const transcriptFileId = options.transcriptTelegramFileId || (fs.existsSync(transcriptPath) ? cleanString(fs.readFileSync(transcriptPath, "utf-8")) : "");

  const rawSlices: SliceItem[] = JSON.parse(fs.readFileSync(progressFile, "utf-8"));
  const bookSlug = bookTitle.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");

  let effectiveBookId = (options.bookId && options.bookId !== "auto") ? options.bookId : `cmv_${bookSlug.replace(/-/g, "_")}`;

  // 1. Group slices by chapter_num
  const byChapter = new Map<number, SliceItem[]>();
  for (const s of rawSlices) {
    if (!s.telegram_file_id) continue;
    const list = byChapter.get(s.chapter_num) || [];
    list.push(s);
    byChapter.set(s.chapter_num, list);
  }

  const sortedChapterNums = Array.from(byChapter.keys()).sort((a, b) => a - b);
  console.log(`[*] Mapping ${sortedChapterNums.length} clean chapters for "${bookTitle}" [${effectiveBookId}]...`);

  if (!fs.existsSync(CACHE_DIR)) fs.mkdirSync(CACHE_DIR, { recursive: true });
  if (!fs.existsSync(MANIFESTS_DIR)) fs.mkdirSync(MANIFESTS_DIR, { recursive: true });

  // 2. Pre-assemble multi-part chapters on the backend
  const chaptersData = [];
  const manifestItems = [];

  for (const cNum of sortedChapterNums) {
    const parts = byChapter.get(cNum)!;
    parts.sort((a, b) => a.part - b.part);

    const cleanTitle = parts[0].title.split(/\s*\(Part\s*\d+\)/i)[0].trim();
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
        const ffmpegCmd = fs.existsSync(FFMPEG) ? `"${FFMPEG}"` : "ffmpeg";
        execSync(`${ffmpegCmd} -y -i "${concatArg}" -c copy "${outMp3}"`, { stdio: "ignore" });
        console.log(`  [+] Stitched ${parts.length} backend parts -> ${audioKey}.mp3 (${totalSec}s)`);
      } catch (err) {
        console.error(`  [!] Concat error for Ch ${cNum}:`, err);
      }
    }

    const item = {
      chapterNum: cNum,
      title: cleanTitle,
      durationSec: totalSec,
      durationMs: totalSec * 1000,
      audioKey,
      audioUrl: `/api/mobile/v1/audio/${audioKey}`,
      telegramFileIds: parts.map((p) => p.telegram_file_id!),
      partsCount: parts.length
    };
    chaptersData.push(item);
    manifestItems.push({
      audio_key: audioKey,
      telegram_file_ids: item.telegramFileIds
    });
  }

  // Save dynamic multi-part fallback manifest
  const manifestFile = path.join(MANIFESTS_DIR, `${bookSlug}.json`);
  fs.writeFileSync(manifestFile, JSON.stringify(manifestItems, null, 2), "utf-8");
  console.log(`[+] Dynamic multi-part manifest saved: ${manifestFile}`);

  // 3. Upsert Book
  const coverUrl = coverFileId ? `/api/mobile/v1/covers/${coverFileId}` : null;
  await db.book.upsert({
    where: { id: effectiveBookId },
    create: {
      id: effectiveBookId,
      title: bookTitle,
      author: author || "Unknown",
      source: "manual",
      coverUrl,
      license: "Personal Study",
      isPublicDomain: false,
      isEditorsChoice: options.isEditorsChoice ?? false,
      category: options.category ?? null,
      audioUrl: chaptersData[0]?.audioUrl || null
    },
    update: {
      title: bookTitle,
      author: author || "Unknown",
      coverUrl,
      ...(options.isEditorsChoice !== undefined ? { isEditorsChoice: options.isEditorsChoice } : {}),
      ...(options.category ? { category: options.category } : {}),
      audioUrl: chaptersData[0]?.audioUrl || null
    }
  });
  console.log(`[+] Upserted Book: [${effectiveBookId}] "${bookTitle}"`);

  // 4. Clean up old/duplicate FeedItems for this book
  const existingItems = await db.feedItem.findMany({
    where: { bookId: effectiveBookId },
    select: { id: true }
  });
  const existingIds = existingItems.map((it) => it.id);

  if (existingIds.length > 0) {
    await db.itemState.deleteMany({ where: { itemId: { in: existingIds } } });
    await db.feedItem.deleteMany({ where: { bookId: effectiveBookId } });
    console.log(`[+] Cleaned ${existingIds.length} old/duplicate FeedItems.`);
  }

  // 5. Insert clean 1-N chapters into Supabase
  const baseTime = new Date("2026-11-16T10:00:00Z").getTime();

  for (let i = 0; i < chaptersData.length; i++) {
    const ch = chaptersData[i];
    const padNum = String(ch.chapterNum).padStart(2, "0");
    const id = `cmv_${bookSlug.replace(/-/g, "_")}_ch_${padNum}`;
    const contentHash = `${bookSlug}-ch-${padNum}`;
    const createdAt = new Date(baseTime + i * 1000);

    const mins = Math.floor(ch.durationSec / 60);
    const secs = ch.durationSec % 60;
    const bodyMd = `### ${ch.title}\n\nAudiobook chapter from *${bookTitle}* by ${author}.\n\n- **Duration**: ${mins}m ${secs}s\n- **Focus**: Psychology, growth, and self-mastery.\n- **Audio**: High-fidelity streaming from Telegram cloud.`;
    const actionLine = `Reflect on and apply insights from ${ch.title}.`;

    await db.feedItem.create({
      data: {
        id,
        type: "audio",
        title: ch.title.length > 80 ? ch.title.slice(0, 77) + "..." : ch.title,
        bodyMd,
        actionLine,
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

  console.log(`\n[+] SUCCESS: All ${chaptersData.length} clean chapters mapped to SPARK!`);
}

// CLI entry
const args = process.argv.slice(2);
if (args.length > 0) {
  const getArg = (name: string) => {
    const idx = args.indexOf(name);
    return idx !== -1 ? args[idx + 1] : "";
  };

  const bookTitle = getArg("--title");
  const author = getArg("--author") || "Unknown";
  const progressFile = getArg("--progress-file");
  const bookId = getArg("--book-id");
  const isEditorsChoice = args.includes("--editors-choice") || getArg("--editors-choice") === "true";
  const category = getArg("--category") || undefined;

  if (!bookTitle || !progressFile) {
    console.error("Usage: npx tsx scripts/book_pipeline/05_map_to_spark.ts --title <TITLE> --progress-file <PATH> [--author <AUTHOR>] [--book-id <ID>] [--editors-choice] [--category <CATEGORY>]");
    process.exit(1);
  }

  mapBookToSpark({ bookTitle, author, progressFile, bookId, isEditorsChoice, category })
    .catch((err) => {
      console.error("[!] Fatal mapping error:", err);
      process.exit(1);
    })
    .finally(() => db.$disconnect());
}
