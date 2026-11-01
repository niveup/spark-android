// Shared text helpers: edge-quote trimming and Gutenberg boilerplate cutting.
// Pure logic only (no network, no DB) so CI-safe.
import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { stripEdgeQuotes, stripGutenbergBoilerplate } from "../src/lib/text";

describe("stripEdgeQuotes", () => {
  it("removes straight and curly quotes from both edges", () => {
    assert.equal(stripEdgeQuotes('"hello"'), "hello");
    assert.equal(stripEdgeQuotes("\u201chello\u201d"), "hello");
    assert.equal(stripEdgeQuotes('  "\u201chello\u201d"  '), "hello");
  });

  it("keeps quotes inside the sentence", () => {
    assert.equal(stripEdgeQuotes('He said "hi" today'), 'He said "hi" today');
  });

  it("returns plain text unchanged", () => {
    assert.equal(stripEdgeQuotes("plain text"), "plain text");
  });
});

describe("stripGutenbergBoilerplate", () => {
  const text = [
    "Header credits",
    "*** START OF THE PROJECT GUTENBERG EBOOK TEST ***",
    "Real body text.",
    "*** END OF THE PROJECT GUTENBERG EBOOK TEST ***",
    "License footer",
  ].join("\n");

  it("keeps only the body between the markers", () => {
    assert.equal(stripGutenbergBoilerplate(text).trim(), "Real body text.");
  });

  it("fails open when no marker is present", () => {
    assert.equal(stripGutenbergBoilerplate("no markers here"), "no markers here");
  });

  it("cuts the footer even when the start marker is missing", () => {
    const onlyEnd = "body\n*** END OF THE PROJECT GUTENBERG EBOOK ***\nfooter";
    assert.equal(stripGutenbergBoilerplate(onlyEnd).trim(), "body");
  });
});
