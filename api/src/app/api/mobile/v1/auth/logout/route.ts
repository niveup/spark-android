import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

// Contract: POST /auth/logout — revoke current token.
export async function POST(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");
  await db.deviceToken.delete({ where: { id: authed.tokenId } });
  return ok({ revoked: true });
}
