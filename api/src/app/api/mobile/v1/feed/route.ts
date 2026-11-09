import { z } from "zod";
import { NextResponse } from "next/server";
import { fail } from "@/lib/api";
import { rateLimited, requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { decodeCursor, encodeCursor, rankFeed, toFeedItem } from "@/lib/feed";

const FeedQuery = z.object({
  limit: z.coerce.number().int().min(1).max(50).default(20),
  cursor: z.string().optional(),
  sessionId: z.string().optional(),
  excludeSeen: z
    .enum(["true", "false"])
    .default("true")
    .transform((v) => v === "true"),
  topics: z.string().optional(),
});

// Contract: GET /feed — ranked session feed, cursor pages.
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  if (rateLimited(`feed:${authed.tokenHash}`, 30)) {
    return fail("RATE_LIMITED", "Slow down.");
  }
  const url = new URL(req.url);
  const parsed = FeedQuery.safeParse(Object.fromEntries(url.searchParams));
  if (!parsed.success) return fail("VALIDATION", "Bad query params.");
  const q = parsed.data;

  const items = await db.feedItem.findMany({
    where: { status: "published" },
    include: { book: true },
    orderBy: { publishedAt: "asc" },
  });
  const states = await db.itemState.findMany({ where: { tokenHash: authed.tokenHash } });
  const statesById = new Map(states.map((s) => [s.itemId, s]));

  // excludeSeen=false is refill mode: seen cards are eligible again (saved
  // first), which is what the app's "Read them again" button asks for.
  let ranked = rankFeed(items, statesById, q.sessionId ?? authed.tokenHash, !q.excludeSeen);
  if (q.topics) {
    const boost = new Set(q.topics.split(",").map((t) => t.trim()).filter(Boolean));
    if (boost.size > 0) {
      ranked = [...ranked].sort((a, b) => {
        const ab = a.topicTags.some((t) => boost.has(t)) ? 0 : 1;
        const bb = b.topicTags.some((t) => boost.has(t)) ? 0 : 1;
        return ab - bb;
      });
    }
  }
  if (!q.excludeSeen) {
    // Refill mode: seen items may reappear (saved first).
    const seenFirst = [...ranked].sort((a, b) => {
      const as = statesById.get(a.id)?.saved ? 0 : 1;
      const bs = statesById.get(b.id)?.saved ? 0 : 1;
      return as - bs;
    });
    ranked = seenFirst;
  }

  const offset = decodeCursor(q.cursor ?? null);
  const page = ranked.slice(offset, offset + q.limit);
  const nextOffset = offset + page.length;
  // Contract paged shape: { data, nextCursor } at top level (not nested).
  return NextResponse.json({
    data: page.map((it) => toFeedItem(it, statesById.get(it.id))),
    nextCursor: nextOffset >= ranked.length ? null : encodeCursor(nextOffset),
  });
}
