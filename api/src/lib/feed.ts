import type { Book, FeedItem as DbItem, ItemState as DbState } from "../../generated/prisma/client";

// Contract FeedItem serializer (DB row -> API shape).
export type ApiFeedItem = {
  id: string;
  type: string;
  title: string;
  bodyMd: string;
  actionLine: string;
  topicTags: string[];
  language: string;
  estSeconds: number;
  progressMs: number | null; // Phase 6 audiobook resume; null until then
  quiz: { options: string[]; correctIndex: number; explanation: string } | null;
  audioUrl: string | null;
  durationMs: number | null;
  book: { id: string; title: string; author: string; coverUrl: string | null } | null;
  sourceType: string;
  sourceRef: string;
  license: string;
  isAiGenerated: boolean;
  isGrounded: boolean;
  // Per-device interaction state (additive v1.1): the app renders ♥/👍 from the
  // server on a fresh session instead of guessing.
  saved: boolean;
  liked: boolean | null; // null = no rating
  dismissed: boolean;
  publishedAt: string;
};

export type ItemWithBook = DbItem & { book: Book | null };

export function toFeedItem(item: ItemWithBook, state?: DbState | null): ApiFeedItem {
  const quizRaw = item.quizJson as {
    options?: string[];
    correctIndex?: number;
    explanation?: string;
  } | null;
  return {
    id: item.id,
    type: item.type,
    title: item.title,
    bodyMd: item.bodyMd,
    actionLine: item.actionLine,
    topicTags: item.topicTags,
    language: item.language,
    estSeconds: item.estSeconds,
    progressMs: null,
    quiz:
      item.type === "quiz" && quizRaw
        ? {
            options: quizRaw.options ?? [],
            correctIndex: quizRaw.correctIndex ?? 0,
            explanation: quizRaw.explanation ?? "",
          }
        : null,
    audioUrl: item.audioUrl,
    durationMs: item.durationMs,
    book: item.book
      ? { id: item.book.id, title: item.book.title, author: item.book.author ?? "", coverUrl: item.book.coverUrl }
      : null,
    sourceType: item.sourceType,
    sourceRef: item.sourceRef,
    license: item.license,
    isAiGenerated: item.isAiGenerated,
    isGrounded: item.isGrounded,
    saved: state?.saved ?? false,
    liked: state?.liked ?? null,
    dismissed: state?.dismissed ?? false,
    publishedAt: (item.publishedAt ?? item.createdAt).toISOString(),
  };
}

export type ApiItemState = {
  itemId: string;
  seenAt: string | null;
  saved: boolean;
  liked: boolean | null;
  dismissed: boolean;
  dwellMs: number;
  srsDueAt: string | null;
};

export function toItemState(itemId: string, s: DbState): ApiItemState {
  return {
    itemId,
    seenAt: s.seenAt?.toISOString() ?? null,
    saved: s.saved,
    liked: s.liked,
    dismissed: s.dismissed,
    dwellMs: s.dwellMs,
    srsDueAt: s.srsDueAt?.toISOString() ?? null,
  };
}

// --- Ranking v1 (contract §GET /feed, no ML) ---
// 1. SRS-due first · 2. unseen from liked/saved topics · 3. ~20% exploration
// 4. never seen-within-30d unless saved. Deterministic per seed (sessionId).
// Rule 1 outranks rule 4: a due review must resurface even if recently seen,
// otherwise SRS intervals (1-3-7 days) could never fire inside the 30-day block.

// Minimal shapes so the ranker is unit-testable with plain objects (no DB rows).
export type RankableItem = { id: string; topicTags: string[] };
export type RankableState = {
  itemId: string;
  seenAt: Date | null;
  saved: boolean;
  liked: boolean | null;
  srsDueAt: Date | null;
};

export function hashSeed(seed: string): number {
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export function mulberry32(seed: number): () => number {
  let a = seed;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function rankFeed<T extends RankableItem>(
  items: T[],
  statesById: Map<string, RankableState>,
  seed: string,
  /** Refill mode (feed?excludeSeen=false): recently seen cards may come back. */
  includeSeen = false,
): T[] {
  const now = Date.now();
  const thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000;
  const likedTopics = new Set<string>();
  for (const s of statesById.values()) {
    if (s.saved || s.liked === true) {
      const item = items.find((i) => i.id === s.itemId);
      item?.topicTags.forEach((t) => likedTopics.add(t));
    }
  }
  const due: T[] = [];
  const preferred: T[] = [];
  const explore: T[] = [];
  for (const item of items) {
    const st = statesById.get(item.id);
    if (st?.srsDueAt && st.srsDueAt.getTime() <= now) {
      due.push(item); // rule 1 — before rule 4 on purpose (see header)
      continue;
    }
    const seenRecently = !!st?.seenAt && st.seenAt.getTime() > thirtyDaysAgo;
    // Rule 4 normally drops recently seen cards; refill mode keeps them so the
    // app can still serve something once every card has been read.
    if (seenRecently && !st?.saved && !includeSeen) continue;
    if (st?.seenAt) {
      explore.push(item); // seen long ago / saved refill
      continue;
    }
    if (item.topicTags.some((t) => likedTopics.has(t))) preferred.push(item);
    else explore.push(item);
  }
  // ~20% exploration interleaved deterministically (rule 3). Explore items
  // are shuffled first: the DB returns publish order (oldest first), and
  // without a shuffle the same oldest items would hold the top slots forever
  // while freshly published items stay unreachable at the tail.
  const rand = mulberry32(hashSeed(seed));
  for (let i = explore.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    const t = explore[i];
    explore[i] = explore[j];
    explore[j] = t;
  }
  const ordered = [...due];
  const exp: T[] = [];
  const rest: T[] = [];
  for (const item of explore) {
    (rand() < 0.2 ? exp : rest).push(item);
  }
  const main = [...preferred, ...rest];
  let ei = 0;
  for (let i = 0; i < main.length; i++) {
    ordered.push(main[i]!);
    if (ei < exp.length && i % 4 === 3) ordered.push(exp[ei++]!);
  }
  while (ei < exp.length) ordered.push(exp[ei++]!);
  return ordered;
}

export function encodeCursor(offset: number): string {
  return Buffer.from(JSON.stringify({ o: offset })).toString("base64url");
}

export function decodeCursor(cursor: string | null): number {
  if (!cursor) return 0;
  try {
    const parsed = JSON.parse(Buffer.from(cursor, "base64url").toString("utf8")) as {
      o?: number;
    };
    return Number.isInteger(parsed.o) && (parsed.o ?? 0) >= 0 ? (parsed.o as number) : 0;
  } catch {
    return 0; // unknown cursor -> first page (app treats cursors as opaque)
  }
}
