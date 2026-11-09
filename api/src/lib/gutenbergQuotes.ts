// Quote candidates from the real text of a public-domain book.
//
// Option A (owner decision, Oct 2026): SPARK's quote cards come from the books
// it already recommends — not from a general 1900s anthology. A quote must be
// a VERBATIM slice of the Gutenberg text, so every quote card can point at a
// specific book + chapter and can be re-verified against the source later.
//
// This module is a pure function (no network): give it the raw book text, get
// back ranked candidate sentences with a chapter anchor.

export type QuoteCandidate = {
  /** Verbatim sentence from the source text. */
  text: string;
  /** Nearest preceding heading, e.g. "CHAPTER III" or "PART TWO". */
  chapter: string;
  /** Higher = more likely to be a useful standalone quote. */
  score: number;
};

/** Archaic/religious register — reads as "old book", not as advice. */
const ARCHAIC =
  /\b(thee|thy|thine|thou|ye|hath|doth|shalt|wilt|wherefore|whilst|henceforth|unto|amidst|amongst|peradventure|verily)\b/i;
const RELIGIOUS =
  /\b(providence|deity|immortal|heavens|scripture|holy spirit|almighty|godhead|gods? will|alms)\b/i;

/** Signals that a sentence states something you can act on. */
const SECOND_PERSON = /\b(you|your|yours|yourself)\b/i;
const CONTRAST = /\bbut\b|\bnot\b[^.]{0,60}\bbut\b/i;
const ABSOLUTE = /\b(always|never|must|should|every|none|no one|only when|as soon as|if you)\b/i;
const CAUSAL = /\b(the secret|the reason|the cause|the key|the difference|the law|the rule|the whole|depends on|comes from)\b/i;
const CONCRETE = /\b(hour|minute|morning|evening|day|week|year|money|pay|write|read|walk|build|begin|start|finish|choose|decide|practice)\b/i;

/** Bookmarks / front matter that are not book content. */
const JUNK_LINE =
  /^(Produced by|Credits|Title|Author|Release date|Language|More|Contents|CONTENTS|TRANSCRIBER|This eBook|Cover)/i;

/** Lines that name a part of the book — they anchor the quotes that follow. */
const CHAPTER_LINE =
  /^(CHAPTER|CHAP\.|PART|BOOK|SECTION|PREFACE|INTRODUCTION|EPILOGUE|PROLOGUE|CONCLUSION)\b/i;

const ABBREVIATIONS = /\b(Mr|Mrs|Ms|Dr|St|vs|etc|e\.g|i\.e|vol|No|fig|pp|inc|jr|sr)\.$/i;

function isHeading(line: string): boolean {
  const t = line.trim();
  if (t.length === 0 || t.length > 70) return false;
  if (CHAPTER_LINE.test(t)) return true;
  // Short ALL-CAPS lines act as headings in most of these books.
  return /^[A-Z0-9][A-Z0-9 .,'’\-()&]{2,69}$/.test(t) && /[A-Z]{4,}/.test(t);
}

/** Split a paragraph into sentences, keeping common abbreviations intact. */
export function splitSentences(paragraph: string): string[] {
  const raw = paragraph.split(/(?<=[.!?])\s+/);
  const out: string[] = [];
  for (let i = 0; i < raw.length; i++) {
    let s = raw[i] ?? "";
    // Re-join a fragment that ended on an abbreviation.
    while (ABBREVIATIONS.test(s.trimEnd()) && i + 1 < raw.length) {
      i += 1;
      s = `${s} ${raw[i] ?? ""}`;
    }
    const t = s.trim();
    if (t.length > 0) out.push(t);
  }
  return out;
}

/**
 * Extract ranked quote candidates from a raw book text.
 * @param text raw Gutenberg plain text
 * @param limit max candidates to return
 */
export function extractQuoteCandidates(text: string, limit = 25): QuoteCandidate[] {
  const body = stripGutenbergBoilerplate(text);
  const paragraphs = body
    .split(/\n\s*\n/)
    .map((p) => p.replace(/\s*\n\s*/g, " ").trim())
    .filter((p) => p.length > 0);

  const found: QuoteCandidate[] = [];
  let chapter = "";
  for (const paragraph of paragraphs) {
    const flat = paragraph.trim();
    // A heading anchors the quotes under it. Checked BEFORE the junk filter:
    // "CHAPTER ONE." is a chapter name, not front matter to throw away.
    if (flat.length > 0 && flat.length <= 70 && isHeading(flat)) {
      chapter = flat.replace(/[.:\s]+$/, "");
      continue;
    }
    if (JUNK_LINE.test(flat) && flat.length < 70) continue;
    if (/^_.*_$/.test(flat)) continue; // Gutenberg italic markup on its own line
    if (flat.includes("Project Gutenberg")) continue;

    for (const sentence of splitSentences(flat)) {
      const s = sentence.trim();
      const score = scoreSentence(s);
      if (score <= 0) continue;
      found.push({ text: s, chapter, score });
    }
  }

  // Same sentence can appear twice in these books (chapter openers repeat).
  const seen = new Set<string>();
  const unique = found.filter((c) => {
    const key = c.text.toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  unique.sort((a, b) => b.score - a.score);
  return unique.slice(0, limit);
}

/** Collapse all whitespace so a sentence split across Gutenberg line breaks matches. */
export function normalizeWhitespace(text: string): string {
  return text.replace(/\s+/g, " ").trim();
}

/**
 * Find the full sentence that starts with `match` inside already-normalized
 * text. Returns null when the match is absent or the sentence is malformed —
 * the ingest treats that as a hard error so a quote can never drift from its
 * source.
 */
export function findQuoteSentence(normalizedText: string, match: string): string | null {
  const needle = normalizeWhitespace(match);
  if (needle.length === 0) return null;
  const idx = normalizedText.indexOf(needle);
  if (idx < 0) return null;

  const window = normalizedText.slice(idx, idx + 420);
  for (let i = 0; i < window.length; i++) {
    const ch = window[i];
    if (ch !== "." && ch !== "!" && ch !== "?") continue;
    const before = window.slice(0, i);
    // Skip abbreviations ("Mr.", "e.g.") so we do not cut a sentence short.
    const lastToken = `${before.split(/\s+/).pop() ?? ""}.`;
    if (ch === "." && ABBREVIATIONS.test(lastToken)) continue;
    const next = window[i + 1];
    if (next !== undefined && next !== " ") continue;
    const after = window.slice(i + 2, i + 3);
    // A digit after the stop means a numbered list item ("...what you resolve. 5."),
    // not a continuation of the sentence.
    if (after !== undefined && after !== "" && !/[A-Z“‘"0-9]/.test(after)) continue;
    const sentence = window.slice(0, i + 1).trim();
    if (sentence.length < 40 || sentence.length > 300) return null;
    return sentence;
  }
  return null;
}

/** Cut the Project Gutenberg header/footer and the license/credits blocks. */
export function stripGutenbergBoilerplate(text: string): string {
  const start = /\*\*\*\s*START OF THE PROJECT GUTENBERG[^*]*\*\*\*/i.exec(text);
  let body = text;
  if (start !== null && start.index >= 0) {
    body = text.slice(start.index + start[0].length);
  }
  const end = /\*\*\*\s*END OF THE PROJECT GUTENBERG/i.exec(body);
  if (end !== null && end.index >= 0) body = body.slice(0, end.index);
  return body;
}

/** 0 = reject, higher = better standalone quote. */
export function scoreSentence(sentence: string): number {
  const s = sentence.trim();
  const len = s.length;
  if (len < 45 || len > 220) return 0;
  if (s.includes("  ")) return 0;
  // Exactly one terminal sentence, no mid-sentence semicolon lists or quotes.
  const terminal = (s.match(/[.!?](?=["')\]]?$)/g) ?? []).length;
  if (terminal !== 1) return 0;
  if (/[,;:]/.test(s.slice(0, s.length - 1))) {
    // allow at most two commas for readability
    if ((s.slice(0, s.length - 1).match(/,/g) ?? []).length > 2) return 0;
  }
  if (/\b[A-Z]{2,}\b/.test(s) && !/\b(I|OK|CEO|US|IT|GOD|ONE|ALL|BE|DO|IT)\b/.test(s)) {
    // shouting all-caps runs = headings or tables
  }
  if (ARCHAIC.test(s)) return 0;
  if (RELIGIOUS.test(s)) return 0;
  if (/\b(he|she|it|they)\b/i.test(s) && !SECOND_PERSON.test(s) && !CAUSAL.test(s)) return 0;
  // A quote has to say something you can act on. Pure narration ("the train
  // left the station early...") is not a card.
  if (!SECOND_PERSON.test(s) && !CONTRAST.test(s) && !ABSOLUTE.test(s) && !CAUSAL.test(s)) {
    return 0;
  }

  let score = 1;
  if (SECOND_PERSON.test(s)) score += 3;
  if (CONTRAST.test(s)) score += 2;
  if (ABSOLUTE.test(s)) score += 2;
  if (CAUSAL.test(s)) score += 2;
  if (CONCRETE.test(s)) score += 1;
  // Prefer a clean, quotable shape: starts with a capital, ends with a period.
  if (/^[A-Z"]/.test(s)) score += 1;
  if (len >= 70 && len <= 180) score += 1;
  return score;
}
