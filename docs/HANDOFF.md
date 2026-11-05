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

## PHASE 8 — Hardening [IN PROGRESS — Oct 2026]
SECURITY.md review, dependency audit, Sentry test crash, coverage API/auth/ranking/SRS/queue, startup/memory pass, a11y, error-state audit, saved export JSON/Markdown.

Done this pass: Prisma 7.10.1 (npm audit 0) + CI audit gate; backup exclusions +
network security config; POST_NOTIFICATIONS removed; explicit shortcut; R8 +
resource shrinking + QA smoke variant; a11y labels/headings; 8 Compose
on-device tests; Kover 12% floor (actual 15.48%); saved JSON/Markdown export
via FileProvider share sheet. Error-state audit: feed (offline banner + retry),
saved (loading/error/empty), me (loading/error/retry), login (inline error),
audio (failure text), export (Toast on failure).

Verified on the phone: lint/unit/Kover and 8/8 Compose tests; debug and QA
(R8) login + feed; QA save → Saved shows the just-saved card immediately;
6 rapid debug Save→Saved cycles and 2 QA cycles with no fatal crash; export
share sheet opened and the pulled JSON parsed; Sentry test crash captured
locally (event `81a88465e42f4087ad60d1397466cb30`) — owner dashboard/PII check
still pending. QA and release R8 builds succeed.

Crash found and fixed during hardening: Material3 1.4.0 throws
`visitAncestors called on an unattached node` from `ChildSemanticsNode` when
Save changes a `TextButton`'s semantics and the card is disposed by an
immediate tab switch. Card actions now use a plain clickable `CardAction`
(same button role, content description and state description). The Saved tab
also waits at most 2s for a queued upload and refreshes when `pendingSync`
reaches 0, so a slow upload cannot leave the just-saved card missing. Revisit
the workaround when a stable Material3 release fixes the Surface
child-semantics bug.

## PHASE 9 — Release, backup, runbook
**Backend deployment to Vercel (free tier) + Supabase happens here** — after hardening, before the signed release. User-facing effect: the phone no longer needs the PC (app talks to the public HTTPS API URL; dev build keeps the USB tunnel for development). Then: identity/icon/splash, versionCode+1, signing key backup x2 (never repo), signed release APK via Studio or Actions, prod env + migration after backup, smoke test, RUNBOOK.md (deploy/rollback/revoke/restore/re-run/lost-phone), monthly checklist.

## Deck change (Oct 2026) — quotes now come from the books SPARK recommends
Owner feedback: the old quote cards were the weakest content in the feed — Bartlett's 1905 anthology produced Victorian verse and providentialism ("Do you seek Alcides' equal?"), the hand-written seed quotes were the generic Instagram kind, and every card in a group shared one action line ("name one outside event you will stop arguing with" glued onto three different Marcus Aurelius quotes, one of them about the gods). Owner decision: **option A — quotes come from the books SPARK already recommends.**
- New pipeline: `prisma/data/growth-quotes.json` (curated: book slug, chapter, `match` prefix, per-quote action line) + `prisma/ingest-quotes.ts`, which re-fetches each book's Gutenberg text and pulls the **verbatim sentence** from it. A `match` that is not found verbatim is a hard error — a quote can never drift from the book it claims to come from. The quote text is grounded (`isGrounded = true`); the action line is written per quote, so the card is flagged `isAiGenerated = true` like the existing summary cards.
- 16 quotes live in the deck now, from 6 books: As a Man Thinketh (3) · 24 Hours (3) · Franklin (3) · Acres of Diamonds (2) · Science of Getting Rich (2) · Enchiridion of Epictetus (3). Each links to its Book row, so "Why this?" shows the book and chapter.
- Legacy quote cards (9 Bartlett + 4 hand-typed) are archived, not deleted: `npm run content:archive-legacy-quotes` (`-- --restore` to bring them back).
- Discovery helper: `npx tsx prisma/quote-candidates.ts [slug]` prints ranked candidate sentences per book (filters out archaic/religious register, narration and verse). Unit tests: `api/test/quotes.test.ts`.
- Deck now: 16 quote · 14 concept · 12 book_idea · 3 challenge = 45 cards, 0 audio.

## Deck change (Oct 2026) — audiobook cards removed
Owner feedback: the LibriVox chapter cards were pure boilerplate ("Section 3 of the public-domain LibriVox reading of *X* by *Y*"), every one of them shared the same action line, and they made up ~41% of the published deck. Removed two ways, both reversible: (1) 29 published audio cards were **archived** (`status: published → archived`, nothing deleted, saved state kept) via the new `npm run content:archive-audio` — add `--restore` to publish them again; (2) the ingest no longer produces them (`prisma/data/growth-config.json` → `audio.enabled: false`). Published deck is now 42 cards: 14 concept · 13 quote · 12 book_idea · 3 challenge. The Media3 player code stays in the app (dormant) so audio can come back cheaply once a card body carries a real excerpt of the chapter instead of a template sentence.

## PHASE 8.5 — UI polish pass [NOT STARTED — owner triggers it]
Owner's standing instruction (AGENTS.md rule 13): **the agent flags when polish is due; the owner says go.** Recommended trigger point: after Phase 8 hardening, before Phase 9 release — that is the last moment the whole app can be looked at as a whole (all 6 card types, offline states, audio, saved, me) without release pressure. Second acceptable trigger: a week of real daily use on the phone, so the owner can judge what actually annoys him.

Scope = **visual/interaction only, no new features**: icons, type scale, spacing rhythm, motion/haptics, states, labels. Everything here is deliberately parked until then.

### UI polish backlog (check items off as they land)
1. **Icons** — emoji render per-phone-font and look inconsistent.
   - [x] Bottom nav: done. `androidx.compose.material:material-icons-core` added (owner-approved), solid-when-selected / outline-when-not. This is *core*, not *extended* — verify each glyph exists before using it (core has ~49 icons; `ThumbDown`, `VolumeUp`, `Info` and most "outlined" variants are only in *extended*).
   - [x] Card action row (`feed/CardViews.kt`): done — five 48 dp icon targets, grey at rest, amber only when on. `thumb_down` shipped as two local vector drawables (Google path data from the material-design-icons repo, Apache 2.0) because *core* has no thumb-down.
2. **Type scale** — no `MaterialTheme.typography` overrides exist; screens hardcode `30.sp / 24.sp / 18.sp`. Define one scale in `ui/theme/Theme.kt` (display/headline/body/label) and use tokens everywhere.
3. **Spacing rhythm** — 20.dp/24.dp paddings scattered per screen; define a spacing scale (8/12/16/24/32) in the theme and apply it consistently.
4. **Haptics** — no feedback on Save / Like / Dislike / card snap (`LocalHapticFeedback`); a swipe-scroller feels dead without it.
5. **Motion** — pager cards change instantly; consider a subtle enter transition + `AnimatedContent` for the body when a card settles.
6. **"AI-written, unverified" label** — currently painted in `error` red (reads as a failure). Should be a neutral/amber "unverified" chip (warning, not error).
7. **Card header chip** — raw enum lowercase `[concept·thermo]` → pretty `CONCEPT · THERMO` with a filled chip (matches `docs/theme-preview.html` card/tag mock).
8. **Home "TODAY" block** — currently shows session limits ("● 20 cards per session"), but wireframe §0b wants weak topics / due counts. Needs server data (weak topics) — only if the API can supply it cheaply.
9. **Saved tab** — tapping a row should open the full card (wireframe §10); today the row is inert. Add a per-type dot colour.
10. **Audio overlay** — full-screen "Now playing" (wireframe §13): resume position, −10s/+30s, speed, sleep timer. Functionality parked from Phase 6.
11. **Loading / empty states** — plain text ("Loading saved cards…") → branded state blocks with an icon + one line of guidance (there is `ui/States.kt` to rework).
12. **Bottom nav** — done. Real Material icons (solid = selected, outline = not), amber icon+label with a 16%-alpha amber pill indicator, bar container `surfaceContainerLow` with no divider line.
13. **a11y** — Phase 8 landed content descriptions for card actions/steppers, heading semantics, and default 48.dp Material touch targets. Remaining 8.5 pass: full TalkBack order on cards, focus after swipe, and a screen-reader walkthrough with the owner.
14. **Me tab** — owner wants this screen to feel excellent, not just functional: a clear identity header, grouped settings (session limits, audio, offline cache), readable stats, and a calm sign-out area. Keep every existing action working. Visual/interaction only — parked for 8.5.
15. **Saved export buttons** - "Export Markdown" wraps to two lines in the half-width button. Use short labels (`Export .json` / `Export .md`) or a small overflow menu; keep both formats.
16. **Saved empty state** - export buttons are always composed but disabled while loading/empty (kept stable to avoid tab-transition churn). Decide in 8.5 whether to hide them on empty; keep them reachable whenever cards exist.
17. **Card action row** - `CardAction` is a plain clickable (workaround for the Material3 1.4.0 `ChildSemanticsNode` crash). Give it the final alignment/ripple/padding treatment in 8.5 and restore standard Material3 buttons once a stable release fixes the bug; keep the role + state descriptions.
