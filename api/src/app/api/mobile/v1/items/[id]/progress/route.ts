import { z } from "zod";
import { fail, ok } from "@/lib/api";
import { rateLimited, requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

const ProgressBody = z
  .object({
    positionMs: z.number().int().min(0),
    durationMs: z.number().int().min(0),
  })
  .refine((v) => v.positionMs <= v.durationMs, { message: "positionMs <= durationMs required." });

// Contract: PUT /items/:id/progress — audiobook resume position (Phase 6).
// Last-write-wins; app sends at most every 10s and on pause/kill.
export async function PUT(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  if (rateLimited(`write:${authed.tokenHash}`, 60)) {
    return fail("RATE_LIMITED", "Slow down.");
  }
  const { id } = await params;
  const item = await db.feedItem.findUnique({ where: { id } });
  if (!item) return fail("NOT_FOUND", "Item not found.");
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = ProgressBody.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "positionMs <= durationMs, both >= 0.");
  await db.itemState.upsert({
    where: { tokenHash_itemId: { tokenHash: authed.tokenHash, itemId: id } },
    create: {
      tokenHash: authed.tokenHash,
      itemId: id,
      progressMs: parsed.data.positionMs,
      progressDurationMs: parsed.data.durationMs,
    },
    update: { progressMs: parsed.data.positionMs, progressDurationMs: parsed.data.durationMs },
  });
  return ok({ itemId: id, positionMs: parsed.data.positionMs, updatedAt: new Date().toISOString() });
}
