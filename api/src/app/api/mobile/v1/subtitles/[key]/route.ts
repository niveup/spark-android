import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import { ok, fail } from "@/lib/api";

const SUBTITLES_DIR = path.join(process.cwd(), "src", "lib", "subtitles");
const subtitleMemoryCache = new Map<string, any[]>();

function findSubtitleFile(key: string): string | null {
  const safeKey = key.replace(/[^a-zA-Z0-9_\-]/g, "");
  const keysToTry = [safeKey];

  if (safeKey.startsWith("the-laws-of-human-nature_")) {
    keysToTry.push(safeKey.replace("the-laws-of-human-nature_", "human_nature_"));
  } else if (safeKey.startsWith("human_nature_")) {
    keysToTry.push(safeKey.replace("human_nature_", "the-laws-of-human-nature_"));
  }

  for (const k of keysToTry) {
    const p = path.join(SUBTITLES_DIR, `${k}.json`);
    if (fs.existsSync(p)) return p;
  }
  return null;
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ key: string }> }
) {
  const { key } = await params;
  if (!key) return fail("VALIDATION", "Missing subtitle key");

  const safeKey = key.replace(/[^a-zA-Z0-9_\-]/g, "");
  if (subtitleMemoryCache.has(safeKey)) {
    const cachedData = subtitleMemoryCache.get(safeKey)!;
    const res = ok(cachedData);
    res.headers.set("Cache-Control", "public, s-maxage=86400, stale-while-revalidate=604800");
    return res;
  }

  const filePath = findSubtitleFile(key);
  if (!filePath) {
    return ok([]);
  }

  try {
    const content = JSON.parse(fs.readFileSync(filePath, "utf-8"));
    subtitleMemoryCache.set(safeKey, content);
    const res = ok(content);
    res.headers.set("Cache-Control", "public, s-maxage=86400, stale-while-revalidate=604800");
    return res;
  } catch {
    return ok([]);
  }
}
