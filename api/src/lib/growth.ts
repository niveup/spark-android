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
// Blocks printed under an "[Illustration: ..." caption can merge the caption
// quote ("... --Some Name.]") with the first real paragraph when Gutenberg
// uses only a single newline between them. Cut through the attribution
// bracket so the card gets the real paragraph.
export function stripMergedCaption(block: string): string {
  if (!/^[ \t]*["\u201c]/.test(block)) return block;
  // Bracketed attribution: "... --Some Name.] REAL TEXT"
  const idx = block.indexOf("]");
  if (idx >= 0 && idx < 600 && /--/.test(block.slice(0, idx))) {
    return block.slice(idx + 1).trim();
  }
  // Bracketless duplicate of the same caption: "... --Some Name. REAL TEXT"
  const m = /--[A-Z][\w .'’-]*\.[ \t]+(?=["\u201cA-Z])/.exec(block.slice(0, 700));
  if (m !== null && m.index < 600) return block.slice(m.index + m[0].length).trim();
  return block;
}

export function openingExcerpt(bodyStart: string, maxChars = 430): string | null {
  const junk =
    /^(Produced by|Credits|Title|Author|Release date|Language|More|Contents|CONTENTS|TRANSCRIBER|preferably|This eBook|Cover)/i;
  const blocks = bodyStart
    .split(/\n\s*\n/)
    .map((b) => b.replace(/\s*\n\s*/g, " ").trim())
    // Gutenberg emphasis markers (e.g. `_Mind is the Master power..._`) are
    // markup, not content — plain text must not show literal underscores.
    .map((b) => b.replace(/_/g, ""))
    .map(stripMergedCaption)
    .filter((b) => {
      if (b.length < 140) return false;
      if (junk.test(b)) return false;
      if (b.includes("Project Gutenberg eBook")) return false;
      if (b.includes("START OF THE PROJECT GUTENBERG")) return false;
      if (b.includes("END OF THE PROJECT GUTENBERG")) return false;
      if (/\[Illustration/i.test(b)) return false;
      if (/\bIllustrations\b/i.test(b)) return false;
      if (/\bCopyright\b/i.test(b)) return false;
      if (/\bCONTENTS\b.*/i.test(b)) return false;
      if (/\bIntroduction\s+\d+$/i.test(b)) return false;
      if (/\]\s*$/.test(b)) return false; // caption/bracket tail "--Thomas Jefferson.]"
      // Quote-opening blocks repeat as plate captions on later pages (the
      // same anecdote can print twice); real opening paragraphs almost never
      // start with a quotation mark. Epigraph-styled books fall through to
      // their real first prose block.
      if (/^[ \t]*["\u201c]/.test(b)) return false;
      // Gutenberg CONTENTS tables: rows like "I. Title .... 3 II. Title .... 9"
      if (/\b(?:X{0,3}(?:IX|IV|V?I{0,3}))\.\s+\S.*\s\d{1,3}\b/.test(b)) return false;
      // 4+ standalone small numbers = index/table-of-contents block
      const digits = b.match(/\b\d{1,3}\b/g) ?? [];
      if (digits.length >= 4) return false;
      // Aligned column layout (4+ runs of wide spacing) = table/index block
      // even when page numbers contain OCR typos like "34O".
      const gaps = b.match(/[ \t]{4,}/g) ?? [];
      if (gaps.length >= 4) return false;
      return true;
    });
  if (blocks.length === 0) return null;
  const text = `${blocks[0] ?? ""} ${blocks[1] ?? ""}`.trim();
  if (text.length < 100) return null;
  const cut = text.slice(0, maxChars);
  if (cut.length >= text.length) return cut;
  // Cap fell mid-sentence: end at the last completed sentence instead of a
  // truncated fragment like "... power of thought. I".
  const end = Math.max(cut.lastIndexOf(". "), cut.lastIndexOf("! "), cut.lastIndexOf("? "));
  if (end > 100) return cut.slice(0, end + 1);
  return cut;
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
  // Bartlett's source lines already carry their own quotation marks in some
  // editions; strip any edge quotes here so the card body wraps exactly once.
  const cleanQuote = (raw: string) =>
    raw
      .trim()
      .replace(/^["\u201c\u201d]+/, "")
      .trim()
      .replace(/["\u201c\u201d]+$/, "")
      .trim();
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
        bodyMd: `"${cleanQuote(q.quote)}"`.slice(0, 600),
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
