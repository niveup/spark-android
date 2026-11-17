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
  const filePath = path.join(SUBTITLES_DIR, `${safeKey}.json`);

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
