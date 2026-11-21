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

// Helper to resolve Telegram file_path
async function getTelegramFilePath(fileId: string, token: string): Promise<string> {
  const fileRes = await fetch(
    `https://api.telegram.org/bot${token}/getFile?file_id=${encodeURIComponent(fileId)}`
  );
  const fileJson = (await fileRes.json()) as {
    ok: boolean;
    result?: { file_path?: string };
  };

  if (!fileJson.ok || !fileJson.result?.file_path) {
    throw new Error(`File not found on Telegram for ID: ${fileId}`);
  }

  return fileJson.result.file_path;
}

// Helper to download a single file from Telegram (for multi-part concatenation)
async function downloadTelegramFile(fileId: string, token: string): Promise<Buffer> {
  const filePath = await getTelegramFilePath(fileId, token);
  const audioRes = await fetch(
    `https://api.telegram.org/file/bot${token}/${filePath}`
  );

  if (!audioRes.ok) {
    throw new Error(`Failed to download audio from Telegram: ${audioRes.statusText}`);
  }

  const arrayBuffer = await audioRes.arrayBuffer();
  return Buffer.from(arrayBuffer);
}

// Find multi-part chapter definition across all manifests
function findManifestEntry(fileId: string): string[] | null {
  if (!fs.existsSync(MANIFESTS_DIR)) return null;
  const files = fs.readdirSync(MANIFESTS_DIR);
  for (const f of files) {
    if (f.endsWith(".json")) {
      try {
        const list = JSON.parse(fs.readFileSync(path.join(MANIFESTS_DIR, f), "utf-8"));
        const item = list.find((it: any) => it.audio_key === fileId || it.audioKey === fileId);
        if (item) {
          const ids = item.telegram_file_ids || item.telegramFileIds;
          if (Array.isArray(ids) && ids.length > 0) return ids;
        }
      } catch {}
    }
  }
  return null;
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ fileId: string }> }
) {
  const { fileId } = await params;
  if (!fileId) {
    return new NextResponse("Missing file ID", { status: 400 });
  }

  // Resolve cached file from local disk or serverless writable /tmp
  let cachedFilePath: string | null = null;
  const localCandidate = path.join(LOCAL_AUDIO_DIR, `${fileId}.mp3`);
  const tmpCandidate = path.join(AUDIO_TMP_DIR, `${fileId}.mp3`);

  if (fs.existsSync(localCandidate)) {
    cachedFilePath = localCandidate;
  } else if (fs.existsSync(tmpCandidate)) {
    cachedFilePath = tmpCandidate;
  }

  // 1. If not cached, stream or concatenate from Telegram
  if (!cachedFilePath) {
    const token = process.env.TELEGRAM_BOT_TOKEN;
    if (!token) {
      return new NextResponse("Telegram bot not configured", { status: 500 });
    }

    const manifestParts = findManifestEntry(fileId);

    // Multi-part chapter: concatenate into temporary disk file first
    if (manifestParts && manifestParts.length > 1) {
      try {
        if (!fs.existsSync(AUDIO_TMP_DIR)) {
          fs.mkdirSync(AUDIO_TMP_DIR, { recursive: true });
        }

        const buffers: Buffer[] = [];
        for (const tgId of manifestParts) {
          const buf = await downloadTelegramFile(tgId, token);
          buffers.push(buf);
        }
        const combined = Buffer.concat(buffers);
        fs.writeFileSync(tmpCandidate, combined);
        cachedFilePath = tmpCandidate;
      } catch (err: any) {
        console.error("Error fetching multi-part audio from Telegram:", err);
        return new NextResponse(err?.message || "Internal server error", { status: 500 });
      }
    } else {
      // Single-file chapter: Stream directly from Telegram with Range support (INSTANT playback)
      const targetFileId = (manifestParts && manifestParts.length === 1) ? manifestParts[0] : fileId;
      try {
        const filePath = await getTelegramFilePath(targetFileId, token);
        const range = req.headers.get("range");
        const tgHeaders: Record<string, string> = {};
        if (range) tgHeaders["range"] = range;

        const tgRes = await fetch(
          `https://api.telegram.org/file/bot${token}/${filePath}`,
          { headers: tgHeaders }
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

        const cl = tgRes.headers.get("content-length");
        if (cl) responseHeaders.set("Content-Length", cl);

        const cr = tgRes.headers.get("content-range");
        if (cr) responseHeaders.set("Content-Range", cr);

        return new NextResponse(tgRes.body, {
          status: tgRes.status,
          headers: responseHeaders,
        });
      } catch (err: any) {
        console.error("Direct audio streaming error:", err);
        return new NextResponse(err?.message || "Streaming failed", { status: 500 });
      }
    }
  }

  // 2. Serve from disk with Range request support (HTTP 206)
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
          headers: {
            "Content-Range": `bytes */${fileSize}`,
          },
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
    console.error("Error streaming audio file:", err);
    return new NextResponse("Failed to stream audio file", { status: 500 });
  }
}
