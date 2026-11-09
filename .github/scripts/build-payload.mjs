// Turns a repository_dispatch / workflow_dispatch payload into the exact file
// api/prisma/import-cards.ts expects: { "cards": [ … ] }.
//
// This step exists so the importer never has to know how the JSON arrived, and
// so a malformed or oversized payload fails BEFORE anything touches the DB.
// It never prints card content — only counts and sizes.
//
// Env in:  EVENT_NAME, DISPATCH_PAYLOAD (toJSON of client_payload), INPUT_CARDS,
//          INPUT_DRY_RUN, GITHUB_OUTPUT, RUNNER_TEMP
// Out:     $RUNNER_TEMP/cards.json  +  step outputs ready / dry_run / count
import { appendFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

// GitHub caps a dispatch payload at 64 KB; stay under it with room for the
// wrapper object. 50 cards is a generous ceiling for one run.
const MAX_BYTES = 60_000;
const MAX_CARDS = 50;

const tmp = process.env.RUNNER_TEMP || process.env.TEMP || ".";
const event = process.env.EVENT_NAME ?? "workflow_dispatch";

function fail(message) {
  console.error(`payload rejected: ${message}`);
  process.exit(1);
}

const raw =
  event === "repository_dispatch"
    ? (process.env.DISPATCH_PAYLOAD ?? "")
    : (process.env.INPUT_CARDS ?? "");

if (raw.trim() === "") fail(`empty payload (event: ${event})`);

const rawBytes = Buffer.byteLength(raw, "utf8");
if (rawBytes > MAX_BYTES) fail(`${rawBytes} bytes, limit ${MAX_BYTES} — split the batch`);

let parsed;
try {
  parsed = JSON.parse(raw);
} catch {
  fail("not valid JSON");
}

const cards = Array.isArray(parsed) ? parsed : parsed?.cards;
if (!Array.isArray(cards) || cards.length === 0) {
  fail('expected a non-empty array, or { "cards": [ … ] }');
}
if (cards.length > MAX_CARDS) fail(`${cards.length} cards, limit ${MAX_CARDS} per run`);

const notObject = cards.findIndex(
  (card) => card === null || typeof card !== "object" || Array.isArray(card),
);
if (notObject >= 0) fail(`card #${notObject + 1} is not a JSON object`);

// Dry run comes from the manual checkbox, or from the dispatch payload itself.
const dryRun =
  event === "workflow_dispatch"
    ? String(process.env.INPUT_DRY_RUN ?? "true") === "true"
    : parsed?.dryRun === true || String(parsed?.mode ?? "").toLowerCase() === "dry-run";

const file = join(tmp, "cards.json");
const body = JSON.stringify({ cards });
writeFileSync(file, body, "utf8");

const output = process.env.GITHUB_OUTPUT;
if (output) appendFileSync(output, `ready=true\ndry_run=${dryRun}\ncount=${cards.length}\n`);

console.log(
  `payload ok: ${cards.length} card(s), ${Buffer.byteLength(body, "utf8")} bytes, dry_run=${dryRun} → ${file}`,
);
