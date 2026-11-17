import { NextResponse } from "next/server";
import { processTelegramMessage } from "@/lib/telegram";

export async function POST(req: Request) {
  try {
    const update = await req.json();
    const msg = update.message || update.channel_post;
    if (msg) {
      await processTelegramMessage(msg);
    }
    return NextResponse.json({ ok: true });
  } catch (err: unknown) {
    console.error("Webhook processing error:", err);
    return NextResponse.json({ ok: false }, { status: 200 }); // Always 200 to Telegram so it doesn't spam retry
  }
}
