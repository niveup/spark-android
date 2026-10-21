// Shared typing for the growth-library data files (JSON is cast to these on
// load so the ingest script and resolver agree on the shape).
export type BookEntry = {
  slug: string;
  title: string;
  author: string;
  year: number;
  topicTags: string[];
  actionIdea: string;
  /** Pinned after resolve-books.ts eyeball verification; null = unresolved. */
  gutenbergId: number | null;
  librivoxId: string | null;
};

export type QuoteTarget = {
  /** Bartlett's header substring, e.g. "AURELIUS" matches "...AURELIUS ANTONINUS. ..." */
  match: string;
  /** Display name stored on cards. */
  authorName: string;
  topicTags: string[];
  actionLine: string;
};

export type ConceptTarget = {
  /** Wikipedia article title for the REST summary endpoint. */
  wiki: string;
  topicTags: string[];
  actionLine: string;
};

export type GrowthConfig = {
  quotes: QuoteTarget[];
  concepts: ConceptTarget[];
  /**
   * Audiobook (LibriVox) chapter cards. Disabled by default: the cards were
   * pure boilerplate ("Section 3 of the public-domain LibriVox reading of…")
   * and made up ~38% of the deck. Re-enable once the card body carries a real
   * excerpt from the chapter instead of a template sentence.
   */
  audio: { enabled: boolean };
};
