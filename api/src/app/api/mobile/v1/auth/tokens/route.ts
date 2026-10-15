import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

// Contract: GET /auth/tokens — list devices (lost-phone case). No raw tokens.
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const rows = await db.deviceToken.findMany({
    where: { revokedAt: null },
    orderBy: { createdAt: "desc" },
    select: { id: true, deviceLabel: true, createdAt: true, lastUsedAt: true },
  });
  return ok(
    rows.map((r) => ({
      id: r.id,
      deviceLabel: r.deviceLabel,
      createdAt: r.createdAt.toISOString(),
      lastUsedAt: r.lastUsedAt?.toISOString() ?? null,
    })),
  );
}
