// Bartlett's Familiar Quotations (9th ed., 1905 — Project Gutenberg #27889)
// plain-text quote extractor. Public domain source; the attribution line is
// kept verbatim and stored in sourceRef on each quote card (AGENTS.md 1.7/1.9).
//
// Observed PG plaintext structure (verified live, Oct 2026):
//   GEOFFREY CHAUCER. 1328-1400.        <- author header (CAPS + dates)
//   (_From the text of Tyrwhitt._)      <- parenthesised notes (ignored)
//       WHANNE that April with his shoures sote    <- quote lines (indented)
//   _Canterbury Tales. Prologue. Line 1._          <- attribution (underscored)

export type BartlettQuote = {
  /** Raw author header text, e.g. "SENECA." */
  header: string;
  /** Quote text (verbatim, joined lines). */
  quote: string;
  /** Attribution text, e.g. "Canterbury Tales. Prologue. Line 9." */
  work: string;
};

// "MARCUS AURELIUS ANTONINUS. 121-180."
const HEADER_DATED = /^[A-Z][A-Z0-9 ,.;&:\u2019'’\u2014\u2013-]{3,64}\.\s*\d{3,4}(?:\s*-\s*\d{0,4})?\s*\.?$/;
// "SENECA."
const HEADER_PLAIN = /^[A-Z][A-Z0-9 ,.;&:\u2019'’\u2014\u2013-]{3,64}\.$/;
// "_Canterbury Tales. Prologue. Line 1._"
const ATTRIBUTION = /^_\s*(.+?)\s*_\s*$/;

function isQuoteLine(raw: string): boolean {
  return /^[ \t]{2,}\S/.test(raw);
}

/** Returns the matched target token if the header matches, else null. */
function targetHeader(trimmed: string, targets: string[]): string | null {
  if (!HEADER_DATED.test(trimmed) && !HEADER_PLAIN.test(trimmed)) return null;
  const upper = trimmed.toUpperCase();
  for (const target of targets) {
    const token = target.toUpperCase().trim();
    if (token.length >= 4 && upper.includes(token)) return token;
  }
  return null;
}

/**
 * Extract verbatim quotes per target author (substring match on the header,
 * e.g. "AURELIUS", "SENECA"). Only quotes followed by an attribution line are
 * accepted. Deterministic: the first capPerAuthor qualifying quotes per author.
 */
export function parseBartlettQuotes(
  text: string,
  targets: string[],
  capPerAuthor = 3,
): BartlettQuote[] {
  const lines = text.replace(/\r\n?/g, "\n").split("\n");
  const results: BartlettQuote[] = [];
  const seen = new Set<string>();
  const perToken = new Map<string, number>();

  let token: string | null = null;
  let author: string | null = null;
  let pending: string[] = [];

  const finalize = (): void => {
    pending = [];
  };

  const emit = (attribution: string): boolean => {
    if (token === null || author === null) return false;
    // Strip Bartlett's footnote page-ref markers, e.g. "[714-7]", "[749-1]".
    const quote = pending
      .map((l) => l.replace(/^[ \t]+/, "").replace(/[ \t]+$/, ""))
      .join("\n")
      .replace(/\[\s*\d+(?:\s*-\s*\d+)?\s*\]/g, "")
      .trim();
    pending = [];
    if (quote.length < 40 || quote.length > 340) return false;
    if (/[<>]+[A-Za-z]/.test(quote)) return false; // mis-OCR junk guard
    if (attribution.length === 0 || attribution.length > 200) return false;
    const key = `${author}|${quote}`;
    if (seen.has(key)) return false;
    seen.add(key);
    const count = perToken.get(token) ?? 0;
    if (count >= capPerAuthor) return false;
    perToken.set(token, count + 1);
    results.push({ header: author, quote, work: attribution });
    return true;
  };

  for (const raw of lines) {
    const trimmed = raw.trim();

    const headerToken = targetHeader(trimmed, targets);
    const looksHeader =
      headerToken !== null || HEADER_DATED.test(trimmed) || HEADER_PLAIN.test(trimmed);
    if (looksHeader) {
      // Zone switch: non-target headers must clear the previous author so
      // their quotes are never attributed to a target.
      finalize();
      token = headerToken;
      author =
        headerToken !== null
          ? trimmed.replace(/\s*\d{3,4}.*$/, "").replace(/\.$/, "").trim()
          : null;
      continue;
    }
    if (token === null) continue;

    const attrMatch = ATTRIBUTION.exec(trimmed);
    if (attrMatch !== null) {
      if (pending.length > 0) emit(attrMatch[1] ?? "");
      continue;
    }
    if (trimmed.length === 0) continue; // blank: quote block may continue (verse stanzas)
    if (isQuoteLine(raw)) {
      pending.push(raw);
      continue;
    }
    // any other non-indented line ends the pending block unattributed
    finalize();
  }
  finalize();
  return results;
}
