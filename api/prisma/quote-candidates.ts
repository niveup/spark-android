// Dump ranked quote candidates from each pinned book's Gutenberg text so the
// owner (or the agent) can pick the ones worth a card. Read-only: it prints,
// it writes nothing to the database.
//
// Usage: npx tsx prisma/quote-candidates.ts [slug]
import "dotenv/config";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { gutenbergBody, sleep, withRetry } from "../src/lib/adapters";
import { extractQuoteCandidates } from "../src/lib/gutenbergQuotes";
import type { BookEntry } from "./growth-types";

const here = dirname(fileURLToPath(import.meta.url));
const booksPath = resolve(here, "data/growth-books.json");
const entries = (JSON.parse(readFileSync(booksPath, "utf-8")) as { entries: BookEntry[] }).entries;

const only = process.argv[2];
const limit = Number(process.env.QUOTE_LIMIT ?? 14);
const targets = only ? entries.filter((e) => e.slug === only) : entries;

// Editorial front matter (translator prefaces, editor introductions, glossaries)
// is not the book's own voice — skip those chapters when curating.
const EDITORIAL = /^(GLOSSARY|INTRODUCTION|PREFACE|ILLUSTRATIONS|POPULAR EDITION|THE AUTHOR|EDITOR|TRANSLATOR|NOTE|INDEX|CONTENTS)/i;

for (const e of targets) {
  if (e.gutenbergId === null) {
    console.log(`\n### ${e.title}: no Gutenberg id — skipped.`);
    continue;
  }
  const text = await withRetry(() => gutenbergBody(e.gutenbergId ?? 0, 260000));
  if (text === null) {
    console.log(`\n### ${e.title}: fetch failed.`);
    continue;
  }
  const candidates = extractQuoteCandidates(text, limit).filter(
    (c) => !EDITORIAL.test(c.chapter) && /^[A-Z"]/.test(c.text),
  );
  console.log(`\n### ${e.title} (${e.author}) — PG #${e.gutenbergId}`);
  console.log(`    ${candidates.length} candidates`);
  candidates.forEach((c, i) => {
    console.log(`  ${String(i + 1).padStart(2)}. [${c.score}] (${c.chapter || "no heading"}) ${c.text}`);
  });
  await sleep(700);
}
