import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import os from "os";

export const maxDuration = 60;

const CACHE_TMP_DIR = path.join(os.tmpdir(), "spark_covers");
const LOCAL_CACHE_DIR = path.join(process.cwd(), "public", "covers");

export async function GET(
  _req: Request,
  { params }: { params: Promise<{ fileId: string }> }
) {
  const { fileId } = await params;
  if (!fileId) {
    return new NextResponse("Missing file ID", { status: 400 });
  }

  // 1. Check if already cached on local disk or serverless /tmp
  try {
    const localFile = path.join(LOCAL_CACHE_DIR, `${fileId}.jpg`);
    const tmpFile = path.join(CACHE_TMP_DIR, `${fileId}.jpg`);

    const cachedFilePath = fs.existsSync(localFile)
      ? localFile
      : fs.existsSync(tmpFile)
      ? tmpFile
      : null;

    if (cachedFilePath) {
      const fileBuffer = fs.readFileSync(cachedFilePath);
      return new NextResponse(fileBuffer, {
        status: 200,
        headers: {
          "Content-Type": "image/jpeg",
          "Cache-Control": "public, max-age=31536000, immutable",
        },
      });
    }
  } catch (err) {
    console.warn("Server disk cache check error:", err);
  }

  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) {
    return new NextResponse("Telegram bot not configured", { status: 500 });
  }

  try {
    // 2. Get file path from Telegram
    const fileRes = await fetch(
      `https://api.telegram.org/bot${token}/getFile?file_id=${encodeURIComponent(fileId)}`
    );
    const fileJson = (await fileRes.json()) as {
      ok: boolean;
      result?: { file_path?: string };
    };

    if (!fileJson.ok || !fileJson.result?.file_path) {
      return new NextResponse("File not found on Telegram", { status: 404 });
    }

    const filePath = fileJson.result.file_path;

    // 3. Fetch the actual file stream from Telegram
    const imageRes = await fetch(
      `https://api.telegram.org/file/bot${token}/${filePath}`
    );

    if (!imageRes.ok) {
      return new NextResponse("Failed to download image from Telegram", {
        status: 502,
      });
    }

    const arrayBuffer = await imageRes.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    // 4. Save to temporary disk cache
    try {
      if (!fs.existsSync(CACHE_TMP_DIR)) {
        fs.mkdirSync(CACHE_TMP_DIR, { recursive: true });
      }
      const cachedFilePath = path.join(CACHE_TMP_DIR, `${fileId}.jpg`);
      fs.writeFileSync(cachedFilePath, buffer);
    } catch (saveErr) {
      console.warn("Could not save cover to disk cache:", saveErr);
    }

    return new NextResponse(buffer, {
      status: 200,
      headers: {
        "Content-Type": "image/jpeg",
        "Cache-Control": "public, max-age=31536000, immutable",
      },
    });
  } catch (err) {
    console.error("Telegram cover stream error:", err);
    return new NextResponse("Internal server error", { status: 500 });
  }
}
