import { z } from "zod";
import { fail, ok } from "@/lib/api";
import { checkPasscode, clientIp, mintToken, rateLimited } from "@/lib/auth";
import { db } from "@/lib/db";

const LoginBody = z.object({
  passcode: z.string().min(1).max(128),
  deviceLabel: z.string().min(1).max(64),
});

// Contract: POST /auth/login — exchange passcode for token.
export async function POST(req: Request) {
  if (rateLimited(`login:${clientIp(req)}`, 5)) {
    return fail("RATE_LIMITED", "Too many login attempts. Wait a minute.");
  }
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return fail("VALIDATION", "Body must be JSON.");
  }
  const parsed = LoginBody.safeParse(body);
  if (!parsed.success) return fail("VALIDATION", "passcode and deviceLabel required.");
  if (!checkPasscode(parsed.data.passcode)) {
    return fail("UNAUTHORIZED", "Wrong passcode."); // same message every time
  }
  const { raw, hash } = mintToken();
  const expiresAt = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000);
  await db.deviceToken.create({
    data: { tokenHash: hash, deviceLabel: parsed.data.deviceLabel, expiresAt },
  });
  return ok({ token: raw, expiresAt: expiresAt.toISOString() });
}
