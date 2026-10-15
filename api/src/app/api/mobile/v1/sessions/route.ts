import { z } from "zod";
import { fail, created } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

const OpenSession = z.object({
  deviceId: z.string().min(1).max(128),
  cardLimit: z.number().int().min(1).max(100),
  minuteLimit: z.number().int().min(1).max(120),
});

// Contract: POST /sessions — open a feed session.
export async function POST(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = OpenSession.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "deviceId, cardLimit, minuteLimit required.");
  const session = await db.feedSession.create({ data: parsed.data });
  return created({
    sessionId: session.id,
    startedAt: session.startedAt.toISOString(),
  });
}
