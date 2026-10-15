import { z } from "zod";
import { fail, ok } from "@/lib/api";
import { rateLimited, requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { applyStateUpdate, serializeState } from "@/lib/state";

const BatchUpdate = z.object({
  itemId: z.string().min(1),
  clientEventId: z.string().min(1).max(128),
  seen: z.boolean().optional(),
  saved: z.boolean().optional(),
  liked: z.boolean().nullable().optional(),
  dismissed: z.boolean().optional(),
  dwellMs: z.number().int().min(0).optional(),
  quizCorrect: z.boolean().nullable().optional(),
});

const BatchBody = z.object({ updates: z.array(BatchUpdate).min(1).max(100) });

// Contract: POST /items/state — batch upsert (offline sync + swipe batching).
export async function POST(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  if (rateLimited(`write:${authed.tokenHash}`, 60)) {
    return fail("RATE_LIMITED", "Slow down.");
  }
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = BatchBody.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "updates[1..100] with itemId + clientEventId required.");

  const ids = [...new Set(parsed.data.updates.map((u) => u.itemId))];
  const found = await db.feedItem.findMany({ where: { id: { in: ids } }, select: { id: true } });
  const known = new Set(found.map((f) => f.id));

  const states = [];
  const rejected: string[] = [];
  for (const update of parsed.data.updates) {
    if (!known.has(update.itemId)) {
      rejected.push(update.itemId);
      continue;
    }
    const { state } = await applyStateUpdate(authed.tokenHash, update.itemId, update);
    states.push(serializeState(update.itemId, state));
  }
  return ok({ states, rejected });
}
