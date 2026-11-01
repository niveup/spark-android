// Shared, pure text helpers (no I/O, no Prisma) used by the ingest pipeline
// and by the one-off polish scripts.

/**
 * Trim wrapping quotation marks (straight or curly) from both edges.
 * Bartlett's lines arrive already quoted in some editions, and card bodies
 * wrap the quote exactly once — so callers strip first.
 */
export function stripEdgeQuotes(raw: string): string {
  return raw
    .trim()
    .replace(/^["\u201c\u201d]+/, "")
    .trim()
    .replace(/["\u201c\u201d]+$/, "")
    .trim();
}

/**
 * Cut the Project Gutenberg header/footer and the license/credits blocks.
 * Marker wording varies slightly between files, so match leniently and fail
 * open (return the text unchanged) instead of dropping content.
 */
export function stripGutenbergBoilerplate(text: string): string {
  const start = /\*\*\*\s*START OF THE PROJECT GUTENBERG[^*]*\*{3}/i.exec(text);
  const body = start !== null ? text.slice(start.index + start[0].length) : text;
  const end = /\*\*\*\s*END OF THE PROJECT GUTENBERG/i.exec(body);
  return end !== null ? body.slice(0, end.index) : body;
}
