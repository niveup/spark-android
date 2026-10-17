import { NextResponse } from "next/server";
import { z } from "zod";
import { fail } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { decodeCursor, encodeCursor, toFeedItem } from "@/lib/feed";

// Phase 3 review gate: drafts are worker output awaiting human publish.
// Same single-user token auth as the mobile API.
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const url = new URL(req.url);
  const limit = Math.min(Math.max(Number(url.searchParams.get("limit") ?? 20), 1), 50);
  const offset = decodeCursor(url.searchParams.get("cursor"));
  const drafts = await db.feedItem.findMany({
    where: { status: "draft" },
    include: { book: true },
    orderBy: { createdAt: "desc" },
  });
  const page = drafts.slice(offset, offset + limit);
  const nextOffset = offset + page.length;
  return NextResponse.json({
    data: page.map((it) => toFeedItem(it)),
    nextCursor: nextOffset >= drafts.length ? null : encodeCursor(nextOffset),
  });
}

const PublishBody = z.object({ ids: z.array(z.string().min(1)).min(1).max(100) });

export async function POST(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = PublishBody.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "ids[1..100] required.");
  const now = new Date();
  const result = await db.feedItem.updateMany({
    where: { id: { in: parsed.data.ids }, status: "draft" },
    data: { status: "published", publishedAt: now },
  });
  return NextResponse.json({ data: { published: result.count } });
}
