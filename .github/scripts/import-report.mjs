// Builds the Telegram report for one import run and sends it.
//
// Reads whatever the job produced (import log summary → summary.json, plus the
// accepted payload) and always sends exactly one report, split across messages
// when it exceeds Telegram's 4096-character limit.
//
// Nothing here can fail the build: missing secrets or a Telegram outage print
// the report to the log instead.
//
// Env: RUNNER_TEMP, TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID, RUN_URL, JOB_STATUS,
//      DRY_RUN, EVENT_NAME.  Flag: --dry (print, never send)
import { readFileSync } from "node:fs";
import { join } from "node:path";

const tmp = process.env.RUNNER_TEMP || process.env.TEMP || ".";
const printOnly = process.argv.includes("--dry");
const token = process.env.TELEGRAM_BOT_TOKEN ?? "";
const chatId = process.env.TELEGRAM_CHAT_ID ?? "";
const runUrl = process.env.RUN_URL ?? "";
const jobStatus = process.env.JOB_STATUS ?? "unknown";
const dryRunFlag = String(process.env.DRY_RUN ?? "") === "true";

// Telegram hard limit is 4096 chars; leave room for the part counter.
const CHUNK = 3800;

function readJson(name) {
  try {
    return JSON.parse(readFileSync(join(tmp, name), "utf8"));
  } catch {
    return null;
  }
}

const summary = readJson("summary.json");
const input = readJson("cards.json");

const esc = (value) =>
  String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

const failed = jobStatus !== "success" || summary === null || (summary.errors ?? 0) > 0;
const nothingStored =
  summary !== null && summary.stored === 0 && (summary.rejected > 0 || summary.duplicates > 0);
const icon = failed ? "❌" : nothingStored ? "⚠️" : "✅";
const mode = dryRunFlag || summary?.dryRun ? "DRY RUN" : summary?.mode === "draft" ? "DRAFT" : "PUBLISHED";

const lines = [];
lines.push(`<b>SPARK content import — ${mode}</b> ${icon}`);

if (summary !== null) {
  lines.push(
    `${summary.received} received · <b>${summary.stored} stored</b> · ` +
      `${summary.duplicates} duplicate(s) · ${summary.rejected} rejected · ${summary.errors} error(s)`,
  );
  if (nothingStored) lines.push("<i>Nothing new was stored in this run.</i>");
} else {
  lines.push(`Import did not produce a summary (job status: ${esc(jobStatus)}).`);
  if (input !== null) lines.push(`${input.cards.length} card(s) were received.`);
}
if (runUrl !== "") lines.push(`<a href="${esc(runUrl)}">Open the run</a>`);

if (summary !== null) {
  const stored = summary.details.filter((d) => d.status === "stored");
  const rejected = summary.details.filter((d) => d.status === "rejected" || d.status === "error");
  const duplicates = summary.details.filter((d) => d.status === "duplicate");

  if (stored.length > 0) {
    lines.push("", `<b>Stored (${stored.length})</b>`);
    for (const card of stored) {
      const tag = card.grounded ? "quote ✓ verified" : "AI-written, unverified";
      lines.push(`• [${esc(card.type)}] <b>${esc(card.title)}</b> — ${tag}`);
      if (card.body) lines.push(esc(card.body) + (card.body.length >= 180 ? "…" : ""));
    }
  }

  if (rejected.length > 0) {
    lines.push("", `<b>Rejected (${rejected.length})</b>`);
    for (const card of rejected) {
      lines.push(`• <b>${esc(card.title)}</b>: ${esc(String(card.reason ?? "").slice(0, 300))}`);
    }
  }

  if (duplicates.length > 0) {
    lines.push("", `<b>Already in the deck (${duplicates.length})</b>`);
    for (const card of duplicates) lines.push(`• ${esc(card.title)}`);
  }

  if (summary.rejected > 0 && summary.stored > 0) {
    lines.push("", "Rejected cards were not stored — fix the JSON and re-run.");
  }
}

// Pack the lines into Telegram-sized messages, keeping the header on the first.
function chunk(rows, limit) {
  const chunks = [];
  let current = "";
  for (const row of rows) {
    const next = current === "" ? row : `${current}\n${row}`;
    if (next.length > limit && current !== "") {
      chunks.push(current);
      current = row;
    } else {
      current = next;
    }
  }
  if (current !== "") chunks.push(current);
  return chunks;
}

const chunks = chunk(lines, CHUNK);

async function send(text) {
  const res = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      chat_id: chatId,
      text,
      parse_mode: "HTML",
      disable_web_page_preview: true,
    }),
  });
  if (!res.ok) {
    // Body is Telegram's error JSON; it never contains the token.
    console.error(`telegram send failed (${res.status}): ${(await res.text()).slice(0, 200)}`);
    return false;
  }
  return true;
}

if (token === "" || chatId === "") {
  console.warn("TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID not set — printing the report instead.");
}

let sent = 0;
for (let i = 0; i < chunks.length; i += 1) {
  const header = chunks.length > 1 ? `(${i + 1}/${chunks.length})\n` : "";
  const text = header + chunks[i];
  if (printOnly || token === "" || chatId === "") {
    console.log(`----- telegram message ${i + 1}/${chunks.length} (${text.length} chars) -----`);
    console.log(text);
    continue;
  }
  if (await send(text)) sent += 1;
}

console.log(`report: ${chunks.length} message(s), ${sent} sent to Telegram`);
