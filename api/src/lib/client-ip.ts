// Resolves the caller IP used as the rate-limit key.
//
// Trust order:
//   1. x-vercel-forwarded-for — set by Vercel, cannot be spoofed by the client.
//   2. x-real-ip             — set by Vercel and most reverse proxies.
//   3. last hop of x-forwarded-for — the value appended by the closest proxy.
//
// The first XFF value is deliberately NOT used: clients can prepend anything,
// which would let an attacker rotate the rate-limit key on every request.
// Outside Vercel, configure the proxy to overwrite x-real-ip; otherwise no
// header is fully trustworthy.
export function clientIp(req: Request): string {
  const vercel = firstHeaderIp(req.headers.get("x-vercel-forwarded-for"));
  if (vercel) return vercel;

  const realIp = req.headers.get("x-real-ip")?.trim();
  if (realIp) return realIp;

  return lastHeaderIp(req.headers.get("x-forwarded-for")) ?? "unknown-ip";
}

function firstHeaderIp(value: string | null): string | null {
  if (!value) return null;
  return value.split(",").map((part) => part.trim()).find(Boolean) ?? null;
}

function lastHeaderIp(value: string | null): string | null {
  if (!value) return null;
  const parts = value.split(",").map((part) => part.trim()).filter(Boolean);
  return parts[parts.length - 1] ?? null;
}
