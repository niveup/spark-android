import { NextResponse } from "next/server";

// Contract: GET /health — unauthenticated probe.
// Used by the app's offline banner to tell "server down" from "phone offline".
export async function GET() {
  return NextResponse.json({ data: { ok: true, version: "v1" } });
}
