import { NextResponse } from "next/server";
import { syncTelegramUpdates } from "@/lib/telegram";

export async function GET() {
  try {
    const result = await syncTelegramUpdates();
    return NextResponse.json({ ok: true, ...result });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return NextResponse.json({ ok: false, error: message }, { status: 500 });
  }
}
