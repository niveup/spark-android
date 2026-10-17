# docs/HANDOFF.md — Phase plan (Kotlin-native, full brief in AGENTS.md)

Send one at a time: "Read AGENTS.md and docs/HANDOFF.md. Execute Phase N only. Stop when done and show verification."

## PHASE 0 — Setup + Hello SPARK on real phone [CURRENT]
[NEW] Git+Studio+SDK+USB debugging verified. Repo `niveup/spark-android` private, `main`.
Scaffold: AGP 9.4.0, Gradle 9.6.0, JDK 17, compileSdk 37, target 36, minSdk 26,
Compose BOM 2026.08.00, `com.spark.app`, `allowBackup=false`, no cleartext.
Hello SPARK screen (dark Material3) + 1 unit test + CI.
Do NOT: any feature beyond Hello.
Verify: app opens on phone showing Hello SPARK; `./gradlew lint test` or Studio Sync+Run passes; CI green; tool versions listed.

## PHASE 0b — Quick-win: Home dashboard + Reels tab (no backend)
Home landing (greeting, today focus, entry points, stats glance) + bottom tabs
[Home|Reels|Saved|Me]. Reels = VerticalPager 20 hardcoded cards + TTS + You're-done
screen. Saved lists saved titles; Me shows about + session stats. Proves structure early.

## PHASE 1 — Mobile API contract (no backend exists yet) [CURRENT]
Write docs/API_CONTRACT.md (/api/mobile/v1/: auth/login, logout, feed cursor, items/:id/state,
items/state batch, items/saved, sessions, sessions/:id, health) + docs/ARCHITECTURE.md (token auth
hashed, single-user passcode, DEV/PROD Supabase plan for Phase 2). No code/DB changes.
User approves contract before Phase 2.

## PHASE 2 — Database + API backend (built new)
New DEV Supabase project + minimal Next.js API host, additive Prisma migration (books, feed_items,
item_state, feed_sessions, device_tokens, ingest_jobs), prod backup before any prod migration,
implement /api/mobile/v1 with zod + rate limit, seed 30 items DEV only, tests auth/pagination/state.

## PHASE 3 — Content pipeline (worker)
Adapters Gutendex/OpenLibrary/LibriVox/Wiki/manual, zod card schema (title<=80, body<=600), injection guards, grounded flags, dedupe content_hash, daily AI cap, ingest_jobs log, draft preview/publish page. Growth bootstrap deck. No copyrighted scraping.

## PHASE 3b — Real growth-content library loads [CURRENT]
Pin verified Gutenberg/LibriVox IDs in api/prisma/data/growth-books.json; resolve script cross-checks LibriVox `url_text_source`.
New adapters: LibriVox by title + `librivoxSections` (extended=1 MP3s+playtime), Bartlett's quote extraction (PG #27889), wikiSummary→concept cards.
Ingest script creates Book rows + data-driven audio/quote/concept/book_idea cards (no hardcoded deck, no big file storage — text fetched transiently, audio streamed by URL). Publish after owner check.


## PHASE 4 — App skeleton: nav, auth, API client
Navigation Compose, Retrofit+OkHttp client from contract, login passcode->EncryptedSharedPreferences/Keystore, auto-login/logout, BuildConfig dev/prod URLs, error boundary, offline banner, loading/empty states, tests.

## PHASE 5a — Feed UI
VerticalPager snap cards per type, Save/Like/Listen/Why-this, 60fps on phone, shortcut I'm-bored.

## PHASE 5b — Interactions + ranking + SRS + Saved/Stats
Batched state sync with retry, session limit + You're-done + settings, server ranking v1 unit-tested, quiz SRS, Saved search, Stats.

## PHASE 3b — Real growth-content library loads [DONE Oct 2026]
Pinned Gutenberg/LibriVox pairs in `api/prisma/data/growth-books.json` (cross-verified via LibriVox `url_text_source`). `ingest-growth.ts` loads Book rows + data-driven cards: LibriVox chapter audio (stream URLs only), Bartlett's 1905 quotes (PG #27889 parser), Wikipedia concept summaries (CC BY-SA), Gutenberg book-open "Why read" cards. 71 published items live on the dev DB. Owner review gated by `ingest:review` → `ingest:publish`.

## PHASE 6 — Audio [DONE — verified on phone Oct 2026]
Media3 1.11.1 background + lock controls implemented: `AudioBookService` (MediaSessionService), inline audiobook card player (play/pause/seek/progress), TTS hidden on real-audio cards. Phone-verified: stream plays, position ticks, media session PLAYING with buffer while app backgrounded 30s+, playback survives screen off, ColorOS shade + lock screen show SPARK media card with full controls. Feed exploration is shuffled (fresh items surface; verified audio card lands top-of-deck). Resume + speed + sleep timer remain (future polish, wireframes §13).

## PHASE 7 — Offline Room + sync [DONE — verified on phone Oct 2026]
Room 2.8.5 (`spark-offline.db`, schema exported to `app/schemas/`): cached deck (max 50 fresh cards, saved always kept) + saved snapshots + pending interaction events. Offline Reels shows the cached deck with an "Offline library" chip; Saved tab mirrors cached saved cards with local substring search; every queued event persists across app restarts and replays on startup (idempotent via clientEventId). Me tab shows cache size + a confirm-dialog "Clear offline cache". Phone-verified: force-stopped the app → removed USB tunnel → relaunch served the cached deck (1/13, chip visible) offline; tapping Save offline survived force-stop — 9 queued changes uploaded and "All changes synced ✓" as soon as the tunnel returned; clear-cache emptied the store. Server-wins content / last-write-wins state per the plan.

## PHASE 8 — Hardening
SECURITY.md review, dependency audit, Sentry test crash, coverage API/auth/ranking/SRS/queue, startup/memory pass, a11y, error-state audit, saved export JSON/Markdown.

## PHASE 9 — Release, backup, runbook
**Backend deployment to Vercel (free tier) + Supabase happens here** — after hardening, before the signed release. User-facing effect: the phone no longer needs the PC (app talks to the public HTTPS API URL; dev build keeps the USB tunnel for development). Then: identity/icon/splash, versionCode+1, signing key backup x2 (never repo), signed release APK via Studio or Actions, prod env + migration after backup, smoke test, RUNBOOK.md (deploy/rollback/revoke/restore/re-run/lost-phone), monthly checklist.
