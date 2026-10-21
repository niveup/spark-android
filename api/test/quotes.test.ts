// Phase 8b unit tests: the quote deck's "verbatim" guarantee.
//
// A quote card must be a VERBATIM sentence from the book it claims to come
// from, so the matching helpers are pure and testable without the network.
// Usage: npm run test:unit
import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import {
  extractQuoteCandidates,
  findQuoteSentence,
  normalizeWhitespace,
  scoreSentence,
} from "../src/lib/gutenbergQuotes";

describe("normalizeWhitespace", () => {
  it("collapses the line breaks Gutenberg wraps sentences with", () => {
    const raw = "Resolve to perform what you\nought; perform without fail\nwhat you resolve.";
    assert.equal(normalizeWhitespace(raw), "Resolve to perform what you ought; perform without fail what you resolve.");
  });
});

describe("findQuoteSentence", () => {
  const book = normalizeWhitespace(
    "CHAPTER ONE. Resolve to perform what you ought; perform without fail what you resolve. 5. " +
      "Let all your things have their places; let each part of your business have its time. 4. " +
      "The whole appeared to me as written with a good deal of decent plainness.",
  );

  it("returns the full sentence that starts with the match", () => {
    const s = findQuoteSentence(book, "Resolve to perform what you ought");
    assert.ok(s !== null);
    assert.equal(s, "Resolve to perform what you ought; perform without fail what you resolve.");
  });

  it("stops before a numbered list marker instead of swallowing it", () => {
    const s = findQuoteSentence(book, "Let all your things have their places");
    assert.ok(s !== null);
    assert.equal(s, "Let all your things have their places; let each part of your business have its time.");
    assert.ok(!s.endsWith(" 4."));
  });

  it("returns null when the sentence is not in the book", () => {
    assert.equal(findQuoteSentence(book, "You have power over your mind"), null);
    assert.equal(findQuoteSentence(book, ""), null);
  });

  it("does not cut a sentence at an abbreviation", () => {
    const text = normalizeWhitespace("He asked Mr. Smith for the bill and left without paying it.");
    const s = findQuoteSentence(text, "He asked Mr. Smith");
    assert.ok(s !== null);
    assert.equal(s, "He asked Mr. Smith for the bill and left without paying it.");
  });
});

describe("scoreSentence", () => {
  it("rejects archaic and religious register", () => {
    assert.equal(scoreSentence("Thou shalt not enter here, for the gods are watching all who pass."), 0);
    assert.equal(scoreSentence("The ways of the gods are full of providence for those who wait."), 0);
  });

  it("rejects fragments and over-long sentences", () => {
    assert.equal(scoreSentence("Short."), 0);
    assert.equal(scoreSentence("x".repeat(300)), 0);
  });

  it("prefers sentences that address the reader", () => {
    const actionable = scoreSentence("If you would protect your body, guard your mind.");
    const passive = scoreSentence("The report was written by a committee of the society last year.");
    assert.ok(actionable > passive, `expected actionable > passive (${actionable} vs ${passive})`);
  });
});

describe("extractQuoteCandidates", () => {
  const text = [
    "CHAPTER ONE.",
    "If you would protect your body, guard your mind.",
    "Thou wilt find rest from vain fancies when the day is done.",
    "The train left the station early and the platform emptied slowly afterward.",
    "CHAPTER TWO.",
    "You cannot waste the next hour; it is kept for you.",
  ].join("\n\n");

  it("keeps only sentences that pass the filters", () => {
    const candidates = extractQuoteCandidates(text, 10);
    const bodies = candidates.map((c) => c.text);
    assert.ok(bodies.includes("If you would protect your body, guard your mind."));
    assert.ok(bodies.includes("You cannot waste the next hour; it is kept for you."));
    assert.ok(!bodies.some((b) => b.includes("Thou wilt find rest")), "archaic quote must be dropped");
    assert.ok(!bodies.some((b) => b.includes("platform emptied")), "narrative sentence must be dropped");
  });

  it("records the chapter the sentence came from", () => {
    const candidates = extractQuoteCandidates(text, 10);
    const second = candidates.find((c) => c.text.startsWith("You cannot waste"));
    assert.ok(second !== undefined);
    assert.equal(second.chapter, "CHAPTER TWO");
  });
});
