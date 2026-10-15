import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

// Contract: DELETE /auth/tokens/:id — revoke one device.
export async function DELETE(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  const { id } = await params;
  const row = await db.deviceToken.findUnique({ where: { id } });
  if (!row || row.revokedAt) return fail("NOT_FOUND", "Device not found.");
  await db.deviceToken.delete({ where: { id } });
  return ok({ revoked: true });
}
