import { z } from "zod";
import { fail, ok } from "@/lib/api";
import { rateLimited, requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";
import { applyStateUpdate, serializeState } from "@/lib/state";

const SingleUpdate = z
  .object({
    seen: z.boolean().optional(),
    saved: z.boolean().optional(),
    liked: z.boolean().nullable().optional(),
    dismissed: z.boolean().optional(),
    dwellMs: z.number().int().min(0).optional(),
    quizCorrect: z.boolean().nullable().optional(),
    clientEventId: z.string().min(1).max(128).optional(),
  })
  .refine((v) => Object.keys(v).length > 0, { message: "At least one field required." });

// Contract: POST /items/:id/state — upsert interaction state (single).
export async function POST(
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
  const parsed = SingleUpdate.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "At least one state field required.");
  const { state } = await applyStateUpdate(authed.tokenHash, id, parsed.data);
  return ok(serializeState(id, state));
}
