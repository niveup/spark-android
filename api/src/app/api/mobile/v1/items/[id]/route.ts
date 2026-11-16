import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { toFeedItem } from "@/lib/feed";

// Contract: GET /items/:id — one published card with the caller's own state.
//
// The Saved tab stores only the card id; opening a saved card in the Reels
// tab maps that id back to the card with this endpoint. Offline the app looks
// the same id up in its local cache instead.
export async function GET(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const { id } = await params;
  const item = await db.feedItem.findUnique({ where: { id }, include: { book: true } });
  // Only published cards are readable — drafts and archived cards stay hidden.
  if (!item || item.status !== "published") return fail("NOT_FOUND", "Item not found.");
  const state = await db.itemState.findUnique({
    where: { tokenHash_itemId: { tokenHash: authed.tokenHash, itemId: id } },
  });
  return ok(toFeedItem(item, state));
}
