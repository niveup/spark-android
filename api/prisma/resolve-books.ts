// Phase 3b resolver: fills the null gutenbergId/librivoxId entries in
// prisma/data/growth-books.json by querying live public APIs and printing a
// resolution report for owner eyeballing. This script NEVER writes to the DB
// and never auto-selects silently ambiguous matches (AGENTS.md rule 3).
// Usage: npx tsx prisma/resolve-books.ts
import "dotenv/config";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import type { BookEntry } from "./growth-types";
import { gutendexSearch, librivoxTitleSearch, sleep } from "../src/lib/adapters";

const here = dirname(fileURLToPath(import.meta.url));
const jsonPath = resolve(here, "data/growth-books.json");
const data = JSON.parse(readFileSync(jsonPath, "utf-8")) as { entries: BookEntry[] };

const norm = (s: string): string =>
  s.toLowerCase().replace(/[^a-z0-9 ]+/g, " ").replace(/\s+/g, " ").trim();

const lastWord = (s: string): string => norm(s).split(" ").filter(Boolean).pop() ?? "";

function tokenOverlap(a: string, b: string): number {
  const A = new Set(norm(a).split(" "));
  const B = new Set(norm(b).split(" "));
  if (A.size === 0 || B.size === 0) return 0;
  let hit = 0;
  for (const x of A) if (B.has(x)) hit++;
  if (A.size < B.size) return hit / Math.max(A.size, B.size);
  return hit / Math.max(A.size, B.size);
}

async function resolveGutenberg(e: BookEntry): Promise<void> {
  if (e.gutenbergId != null) {
    console.log(`[GUT] pinned #${e.gutenbergId} — ok`);
    return;
  }
  const authors = await gutendexSearch(e.title, 8);
  const authorKey = lastWord(e.author.split(" ").slice(-1)[0] ?? "");
  const ranked = authors
    .filter((b) => b.language === "en")
    .map((b) => {
      const authorHit = norm(e.author).length > 0 && (norm(b.author).includes(lastWord(e.author)) || norm(b.author).includes(authorKey));
      return { b, score: (authorHit ? 2 : 0) + tokenOverlap(b.title, e.title) };
    })
    .sort((x, y) => y.score - x.score);
  console.log(`[GUT] "${e.title}" (${e.author}) candidates:`);
  if (ranked.length === 0) console.log("      NONE");
  for (const r of ranked.slice(0, 4)) {
    console.log(
      `      #${r.b.gutenbergId} ${r.score.toFixed(2)} "${r.b.title}" by ${r.b.author || "?"} (${r.b.language}) text=${r.b.textUrl ? "yes" : "no"}`,
    );
  }
  console.log(`      suggestion: gutenbergId: ${ranked[0] && ranked[0].score >= 2 ? ranked[0].b.gutenbergId : "null — eyeball"}`);
}

async function resolveLibrivox(e: BookEntry): Promise<void> {
  if (e.librivoxId != null) {
    const info = await librivoxTitleSearch(e.title, 8);
    const pinned = info.find((b) => b.librivoxId === String(e.librivoxId));
    console.log(
      `[LVX] pinned id ${e.librivoxId} — ${pinned ? `"${pinned.title}" (gutenberg ${pinned.gutenbergId}, ${pinned.numSections ?? "?"} sections)` : "NOT FOUND on re-check!"}`,
    );
    return;
  }
  const info = await librivoxTitleSearch(e.title, 8);
  console.log(`[LVX] "${e.title}" candidates:`);
  if (info.length === 0) console.log("      NONE");
  for (const b of info.slice(0, 5)) {
    const score = tokenOverlap(b.title, e.title);
    console.log(
      `      id ${b.librivoxId} ${score.toFixed(2)} "${b.title}" by ${b.author || "?"} (${b.numSections ?? "?"} ch, ${b.totalSeconds ?? "?"}s, gutenberg ${b.gutenbergId ?? "?"})`,
    );
  }
  await sleep(800);
}

console.log(`Resolving ${data.entries.length} growth-library entries against live APIs...\n`);
for (const e of data.entries) {
  console.log(`= ${e.title}`);
  try {
    await resolveGutenberg(e);
  } catch (err) {
    console.log(`[GUT] failed: ${String(err).slice(0, 160)}`);
  }
  try {
    await resolveLibrivox(e);
  } catch (err) {
    console.log(`[LVX] failed: ${String(err).slice(0, 160)}`);
  }
  console.log("");
}
console.log("Eyeball the tables above, pin ids into growth-books.json, then run ingest-growth.ts.");
