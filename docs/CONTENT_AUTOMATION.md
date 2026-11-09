# Content automation — Phase 1 (GitHub Actions importer)

Phase 1 moves the import off your laptop: a cloud runner takes a SPARK cards
payload, runs the existing importer against Supabase, and reports to Telegram.

```
payload (JSON)  →  GitHub Actions  →  api/prisma/import-cards.ts  →  Supabase  →  app
```

**Not in this phase:** the Gmail/Apps Script trigger and any daily schedule.
Today the workflow starts from a `repository_dispatch` event or from the
GitHub UI. Nothing runs on its own yet.

---

## 1. Secrets

Add these under **Settings → Secrets and variables → Actions → New repository
secret**. They are available only to the workflow, never to the app bundle.

| Secret | What it is | Where to get it |
|---|---|---|
| `DATABASE_URL` | Postgres connection string for the SPARK Supabase project | Supabase → Project Settings → Database → connection string (the pooled `aws-0-…pooler.supabase.com:5432` one) |
| `TELEGRAM_BOT_TOKEN` | Token of the bot that sends the report | Telegram → `@BotFather` → `/newbot` → copy the token |
| `TELEGRAM_CHAT_ID` | Your personal chat id | Message your bot once, then open `https://api.telegram.org/bot<TOKEN>/getUpdates` and read `result[0].message.chat.id` (a number, may start with `-` for groups) |

Never commit these. The repository already ignores `.env*`; this workflow reads
secrets straight into environment variables and does not print them.

## 2. Run it from the GitHub UI (no laptop needed)

**Actions → Content import → Run workflow**

- **cards_json** — paste the exact SPARK payload the importer expects:
  `{ "cards": [ … ] }` (same format as `api/prisma/inbox/cards.example.json`).
- **dry_run** — leave **checked** for a validation run. It executes the whole
  pipeline inside a transaction and rolls back, so you see what *would* happen
  without publishing anything.

The run shows up in the Actions tab and the result arrives in Telegram.

## 3. Run it from a button/script later (`repository_dispatch`)

The workflow listens for event type `spark-content-import`. The
`client_payload` **is** the cards JSON, optionally with a mode flag:

```json
{
  "mode": "dry-run",
  "cards": [
    { "type": "concept", "title": "…", "body": "…", "action": "Try this today: …", "topics": ["focus"] }
  ]
}
```

Trigger it with a token that has access to this repository (this is what the
future Apps Script will do):

```bash
curl -X POST \
  -H "Authorization: Bearer $GITHUB_PAT" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/<owner>/<repo>/dispatches \
  -d '{"event_type":"spark-content-import","client_payload":{"cards":[ … ]}}'
```

`mode: "dry-run"` (or `"dryRun": true`) triggers the rollback path; omit both to
publish.

## 4. What the Telegram report contains

- mode (PUBLISHED / DRY RUN / DRAFT) and an icon: ✅ success, ⚠️ nothing new
  stored, ❌ the run failed
- counts: received, stored, duplicates, rejected, errors
- every stored card with its type, title and a body excerpt, plus whether it was
  a **verified quote** or **AI-written, unverified**
- every rejected card **with the reason** (invented quote, missing Gutenberg id,
  schema violation)
- duplicates as titles only
- a link to the run

Telegram caps a message at 4096 characters, so long reports are split and each
part is prefixed `(1/2)`, `(2/2)`.

If the Telegram secrets are missing the report is printed to the job log
instead — the import still succeeds.

## 5. Safety rules built into the workflow

| Rule | How |
|---|---|
| No secrets in code | only `${{ secrets.* }}` at run time |
| No generated JSON committed | payload is written to `$RUNNER_TEMP/cards.json`, outside the repo |
| Payload validated before use | `build-payload.mjs`: valid JSON, `cards` array, ≤ 50 cards, ≤ 60 KB, every card an object |
| One import at a time | `concurrency: content-import`, no cancellation |
| No `NODE_ENV=production` | the importer refuses to run then; the workflow never sets it |
| Minimal permissions | `permissions: contents: read`, checkout without persisted credentials |
| Failures visible | rejected cards are reported; a DB error exits non-zero |

## 6. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `import-cards refuses to run in production` | something set `NODE_ENV=production`; remove it |
| `ERR_UNKNOWN_FILE_EXTENSION ".ts"` | Node older than 22.18; the workflow pins 24 — check the step wasn't edited |
| `payload rejected: … bytes, limit 60000` | too many cards; split into two dispatches (`concurrency` serialises them) |
| `payload rejected: expected a non-empty array` | the payload was a bare object; it must be `{ "cards": [...] }` or an array |
| `DATABASE_URL repository secret is missing` | secret not set, or set as a variable (Variables ≠ Secrets) |
| `telegram send failed (400)` | wrong `TELEGRAM_CHAT_ID`, or you never messaged the bot first |
| `telegram send failed (403)` | the bot was blocked or removed from the chat |
| Cards stored but not in the app | the feed reads `status='published'`; check the card is not a draft, and that the phone reloaded the deck |
| A card says `DUPLICATE` but you archived it earlier | dedupe looks at every status, not just published: an archived row still owns its `contentHash`. Restore that row instead of re-importing it |
| Manual run rejected for input size | `workflow_dispatch` inputs have their own GitHub size cap; send large batches through `repository_dispatch`, or split them |
| `quote … not verbatim in Gutenberg #N` | the AI paraphrased; fix `match` to the first ~10 real words, or drop the card |
| Gutenberg `fetch failed` / 429 | temporary; re-run the job. The importer retries 3× per book |

## 7. Where the pieces live

| File | Role |
|---|---|
| `.github/workflows/import-cards.yml` | the workflow |
| `.github/scripts/build-payload.mjs` | validates the payload, writes `$RUNNER_TEMP/cards.json` |
| `.github/scripts/import-report.mjs` | builds and sends the Telegram report |
| `api/prisma/import-cards.ts` | the importer (unchanged content contract) |
| `docs/AI_CONTENT_PROMPT.md` | the prompt/format your AI fills in |

## 8. Next phase (not built yet)

Gmail → Apps Script → `repository_dispatch`. That closes the loop so an email
from Gemini publishes cards with no human step. It needs one more secret, a
fine-grained GitHub token (Contents: read and write on this repo only), stored
in Apps Script's Script Properties — never in this repository.
