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

## Deck change (Nov 2026) — OpenStax physics cards archived
Owner asked for the three physics concept cards to go: **Carnot efficiency**,
**First law of thermodynamics**, **Second law: entropy** (OpenStax *University
Physics*, CC BY). They came from an earlier pipeline pass — no current ingest
script produces them — and thermodynamics sits outside the personal-growth
deck. Archived, not deleted: rows, content hashes and saved/liked state are
untouched, and the feed only serves `status = 'published'`.
- `node prisma/archive-physics.ts` from `api/` (`--restore` publishes them
  again). Scoped by `sourceType = 'openstax'` **and** `type = 'concept'`, so a
  future growth-topic OpenStax card is never swept up.
- Published deck is now **42 cards: 16 quote · 11 concept · 12 book_idea ·
  3 challenge**, 0 audio, 0 quiz (these numbers supersede the 42 in the
  October entries above). Remaining concept cards: 11 Wikipedia summaries
  (CC BY-SA, attribution in `sourceRef`).
- On a phone that already cached the deck, the three cards may linger in the
  offline library until the cache refreshes (Me → Clear offline cache).

## Phase 3c — AI content inbox [DONE Nov 2026 — importer verified]
Owner goal: any good AI fills a JSON file, the app shows the cards, with no
coding work in between.
- `api/prisma/import-cards.ts` reads `api/prisma/inbox/cards.json`, validates
  against `CardSchema`, dedupes by `contentHash`, inserts into Supabase, and
  publishes (or holds as drafts with `--draft`). Runs with plain `node`
  (Node 24 type stripping) so it does not need the tsx/esbuild runner.
- **Quote gate:** a `quote` card needs `gutenbergId` + `match`; the sentence is
  re-fetched from the real Gutenberg text and a miss is a hard rejection. This
  is the line that keeps fabricated attributions out: the AI may *claim* a
  quote, only the source may *certify* one. Non-quote cards are original
  AI text (`isAiGenerated=true`, `isGrounded=false`) and therefore render with
  the "AI-written, unverified" chip. Book rows are created on demand for
  verified quotes, so "Why this?" still names the book.
- Format + the copy-paste prompt for the owner's AI: `docs/AI_CONTENT_PROMPT.md`.
  Example inbox: `api/prisma/inbox/cards.example.json`.
- Verified: dry run on the example accepted 1 Epictetus quote (verified
  verbatim against Gutenberg #45109) + 2 original cards; a run with a fabricated
  Steve Jobs quote, a fake Epictetus line and a malformed card rejected all
  three with reasons; rolled back cleanly (deck stayed 42).
- Still missing for fully hands-off: the GitHub Action that runs the importer
  when the inbox file changes (needs `DATABASE_URL` as a repo secret), and a
  review screen for `--draft` mode.

## Phase 3c.1 — Cloud importer (GitHub Actions) [DONE Nov 2026 — Phase 1 only]
The importer can now run without the owner's PC: `.github/workflows/import-cards.yml`
(Node 24, `npm ci` in `api`) starts on `repository_dispatch`
(`spark-content-import`) or `workflow_dispatch`, validates the payload in
`.github/scripts/build-payload.mjs` (≤50 cards, ≤60 KB, `{cards:[…]}`), writes it
to `$RUNNER_TEMP/cards.json` (never the repo), runs
`node prisma/import-cards.ts --summary … [--dry-run]`, and reports to Telegram
via `.github/scripts/import-report.mjs` (split at Telegram's limit, with
per-card results). `concurrency: content-import`, `contents: read`,
no `NODE_ENV`. Importer gained three additive behaviours only: `--summary`
machine-readable output, `on conflict ("contentHash") do nothing`, per-card
error isolation (errors still exit non-zero) + savepoints in dry-run.
Verified locally: payload validation (empty/bad JSON/51 cards/70 KB all
rejected), publish run (3 stored), immediate re-run (3 duplicates), dry-run
rollback, and Telegram rendering/splitting. Still to do: re-run on a real
GitHub runner, and Phase 2 (Gmail → Apps Script trigger). Setup + troubleshooting:
`docs/CONTENT_AUTOMATION.md`.

## PHASE 8.5 — UI polish pass [IN PROGRESS — app-wide dark, offline, and motion pass]
The owner expanded the Home visual pass to cover every current screen plus online/offline behavior, transition quality, responsiveness, and battery-aware animation. The product brief requires dark mode by default, so this pass restores one coherent warm charcoal theme with forest surfaces and amber actions across sign-in, Home, Reels, Saved, Me, session completion, loading states, dialogs, and system bars.

Scope = **app UI, interaction, local cache presentation, and client-side animation/performance**. No backend, database schema, or authentication changes in this pass. Preserve the finite session, real content, pending sync queue, and existing release architecture.

### UI polish backlog (check items off as they land)
1. **Icons** — [x] Bottom nav uses real Material icons (filled selected / outline unselected); card actions use 48 dp targets and local thumb-down vectors because Material Icons Core does not contain that glyph.
2. **Type scale** — [x] Home and Saved/Me share editorial kicker, title, body, and meta styles in `ui/theme/Theme.kt`; the smallest meaningful Home/Reels/session labels were raised. At 200% font scale, Home reflows Today’s Pick into full-width text, removes its decorative cover, shows the full card title, stacks session facts, and keeps both actions visible; Reels card text scrolls while its actions remain reachable.
3. **Spacing rhythm** — [x] Home, Saved, and Me share a 20 dp page gutter through `SparkSpacing`. The spacing scale names 4/8/12/16/20 dp label, content, row, section/card, and page intervals; Home's pick/quote cards and Saved rows use the shared interior spacing tokens. Home, Saved (including the expanded card), Me, and Reels were reviewed on the phone.
4. **Haptics** — [ ] Reels actions/snaps, Saved filters/expansion/browse, and Me session-limit steppers/deck refresh use light `SegmentTick` feedback; session completion no longer vibrates on entry. Review remaining actions and intensity on the phone.
5. **Motion and frame cost** — [ ] Tabs fade through the charcoal page surface with a draw-only veil, avoiding a full-page alpha layer; each tab keeps saveable state. The selected tab surface fades in place beside the page transition, while icon/label colors switch directly to keep the animation light. Its progress is read in the draw phase so animation frames do not recompose the navigation items. The prior moving highlight left a visible trail and was replaced. Pager transforms read offsets inside `graphicsLayer`, avoiding full-screen recomposition for each pager frame. Me status cards ease height changes. The tab shell observes only whether a player surface exists; mini/full player surfaces collect the 500 ms playback-position updates locally, and the mini-player bar and full-player slider interpolate between those updates. Active audio therefore does not trigger app-shell recomposition. Final warm traces on `YHXOP7MFIJ6TOFIB`: repeated four-tab loops rendered 108/110 frames with 12/11 slow UI-thread frames (11.11%/10.00%), p95 69/65 ms, and 1 missed vsync in each loop. Individual changes showed 2–3 slow UI frames each; Saved→Me had 1 missed vsync. GPU work stayed around 2–5 ms on the earlier trace, pointing to UI-thread traversal rather than rendering load. A phone trace after moving the selected-surface animation to draw phase still showed 2–5 janky frames in each 1.3-second switch sample; results varied, so no measured improvement is claimed. Motion remains open for follow-up. The app does not force the display's maximum refresh rate.
6. **"AI-written, unverified" label** — [x] now a restrained amber warning chip instead of an error-red label.
7. **Card header chip** — [x] type and topic are readable title-case text in a filled chip; offline reading shows a compact Wi‑Fi-off retry button beside the card counter.
8. **Home session progress** — [x] uses the real `min(deck size, card limit)`, a visible progress bar with read count, and the configured minute limit. The progress is exposed to accessibility services.
9. **Saved tab** — [x] rows expand to show the full card, action line, source, and a per-type marker; non-book cards no longer repeat their title in both the heading and body. Empty, loading, error, and filtered-result counts no longer pretend that zero cards were loaded.
10. **Audio overlay** — [x] audio cards can open a full-screen player with seek, −10s/+30s, speed (0.75×–2×), sleep timer, retry, and stop controls. A mini-player dock appears above navigation on Home, Saved, and Me while a chapter is active; tapping the title opens the full player. The timer is owned by the MediaSession service and restored if the service restarts. The current deck has 0 published audio cards, so this dormant path still needs an audio card for phone walkthrough.
11. **Loading / empty / offline states** — [x] Home has separate loading/error/empty/done states; Saved retains a static skeleton and retryable errors; Me separates stats and deck states. Home, Saved, Reels, and Me show the compact Wi‑Fi-off button in their existing header/card area when health checks fail; it never pushes the page down with a full-width banner. Reels/Me identify cached deck content. When Android reports no validated connection, Refresh Deck skips the network call and restarts Room's cached deck directly with an explicit local-result message. The offline pager caps that cache to the configured session limit, even if Room holds cards from several sessions. Network errors use plain connection guidance instead of development-only PC/USB instructions; health-check results no longer delay entry to the main app. Connectivity follows Android's validated default-network callbacks, then checks API reachability when internet returns; the monitor unregisters on logout and does no polling.
12. **Bottom nav** — [x] dark elevated bar, real icons, neutral selected surface, readable labels, and a restrained in-place selection fade without a stale highlight trail; no decorative glow or forced high-refresh mode.
13. **Accessibility** — [ ] headings, 48 dp primary controls, progress semantics, and card state descriptions are present. The Home actions “Your shelf,” “Continue session,” and “Peek inside” now use explicit 48 dp minimum heights; the connected Compose size assertions pass. The session-complete actions expose checkbox/expanded state. Offline indicators have spoken descriptions and announce connection retry intent. At 200% font scale, Reels card text scrolls within its panel and the card action row and navigation remain present; the accessibility tree exposes Save, Like, Dislike, and Listen labels. Me wraps and scrolls at this scale. TalkBack order, swipe focus, and the full owner walkthrough remain.
14. **Me tab** — [x] the profile placeholder was removed in favor of a purposeful reading-history panel with cards read, saved, liked, sessions, and quizzes due; deck status, grouped session limits, sync status, offline library management, and sign-out follow. The activity panel animates its loading/content/error size changes and section titles are exposed as accessibility headings. The latest refinement gives the real reading total the only metric accent, softens zero-count metrics, reduces the deck refresh action to a secondary control while preserving full error/empty guidance, defers off-screen settings sections with a saveable `LazyColumn` state, and removes the debug-only crash-test panel from Me. Refresh requests seen cards, resets the pager/session only after a successful response, and shows an animated per-attempt checking/success/no-card/error result. A completed session now offers a direct “Read this deck again” action that uses the same online refill and offline cache restart path. Audio controls live in the shared mini-player/full-player surfaces rather than in Me settings.
15. **Saved export buttons** — [x] concise `Export .json` / `Export .md` labels keep both formats.
16. **Saved empty state** — [x] export controls only appear when saved cards are available.
17. **Card action row** — [x] the accessible 48 dp workaround uses restrained press scale/tint and light haptic; the particle burst and strong haptic are removed. On-device Save→Me switching reproduced the prior crash path without an Android fatal exception; the plain clickable workaround remains instead of the unstable Material3 `TextButton` semantics.
18. **Home dashboard** — [x] shows the real next card, bounded by the configured card limit; uses honest loading/error/empty/offline/session-done states, a grounded review quote, and no invented daily totals.
19. **Reels offline badge** — [x] moved below the card type/topic header; Retry remains reachable without covering card information.
20. **Me deck block** — [x] status area reserves 48 dp, long errors are capped at two lines, and Refresh stays in a stable position.
21. **Offline Saved latency and pending actions** — [x] Saved reads Room before waiting for sync/network. Superseded Saved searches and feed refreshes cancel their previous requests so stale responses cannot replace newer state. Queued Save/Unsave events are overlaid onto cached rows, and clearing cached cards preserves pending interactions. Me observes Room's cached-card count as a `Flow` only while that tab is visible, so async deck writes and cache clears update the status without polling or background observation.
22. **System appearance** — [x] app and system bars use the specified dark default, with light status/navigation icons regardless of the phone's light-mode setting.
23. **Real-phone review** — [x] Home, Reels, Saved, and Me were reviewed on `YHXOP7MFIJ6TOFIB` with a signed-in debug build. Offline mode showed the compact Wi‑Fi-off button in each screen's existing header/card area; the old full-width banner is gone. The latest airplane-mode phone check tapped Me → Refresh deck and displayed Offline deck restarted — 20 local cards ready to read again within 0.7 seconds from Room's 43-card cache. On the current installed build, tapping Me → Refresh deck displayed the refreshed result, then Reels opened at 1/20 with a card ready again. The direct completion-screen replay action is covered by the updated Compose smoke-test source; it was not run on the signed-in phone to preserve the owner's reading history. Home’s adaptive Today’s Pick layout and the Reels text-scroll/action-row layout were checked at 200% font scale. The phone was restored to 100% font scale and Home. The API tunnel and network are online again, and the app was left on Home at 100% font scale. TalkBack remains open under item 13.
24. **Tab state retention** — [x] a saveable state holder is keyed by tab; Saved filter, Home peek, Saved-card expansion, list scroll, and Reels pager position are preserved while switching tabs. The selected tab, content destination, and Saved search query also use saved state so they survive activity recreation.
25. **Sign-in screen and logout return** — [x] sign-in shares the dark theme and page gutter, keeps validation next to the passcode field, disables empty/busy submission, and scrolls with system-bar and keyboard insets. Logging out returns to the sign-in route after the existing local token clear; token and server authentication behavior is unchanged.
26. **Saved empty-state route** — [x] the truly empty library now offers a direct, haptic Browse Reels action; filtered empty results keep the separate Clear search action. Compose smoke coverage checks that the new action calls navigation.
27. **Home action hierarchy** — [x] removed the repeated “One minute, one idea” card because the Today’s pick card already opens Reels and has a clear Continue session action. The dashboard keeps one primary feed entry, with less content competing under the floating nav; the HTML preview matches.

### Current pass verification
`.\gradlew.bat --max-workers=2 -x adbReverseDev :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` passed on the final source: lint, all 56 JVM unit tests (0 failures), APK assembly, and Android test-source compilation. The connected Compose suite passed 9/9 on an earlier build; it was not rerun because this runner previously removed the signed-in debug app. Earlier phone review verified true-offline Refresh Deck against the 43-card Room cache and the Wi‑Fi-off icon in place of the banner. Online Me → Refresh deck returned its success message and Reels showed 1/20. The latest phone check at 200% font scale exercised Reels text scrolling and confirmed the labeled card actions plus bottom navigation remain in the accessibility tree. The new completion-screen replay button is compiled and covered in Compose smoke-test source but was not run on the signed-in phone to preserve reading history. The latest installed build showed the Saved card title once, exposed its full body/action/source on expansion, and the Save→Me switch completed without an Android fatal exception. The test save was undone; the local shelf is empty and the queued save/undo events reached “All changes synced.” All four nav destinations and selected-state semantics were present after switching. `git diff --check` passed (with Git LF-to-CRLF notices). The phone is online, font scale 1.0, airplane mode off, and USB API reverse restored. TalkBack and the remaining UI-thread frame pacing remain open.

**UI polish is not yet complete** — tab-transition frame pacing, TalkBack review, and physical haptic intensity remain open. *UI polish is due now.*
