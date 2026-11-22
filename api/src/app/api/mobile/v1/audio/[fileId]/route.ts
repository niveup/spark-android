import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import os from "os";
import { Readable } from "stream";

// Up to 5 minutes execution on Vercel Fluid Compute
export const maxDuration = 300;

const AUDIO_TMP_DIR = path.join(os.tmpdir(), "spark_audio");
const LOCAL_AUDIO_DIR = path.join(process.cwd(), "public", "audio");
const MANIFESTS_DIR = path.join(process.cwd(), "src", "lib", "audio_manifests");

interface TelegramFileInfo {
  filePath: string;
  fileSize: number;
}

const fileInfoCache = new Map<string, TelegramFileInfo>();
const manifestCache = new Map<string, string[]>();
let manifestsLoaded = false;

function loadManifests() {
  if (manifestsLoaded) return;
  if (fs.existsSync(MANIFESTS_DIR)) {
    try {
      const files = fs.readdirSync(MANIFESTS_DIR);
      for (const f of files) {
        if (f.endsWith(".json")) {
          try {
            const list = JSON.parse(fs.readFileSync(path.join(MANIFESTS_DIR, f), "utf-8"));
            if (Array.isArray(list)) {
              for (const item of list) {
                const key = item.audio_key || item.audioKey;
                const ids = item.telegram_file_ids || item.telegramFileIds;
                if (key && Array.isArray(ids) && ids.length > 0) {
                  manifestCache.set(key, ids);
                }
              }
            }
          } catch {}
        }
      }
    } catch {}
  }
  manifestsLoaded = true;
}

function findManifestEntry(fileId: string): string[] | null {
  loadManifests();
  return manifestCache.get(fileId) || null;
}

async function getTelegramFileInfo(fileId: string, token: string): Promise<TelegramFileInfo> {
  const cached = fileInfoCache.get(fileId);
  if (cached) return cached;

  const fileRes = await fetch(
    `https://api.telegram.org/bot${token}/getFile?file_id=${encodeURIComponent(fileId)}`
  );
  const fileJson = (await fileRes.json()) as {
    ok: boolean;
    result?: { file_path?: string; file_size?: number };
  };

  if (!fileJson.ok || !fileJson.result?.file_path) {
    throw new Error(`File not found on Telegram for ID: ${fileId}`);
  }

  const info: TelegramFileInfo = {
    filePath: fileJson.result.file_path,
    fileSize: fileJson.result.file_size || 0,
  };
  fileInfoCache.set(fileId, info);
  return info;
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ fileId: string }> }
) {
  const { fileId } = await params;
  if (!fileId) {
    return new NextResponse("Missing file ID", { status: 400 });
  }

  // 1. Resolve cached file from local disk or serverless writable /tmp if exists
  let cachedFilePath: string | null = null;
  const localCandidate = path.join(LOCAL_AUDIO_DIR, `${fileId}.mp3`);
  const tmpCandidate = path.join(AUDIO_TMP_DIR, `${fileId}.mp3`);

  if (fs.existsSync(localCandidate)) {
    cachedFilePath = localCandidate;
  } else if (fs.existsSync(tmpCandidate)) {
    cachedFilePath = tmpCandidate;
  }

  if (cachedFilePath) {
    try {
      const stat = fs.statSync(cachedFilePath);
      const fileSize = stat.size;
      const range = req.headers.get("range");

      if (range) {
        const parts = range.replace(/bytes=/, "").split("-");
        const start = parseInt(parts[0], 10);
        const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;

        if (start >= fileSize || end >= fileSize) {
          return new NextResponse("Requested range not satisfiable", {
            status: 416,
            headers: { "Content-Range": `bytes */${fileSize}` },
          });
        }

        const chunkSize = end - start + 1;
        const nodeStream = fs.createReadStream(cachedFilePath, { start, end });
        const webStream = Readable.toWeb(nodeStream);

        return new NextResponse(webStream as any, {
          status: 206,
          headers: {
            "Content-Range": `bytes ${start}-${end}/${fileSize}`,
            "Accept-Ranges": "bytes",
            "Content-Length": chunkSize.toString(),
            "Content-Type": "audio/mpeg",
            "Cache-Control": "public, max-age=31536000, immutable",
          },
        });
      } else {
        const nodeStream = fs.createReadStream(cachedFilePath);
        const webStream = Readable.toWeb(nodeStream);

        return new NextResponse(webStream as any, {
          status: 200,
          headers: {
            "Content-Length": fileSize.toString(),
            "Content-Type": "audio/mpeg",
            "Accept-Ranges": "bytes",
            "Cache-Control": "public, max-age=31536000, immutable",
          },
        });
      }
    } catch (err) {
      console.error("Error streaming local audio file:", err);
    }
  }

  // 2. Direct instant streaming from Telegram with multi-part range mapping
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) {
    return new NextResponse("Telegram bot not configured", { status: 500 });
  }

  const manifestParts = findManifestEntry(fileId);
  const targetFileIds = (manifestParts && manifestParts.length > 0) ? manifestParts : [fileId];

  try {
    // Resolve metadata for all parts in parallel (instant ~100ms)
    const partsInfo = await Promise.all(
      targetFileIds.map((id) => getTelegramFileInfo(id, token))
    );

    const totalSize = partsInfo.reduce((sum, p) => sum + p.fileSize, 0);
    if (totalSize === 0) {
      return new NextResponse("Empty audio file", { status: 404 });
    }

    const rangeHeader = req.headers.get("range");
    let start = 0;
    let end = totalSize - 1;

    if (rangeHeader) {
      const match = rangeHeader.match(/bytes=(\d+)-(\d*)/);
      if (match) {
        start = parseInt(match[1], 10);
        if (match[2]) {
          end = parseInt(match[2], 10);
        }
      }
    }

    if (end >= totalSize) end = totalSize - 1;
    if (start > end || start >= totalSize) {
      return new NextResponse(null, {
        status: 416,
        headers: { "Content-Range": `bytes */${totalSize}` },
      });
    }

    // Determine which part contains the requested `start` offset
    let currentOffset = 0;
    let targetPartIndex = 0;
    let partStart = 0;

    for (let i = 0; i < partsInfo.length; i++) {
      const p = partsInfo[i];
      if (start < currentOffset + p.fileSize) {
        targetPartIndex = i;
        partStart = start - currentOffset;
        break;
      }
      currentOffset += p.fileSize;
    }

    const targetPart = partsInfo[targetPartIndex];
    const partRemaining = targetPart.fileSize - partStart;
    const requestedChunk = end - start + 1;
    // Serve up to what is available in this part (ExoPlayer requests remaining parts seamlessly)
    const chunkLength = Math.min(requestedChunk, partRemaining);
    const partEnd = partStart + chunkLength - 1;
    const actualEnd = start + chunkLength - 1;

    const tgRes = await fetch(
      `https://api.telegram.org/file/bot${token}/${targetPart.filePath}`,
      {
        headers: {
          Range: `bytes=${partStart}-${partEnd}`,
        },
      }
    );

    if (!tgRes.ok && tgRes.status !== 206) {
      return new NextResponse(`Source streaming error: ${tgRes.statusText}`, {
        status: tgRes.status,
      });
    }

    const responseHeaders = new Headers();
    responseHeaders.set("Content-Type", "audio/mpeg");
    responseHeaders.set("Accept-Ranges", "bytes");
    responseHeaders.set("Cache-Control", "public, max-age=31536000, immutable");
    responseHeaders.set("Content-Length", String(chunkLength));
    responseHeaders.set("Content-Range", `bytes ${start}-${actualEnd}/${totalSize}`);

    return new NextResponse(tgRes.body, {
      status: 206,
      headers: responseHeaders,
    });
  } catch (err: any) {
    console.error("Audio streaming error:", err);
    return new NextResponse(err?.message || "Streaming failed", { status: 500 });
  }
}
