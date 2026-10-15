import { z } from "zod";
import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

const CloseSession = z.object({
  itemsSeen: z.number().int().min(0),
  limitReached: z.boolean(),
});

// Contract: PATCH /sessions/:id — close a session.
export async function PATCH(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const { id } = await params;
  const session = await db.feedSession.findUnique({ where: { id } });
  if (!session) return fail("NOT_FOUND", "Session not found.");
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = CloseSession.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "itemsSeen, limitReached required.");
  const closed = await db.feedSession.update({
    where: { id },
    data: { endedAt: new Date(), itemsSeen: parsed.data.itemsSeen, limitReached: parsed.data.limitReached },
  });
  return ok({ sessionId: closed.id, endedAt: closed.endedAt?.toISOString() ?? null });
}
