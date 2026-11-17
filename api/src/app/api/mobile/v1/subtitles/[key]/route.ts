import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import { ok, fail } from "@/lib/api";

const SUBTITLES_DIR = path.join(process.cwd(), "public", "subtitles");

export async function GET(
  req: Request,
  { params }: { params: Promise<{ key: string }> }
) {
  const { key } = await params;
  if (!key) return fail("VALIDATION", "Missing subtitle key");

  const safeKey = key.replace(/[^a-zA-Z0-9_\-]/g, "");
  let filePath = path.join(SUBTITLES_DIR, `${safeKey}.json`);

  if (!fs.existsSync(filePath)) {
    if (safeKey.startsWith("the-laws-of-human-nature_")) {
      const alias = safeKey.replace("the-laws-of-human-nature_", "human_nature_");
      const aliasPath = path.join(SUBTITLES_DIR, `${alias}.json`);
      if (fs.existsSync(aliasPath)) filePath = aliasPath;
    } else if (safeKey.startsWith("human_nature_")) {
      const alias = safeKey.replace("human_nature_", "the-laws-of-human-nature_");
      const aliasPath = path.join(SUBTITLES_DIR, `${alias}.json`);
      if (fs.existsSync(aliasPath)) filePath = aliasPath;
    }
  }

  if (!fs.existsSync(filePath)) {
    return ok([]);
  }

  try {
    const content = JSON.parse(fs.readFileSync(filePath, "utf-8"));
    return ok(content);
  } catch {
    return ok([]);
  }
}
