import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

// Contract: GET /stats (additive v1.1) — personal totals for the Me tab.
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const now = new Date();
  const [seenTotal, savedTotal, likedTotal, quizzesDue, sessionsTotal] =
    await Promise.all([
      db.itemState.count({
        where: { tokenHash: authed.tokenHash, seenAt: { not: null } },
      }),
      db.itemState.count({
        where: { tokenHash: authed.tokenHash, saved: true },
      }),
      db.itemState.count({
        where: { tokenHash: authed.tokenHash, liked: true },
      }),
      db.itemState.count({
        where: { tokenHash: authed.tokenHash, srsDueAt: { lte: now } },
      }),
      db.feedSession.count({ where: { endedAt: { not: null } } }),
    ]);
  return ok({ seenTotal, savedTotal, likedTotal, quizzesDue, sessionsTotal });
}
