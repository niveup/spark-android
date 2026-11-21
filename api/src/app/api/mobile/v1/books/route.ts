import { NextResponse } from "next/server";
import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

// Contract: GET /api/mobile/v1/books — list all books with covers, chapters & audio
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");

  const books = await db.book.findMany({
    where: {
      NOT: {
        title: { contains: "Sherlock", mode: "insensitive" },
      },
    },
    orderBy: { createdAt: "asc" },
    include: {
      items: {
        where: { status: "published" },
        orderBy: { createdAt: "asc" },
      },
    },
  });

  const data = books.map((b) => ({
    id: b.id,
    title: b.title,
    author: b.author || "Unknown",
    coverUrl: b.coverUrl,
    isEditorsChoice: b.isEditorsChoice ?? false,
    category: b.category ?? null,
    hasAudio: !!b.audioUrl || b.items.some((it) => !!it.audioUrl),
    chapterCount: b.items.length,
    chapters: b.items.map((it) => ({
      id: it.id,
      title: it.title,
      type: it.type,
      audioUrl: it.audioUrl,
      durationMs: it.durationMs,
      actionLine: it.actionLine,
    })),
  }));

  const response = ok(data);
  response.headers.set(
    "Cache-Control",
    "public, s-maxage=3600, stale-while-revalidate=86400"
  );
  return response;
}
