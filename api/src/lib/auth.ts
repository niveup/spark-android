import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { db } from "./db";

// Token auth (contract + ARCHITECTURE.md): passcode -> random 256-bit token,
// server stores only SHA-256 hash. Timing-safe passcode compare.

export function hashToken(raw: string): string {
  return createHash("sha256").update(raw, "utf8").digest("hex");
}

export function checkPasscode(input: string): boolean {
  const expected = process.env.SPARK_PASSCODE ?? "";
  if (!expected || !input) return false;
  const a = createHash("sha256").update(input, "utf8").digest();
  const b = createHash("sha256").update(expected, "utf8").digest();
  return a.length === b.length && timingSafeEqual(a, b);
}

export function mintToken(): { raw: string; hash: string } {
  const raw = randomBytes(32).toString("hex"); // 256-bit, hex per contract
  return { raw, hash: hashToken(raw) };
}

export type Authed = { tokenId: string; tokenHash: string };

export async function requireAuth(req: Request): Promise<Authed | null> {
  const header = req.headers.get("authorization") ?? "";
  const match = /^Bearer (.+)$/.exec(header.trim());
  if (!match?.[1]) return null;
  const tokenHash = hashToken(match[1]);
  const row = await db.deviceToken.findUnique({ where: { tokenHash } });
  if (!row || row.revokedAt) return null;
  if (row.expiresAt.getTime() < Date.now()) return null;
  await db.deviceToken.update({
    where: { id: row.id },
    data: { lastUsedAt: new Date() },
  });
  return { tokenId: row.id, tokenHash };
}

// --- Tiny in-memory rate limiter (single instance DEV; contract limits) ---
// login: 5/min per IP · writes: 60/min per token · feed: 30/min per token.
const hits = new Map<string, number[]>();

export function rateLimited(key: string, maxPerMinute: number): boolean {
  const now = Date.now();
  const windowStart = now - 60_000;
  const list = (hits.get(key) ?? []).filter((t) => t > windowStart);
  if (list.length >= maxPerMinute) {
    hits.set(key, list);
    return true;
  }
  list.push(now);
  hits.set(key, list);
  return false;
}

export function clientIp(req: Request): string {
  return (
    req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
    "unknown-ip"
  );
}
