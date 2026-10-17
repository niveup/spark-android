// Phase 3b growth-card builders. Pure transforms: live data in, validated
// candidate cards out. All cards stay inside the CardSchema limits enforced
// by src/lib/ingest.ts (title<=80, body<=600, tags 1-5, etc).
import type { BookEntry, ConceptTarget, QuoteTarget } from "../../prisma/growth-types";
import type { LibrivoxDetailedBook } from "./adapters";
import type { BartlettQuote } from "./bartlett";

export type GrowthCandidate = {
  bookSlug: string | null;
  card: Record<string, unknown>;
};

// SECURITY/QUALITY NOTE: source text (book openers, wiki extracts, quotes) is
// copied verbatim and trimmed to card limits. No strings from sources are
// interpreted — a prompt inside a text cannot affect anything here.

export function chapterCandidates(
  entry: BookEntry,
  lvx: LibrivoxDetailedBook,
  cap: number,
): GrowthCandidate[] {
  return lvx.sections.slice(0, Math.max(0, cap)).map((s) => ({
    bookSlug: entry.slug,
    card: {
      type: "audio",
      title: `${entry.title} — ${s.title}`.slice(0, 80),
      bodyMd:
        `Section ${s.sectionNumber} of the public-domain LibriVox reading of ` +
        `"${entry.title}" by ${entry.author}. Streams from Archive.org — nothing ` +
        `downloads to your phone. (${s.playtimeSec} s)`.slice(0, 600),
      actionLine: "Do it now: play it on a walk, or lying on the floor.",
      topicTags: entry.topicTags.slice(0, 5),
      language: "en",
      estSeconds: Math.max(10, Math.min(s.playtimeSec, 7200)),
      sourceType: "librivox",
      sourceRef: `LibriVox #${lvx.librivoxId} §${s.sectionNumber} — ${lvx.pageUrl}`.slice(0, 300),
      license: "public domain",
      isAiGenerated: false,
      isGrounded: true,
      modelUsed: null,
      quiz: null,
      audioUrl: s.listenUrl,
      durationMs: Math.max(1000, Math.round(s.playtimeSec * 1000)),
    },
  }));
}

// Gutenberg body starts right after the license marker: producer credits,
// title pages and contents come first — skip them and take real paragraphs.
export function openingExcerpt(bodyStart: string, maxChars = 430): string | null {
  const junk =
    /^(Produced by|Credits|Title|Author|Release date|Language|More|Contents|CONTENTS|TRANSCRIBER|preferably|This eBook|Cover)/i;
  const blocks = bodyStart
    .split(/\n\s*\n/)
    .map((b) => b.replace(/\s*\n\s*/g, " ").trim())
    .filter((b) => b.length >= 80 && !junk.test(b));
  if (blocks.length === 0) return null;
  const text = `${blocks[0] ?? ""} ${blocks[1] ?? ""}`.trim();
  if (text.length < 100) return null;
  return text.slice(0, maxChars);
}

export function bookIdeaCandidates(
  entry: BookEntry,
  opening: string | null,
): GrowthCandidate[] {
  const excerpt = opening === null ? null : openingExcerpt(opening);
  if (entry.gutenbergId === null || excerpt === null) return [];
  return [
    {
      bookSlug: entry.slug,
      card: {
        type: "book_idea",
        title: `Why read ${entry.title}`.slice(0, 80),
        bodyMd: `Opening lines of this public-domain classic:\n\n${excerpt}`.slice(0, 600),
        actionLine: entry.actionIdea,
        topicTags: entry.topicTags.slice(0, 5),
        language: "en",
        estSeconds: 60,
        sourceType: "gutenberg",
        sourceRef: `ebook #${entry.gutenbergId}`,
        license: "public domain",
        isAiGenerated: false,
        isGrounded: true,
        modelUsed: null,
        quiz: null,
        audioUrl: null,
        durationMs: null,
      },
    },
  ];
}

export function quoteCandidates(
  targets: QuoteTarget[],
  quotes: BartlettQuote[],
  capPerAuthor: number,
): GrowthCandidate[] {
  const out: GrowthCandidate[] = [];
  const perAuthor = new Map<string, number>();
  for (const q of quotes) {
    const target = targets.find((t) => q.header.toUpperCase().includes(t.match.toUpperCase()));
    if (target === undefined) continue;
    const count = perAuthor.get(q.header) ?? 0;
    if (count >= capPerAuthor) continue;
    perAuthor.set(q.header, count + 1);
    out.push({
      bookSlug: null,
      card: {
        type: "quote",
        title: target.authorName.slice(0, 80),
        bodyMd: `"${q.quote}"`.slice(0, 600),
        actionLine: target.actionLine,
        topicTags: target.topicTags.slice(0, 5),
        language: "en",
        estSeconds: 45,
        sourceType: "bartletts-1905",
        sourceRef:
          `Bartlett's Familiar Quotations, 9th ed. 1905 — PG #27889 — ${q.work}`.slice(0, 300),
        license: "public domain",
        isAiGenerated: false,
        isGrounded: true,
        modelUsed: null,
        quiz: null,
        audioUrl: null,
        durationMs: null,
      },
    });
  }
  return out;
}

export type WikiSummaryLike = { title: string; extract: string; pageUrl: string };

export function conceptCandidates(
  targets: ConceptTarget[],
  summaries: (WikiSummaryLike | null)[],
): GrowthCandidate[] {
  const out: GrowthCandidate[] = [];
  for (let i = 0; i < targets.length; i++) {
    const summary = summaries[i];
    if (summary === null || summary === undefined) continue;
    const extract = summary.extract.trim();
    if (extract.length < 120) continue;
    if (/may refer to/i.test(extract.slice(0, 120))) continue; // disambiguation page
    const target = targets[i];
    if (target === undefined) continue;
    out.push({
      bookSlug: null,
      card: {
        type: "concept",
        title: summary.title.slice(0, 80),
        bodyMd: extract.slice(0, 600),
        actionLine: target.actionLine,
        topicTags: target.topicTags.slice(0, 5),
        language: "en",
        estSeconds: 60,
        sourceType: "wikipedia",
        sourceRef: `Wikipedia: ${summary.title} — ${summary.pageUrl}`.slice(0, 300),
        license: "CC BY-SA 4.0",
        isAiGenerated: false,
        isGrounded: true,
        modelUsed: null,
        quiz: null,
        audioUrl: null,
        durationMs: null,
      },
    });
  }
  return out;
}
