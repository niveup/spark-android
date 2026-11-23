import { NextResponse } from "next/server";
import { fail, ok } from "@/lib/api";
import { requireAuth } from "@/lib/auth";
import { db } from "@/lib/db";

const ALLOWED_BOOK_TITLES = [
  "the 48 laws of power",
  "the laws of human nature",
  "the art of war",
  "atomic habits",
  "what is life",
  "why i am an atheist",
];

function isAllowedBook(title: string): boolean {
  const normalized = title.toLowerCase().replace(/[^a-z0-9]/g, " ").replace(/\s+/g, " ").trim();
  return ALLOWED_BOOK_TITLES.some((allowed) => {
    const normAllowed = allowed.toLowerCase().replace(/[^a-z0-9]/g, " ").replace(/\s+/g, " ").trim();
    return normalized.includes(normAllowed) || normAllowed.includes(normalized);
  });
}

// Contract: GET /api/mobile/v1/books — list all books with covers, chapters & audio
export async function GET(req: Request) {
  const authed = await requireAuth(req);
  if (!authed) return fail("UNAUTHORIZED", "Login required.");

  const books = await db.book.findMany({
    orderBy: { createdAt: "asc" },
    include: {
      items: {
        where: { status: "published" },
        orderBy: { createdAt: "asc" },
      },
    },
  });

  const allowedBooks = books.filter((b) => isAllowedBook(b.title));

  const data = allowedBooks.map((b) => ({
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
