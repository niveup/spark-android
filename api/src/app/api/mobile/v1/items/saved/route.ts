import { NextResponse } from "next/server";
import { z } from "zod";
import { fail } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { decodeCursor, encodeCursor, toFeedItem } from "@/lib/feed";

const SavedQuery = z.object({
  limit: z.coerce.number().int().min(1).max(50).default(20),
  cursor: z.string().optional(),
  q: z.string().max(200).optional(),
});

// Contract: GET /items/saved — most-recently-saved first, substring search.
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const url = new URL(req.url);
  const parsed = SavedQuery.safeParse(Object.fromEntries(url.searchParams));
  if (!parsed.success) return fail("VALIDATION", "Bad query params.");
  const q = parsed.data;

  const states = await db.itemState.findMany({
    where: { tokenHash: authed.tokenHash, saved: true },
    orderBy: { updatedAt: "desc" },
    include: { item: { include: { book: true } } },
  });
  let items = states.map((s) => s.item).filter((i) => i.status === "published");
  if (q.q) {
    const needle = q.q.toLowerCase();
    items = items.filter(
      (i) =>
        i.title.toLowerCase().includes(needle) ||
        i.bodyMd.toLowerCase().includes(needle),
    );
  }
  const offset = decodeCursor(q.cursor ?? null);
  const page = items.slice(offset, offset + q.limit);
  const nextOffset = offset + page.length;
  return NextResponse.json({
    data: page.map(toFeedItem),
    nextCursor: nextOffset >= items.length ? null : encodeCursor(nextOffset),
  });
}
