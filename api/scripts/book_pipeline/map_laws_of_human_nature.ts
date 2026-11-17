import "dotenv/config";
import { db } from "../../src/lib/db";
import fs from "fs";
import path from "path";
import { execSync } from "child_process";

const CACHE_DIR = path.join(process.cwd(), "public", "audio");
const MANIFESTS_DIR = path.join(process.cwd(), "src", "lib", "audio_manifests");
const FFMPEG = "C:\\Users\\Lenovo\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-8.1.1-full_build\\bin\\ffmpeg.exe";

const BOOK_ID = "cmv_the_laws_of_human_nature";
const BOOK_TITLE = "The Laws of Human Nature";
const AUTHOR = "Robert Greene";
const PROGRESS_FILE = "D:\\books\\the-laws-of-human-nature\\slices_18mb\\slices_manifest_progress.json";
const COVER_FILE_ID_PATH = "D:\\books\\the-laws-of-human-nature\\cover_file_id.txt";
const TRANSCRIPT_FILE_ID_PATH = "D:\\books\\the-laws-of-human-nature\\transcript_file_id.txt";

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

async function main() {
  console.log("=== MAPPING THE LAWS OF HUMAN NATURE (19 CLEAN CHAPTERS) ===");

  const rawSlices: SliceItem[] = JSON.parse(fs.readFileSync(PROGRESS_FILE, "utf-8"));
  const coverFileId = fs.existsSync(COVER_FILE_ID_PATH) ? fs.readFileSync(COVER_FILE_ID_PATH, "utf-8").replace(/[^a-zA-Z0-9_\-]/g, "") : "";
  const transcriptFileId = fs.existsSync(TRANSCRIPT_FILE_ID_PATH) ? fs.readFileSync(TRANSCRIPT_FILE_ID_PATH, "utf-8").replace(/[^a-zA-Z0-9_\-]/g, "") : "";

  // 1. Group by chapter_num
  const byChapter = new Map<number, SliceItem[]>();
  for (const s of rawSlices) {
    if (!s.telegram_file_id) continue;
    const list = byChapter.get(s.chapter_num) || [];
    list.push(s);
    byChapter.set(s.chapter_num, list);
  }

  const sortedChapterNums = Array.from(byChapter.keys()).sort((a, b) => a - b);
  console.log(`Loaded ${sortedChapterNums.length} clean chapters.`);

  if (!fs.existsSync(CACHE_DIR)) fs.mkdirSync(CACHE_DIR, { recursive: true });
  if (!fs.existsSync(MANIFESTS_DIR)) fs.mkdirSync(MANIFESTS_DIR, { recursive: true });

  // 2. Pre-assemble multi-part chapters on the backend
  const chaptersData = [];
  const manifestItems = [];

  for (const cNum of sortedChapterNums) {
    const parts = byChapter.get(cNum)!;
    parts.sort((a, b) => a.part - b.part);

    const cleanTitle = parts[0].title.split(" (Part")[0].trim();
    const totalSec = parts.reduce((sum, p) => sum + p.duration_sec, 0);
    const audioKey = `human_nature_ch_${String(cNum).padStart(2, "0")}`;
    const outMp3 = path.join(CACHE_DIR, `${audioKey}.mp3`);

    if (parts.length === 1) {
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

  // Save manifest
  const manifestFile = path.join(MANIFESTS_DIR, "the-laws-of-human-nature.json");
  fs.writeFileSync(manifestFile, JSON.stringify(manifestItems, null, 2));

  // 3. Upsert Book
  const coverUrl = coverFileId ? `/api/mobile/v1/covers/${coverFileId}` : null;
  await db.book.upsert({
    where: { id: BOOK_ID },
    create: {
      id: BOOK_ID,
      title: BOOK_TITLE,
      author: AUTHOR,
      source: "manual",
      coverUrl,
      license: "Personal Study",
      isPublicDomain: false,
      audioUrl: chaptersData[0]?.audioUrl || null
    },
    update: {
      title: BOOK_TITLE,
      author: AUTHOR,
      coverUrl,
      audioUrl: chaptersData[0]?.audioUrl || null
    }
  });
  console.log(`[+] Upserted Book: [${BOOK_ID}] "${BOOK_TITLE}"`);

  // 4. Clean old/duplicate FeedItems for this book
  const existingItems = await db.feedItem.findMany({
    where: { bookId: BOOK_ID },
    select: { id: true }
  });
  const existingIds = existingItems.map((it) => it.id);
  if (existingIds.length > 0) {
    await db.itemState.deleteMany({ where: { itemId: { in: existingIds } } });
    await db.feedItem.deleteMany({ where: { bookId: BOOK_ID } });
    console.log(`[+] Cleaned ${existingIds.length} old/duplicate FeedItems.`);
  }

  // 5. Insert clean 1-N chapters into Supabase
  const baseTime = new Date("2026-11-16T10:00:00Z").getTime();

  for (let i = 0; i < chaptersData.length; i++) {
    const ch = chaptersData[i];
    const padNum = String(ch.chapterNum).padStart(2, "0");
    const id = `cmv_human_nature_ch_${padNum}`;
    const contentHash = `human-nature-ch-${padNum}`;
    const createdAt = new Date(baseTime + i * 1000);

    const mins = Math.floor(ch.durationSec / 60);
    const secs = ch.durationSec % 60;
    const bodyMd = `### ${ch.title}\n\nAudiobook chapter from *${BOOK_TITLE}* by ${AUTHOR}.\n\n- **Duration**: ${mins}m ${secs}s\n- **Focus**: Psychology, emotional intelligence, and human behavior.\n- **Audio**: High-fidelity streaming from Telegram cloud.`;

    const actionLine = ch.chapterNum === 0
      ? "Awaken your self-awareness and observe the hidden forces shaping your behavior."
      : `Apply the wisdom of ${ch.title} to master your emotions and relationships.`;

    await db.feedItem.create({
      data: {
        id,
        type: "audio",
        title: ch.title.length > 80 ? ch.title.slice(0, 77) + "..." : ch.title,
        bodyMd,
        actionLine,
        topicTags: ["psychology", "human-nature", "self-growth", "behavior"],
        language: "en",
        estSeconds: ch.durationSec,
        bookId: BOOK_ID,
        sourceType: "audiobook",
        sourceRef: `${BOOK_TITLE} by ${AUTHOR}`,
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

  console.log(`\n[+] SUCCESS: Mapped all ${chaptersData.length} clean chapters to SPARK!`);
}

main()
  .catch((err) => {
    console.error("[!] Error mapping to database:", err);
    process.exit(1);
  })
  .finally(() => db.$disconnect());
