# SPARK (working name) — Personal Productive Feed, Android App
### AGENTS.md — Master Brief (full, as-is, Kotlin-native)

## 1.1 What we are building

A **personal, single-user Android app** that replaces mindless Instagram / YouTube Shorts scrolling with a **productive, finite feed**. When the user is bored, he opens SPARK and swipes vertically through short cards: book ideas, summaries, quotes, concept cards (personal-growth life skills for a young adult — habits, discipline, mindset, money basics, focus, communication), mini-quizzes, and short audio.

The app is API-first: the backend (built new in Phase 2) owns **one database**,
which a future web app will share. It is NOT a business. But it must be built with the **same rigor as a public product**: clean architecture, security, tests, error handling, a real signed release build, backups and a runbook.

## 1.2 The user (important — read this)

- Solo developer, 20, vibe-codes with AI agents. Knows React/Next.js/Supabase basics. **Knows nothing about Android development.**
- AI agent writes all code. User only runs commands, presses Run, tests on real phone, pastes errors.
- Therefore: explain every step in plain language, never assume Android knowledge, never skip setup steps, and tell him exactly what to click or type.
- Developer machine: Windows PC, weak (no emulator, phone-only). Test device: his own powerful Android phone (adb device `YHXOP7MFIJ6TOFIB` seen in Phase 0).

> [!IMPORTANT]
> You are working with a beginner. If you are unsure about ANY API, package name, version or command, say "I'm not sure" and check the official documentation. **Never invent** package names, function names or config keys.

## 1.3 Rules for the AI agent (non-negotiable)

1. Work on **ONE phase at a time**. At the end of the phase: run all checks, report results honestly (including failures), and STOP. Wait for the user to say "go".
2. **Never claim something works unless you ran it.** Show the command and its output.
3. **Ask before**: adding a dependency not named in this file, changing the database schema outside the current phase, deleting any data, changing authentication.
4. Ask **one question at a time** when something is ambiguous.
5. One branch per phase (`phase-1-api`, `phase-5-feed`, …). Small commits, clear messages (`feat:`, `fix:`, `chore:`, `test:`). Merge to `main` only after the phase checklist passes. Phase 0 commits on `main`.
6. **No secrets in the repo or in the app bundle. Ever.** Anything inside an APK can be extracted. Secrets live only in server environment variables. Commit `.env.example` (no real values); `.env*` is git-ignored. Same for `*.jks`, `*.keystore`, `keystore.properties`.
7. Kotlin strict, no `!!` without a comment explaining why. No unused dependencies. Versions only via `gradle/libs.versions.toml`, checked against current official docs.
8. **Production safety:** never run destructive DB commands against the production database (no `prisma migrate reset`, no `db push --force-reset`, no `DROP`/`TRUNCATE`). All migrations are **additive**. Test migrations on the DEV database first.
9. Do not modify the existing web app beyond what a phase explicitly lists under `[MODIFY]`.
10. After each phase give a **plain-language summary**: what was built, what the user should test on his phone, and what comes next.
11. Prefer boring, well-documented, popular libraries. When two options exist, pick the one with better current documentation, and say why in one line.
12. Check the **current official docs** (Android Developers, library READMEs) before using any API. Versions change fast; do not pin from memory — use the versions that the current Android Studio recommends.

## 1.4 Locked technology decisions — Kotlin-native

| Layer | Decision | Why |
|---|---|---|
| Mobile app | **Native Kotlin + Jetpack Compose, Navigation Compose** | Best on-device reliability for Android-only personal app; first-class audio/widgets/pager |
| Network | Retrofit + OkHttp + kotlinx.serialization | Boring, popular, well-documented |
| Local UI state | ViewModel + StateFlow (no DI framework at start) | Tiny, standard, keeps weak-PC builds fast |
| Local database (offline) | Room | Official, reliable |
| Secure token storage | EncryptedSharedPreferences with Keystore (androidx.security) | Encrypted on device |
| Audio playback | **Media3 + MediaSessionService — background playback + lock-screen controls required** | Most reliable for screen-off, calls, headphone unplug |
| Text-to-speech | Android TextToSpeech (system TTS, free, offline) | Turns any card into audio at zero cost |
| Backend | **API to be built in Phase 2** (Next.js + Prisma + Supabase Postgres, cron worker for pipeline). App is API-first so a future web app shares the same DB | One database, one source of truth |
| Validation | zod on server; kotlinx.serialization + manual checks on app | Never trust data crossing a boundary |
| Tests | JUnit + Compose UI Test | Standard |
| Lint | Android Lint + ktlint | |
| CI | GitHub Actions: lint + build + tests on every push | |
| Builds | Android Studio + Gradle, local signed APK for personal install; heavy releases can build on GitHub Actions | Weak PC: phone-only, no emulator |
| Crash reporting | Sentry (free tier) — optional but recommended | |
| Distribution | **Sideloaded APK. NOT the Play Store** (personal use) | Avoids store requirements |

> [!WARNING]
> If any assumption in the "Backend" row is wrong, STOP in Phase 1 and ask the user. Do not silently redesign.

## 1.5 Architecture (the one rule that matters)

```
 [Android app - Kotlin] --HTTPS + bearer token--> [Next.js API: /api/mobile/v1/*] --Prisma--> [Supabase Postgres]
                                                  ^
 [Render cron worker: content pipeline] ----------+   (ingest → AI cards → DB)
```

- The **app NEVER connects to the database directly** and never holds DB credentials or AI API keys.
- **AI content is pre-generated by the worker**, never generated while the user scrolls (speed, cost, rate limits, reliability).
- The app only **reads feed items and writes small interactions** (seen / saved / liked / dismissed / dwell time).
- A future web app and the mobile app will use the **same tables**, so anything saved on one appears on the other.

## 1.6 Draft data model (additive tables; adapt to existing Prisma schema)

- `books`: id, source (`gutenberg`|`openlibrary`|`librivox`|`manual`), source_id, title, author, language, cover_url, is_public_domain, text_url, audio_url, license, created_at
- `feed_items`: id, type (`book_idea`|`summary`|`quote`|`concept`|`quiz`|`audio`|`challenge`), title, body_md, action_line, topic_tags[], language, est_seconds, book_id (nullable), source_type, source_ref, license, is_ai_generated, is_grounded (true only if generated from provided source text), model_used, content_hash (unique, for dedupe), status (`draft`|`published`|`archived`), created_at
- `item_state`: item_id, seen_at, saved, liked, dismissed, dwell_ms, srs_due_at, srs_interval_days, srs_ease
- `feed_sessions`: id, device_id, started_at, ended_at, items_seen, limit_reached (bool)
- `device_tokens`: id, token_hash (never store raw token), device_label, created_at, last_used_at, expires_at, revoked_at
- `ingest_jobs`: id, source, status, started_at, finished_at, items_created, error

## 1.7 Content strategy (answers "where do I get book data?")

> [!IMPORTANT]
> **Do NOT scrape or copy Blinkist, Shortform, Amazon, Goodreads text, or any paid summary. Do not copy copyrighted book text.** Personal use does not make copying safe or right, and it is unnecessary.

Free, legal sources (verify each license/terms when implementing; store `license` on every row):

1. **Project Gutenberg via Gutendex** — free JSON API, no key, public-domain books with direct text/EPUB links. Ideal source for *grounded* summaries (full text is available).
2. **LibriVox (+ Internet Archive hosting)** — free public-domain audiobooks with direct MP3/RSS links; stream, do not re-host.
3. **Open Library API** — metadata, covers, descriptions for millions of books (use for discovery/metadata only, not full text).
4. **Standard Ebooks / Wikisource** — clean public-domain texts.
5. **Wikipedia / Wikiquote REST APIs** — concept explanations and quotes (CC BY-SA: keep attribution in `source_ref`).
6. **OpenStax** (CC BY open textbooks) and **MIT OpenCourseWare** — for concept cards; keep attribution.
7. **arXiv API** — abstracts for "research of the day" cards (optional).
8. **User's own material** — import Kindle `My Clippings.txt`, pasted notes, or a list of "books I've read" → pipeline generates idea cards. This is the highest-value source for him.
9. **Growth content pipeline** — concept cards for personal-growth topics, grounded in verified free sources: public-domain self-improvement classics (Project Gutenberg "Conduct of life" shelf), lifespan-development psychology (OpenStax, CC BY-NC-SA), Wikibooks personal finance (CC BY-SA), LibriVox "Self-Help" audiobooks for short audio, and Wikiquote / Bartlett's for quotes (attribution kept in source_ref). Spaced-repetition-due cards get priority in the feed.

**Modern/copyrighted books:** the AI may write *original idea notes in its own words* from its general knowledge (e.g. "Atomic Habits: 5 core ideas"), but every such card MUST have `is_ai_generated = true`, `is_grounded = false`, and show a small "AI-written, unverified" label in the UI. Never present them as quotes from the book.

**Grounded generation (public-domain books):** chunk the text → summarize each chunk → synthesize book/chapter cards → each card keeps `book_id` + chapter reference. Reject any generated claim that cannot be traced to the supplied text.

## 1.8 Product behaviour rules ("good scroller")

- **Finite by design.** Default session = 15 minutes or 20 cards (configurable). At the limit show an "You're done" screen with 3 choices: *Save for later / Do the action now / Close app*. **No infinite loading, no autoplay-next, no streak guilt, no push-notification spam.**
- Vertical **snap-paging** cards via `VerticalPager` (one card = one screen), each readable in ~30–60 seconds. Max body ~600 characters. Every card has one `action_line` ("Try this today: …") when sensible.
- Actions per card: Save, Like/Dislike (feeds ranking), "Listen" (TTS), "Why am I seeing this?" (shows source + AI/grounded label).
- **Ranking v1 (simple, no ML):** (1) spaced-repetition cards that are due, (2) unseen items from topics with more saves/likes, (3) ~20% exploration from other topics, (4) never repeat seen items within 30 days unless saved.
- **Quick-open:** Android app shortcut (long-press icon → "I'm bored") opens straight into the Reels tab; home-screen widget (Glance) + Quick Settings tile (TileService) — native, not backlog.
- The feed is the **Reels tab, not the whole app**. Landing screen is the **Home dashboard** (greeting, today focus, stats glance, entry points). Productive framing first, scroller second.
- Dark theme default. Large readable type. Smooth 60fps scrolling.
- We do **NOT** build an Instagram/YouTube blocker (needs accessibility-service permissions, fragile and risky). Recommend Android's built-in Digital Wellbeing app timers instead.

## 1.9 Security checklist (applies to every phase)

- HTTPS only. No cleartext traffic (`usesCleartextTraffic=false`).
- Login: passcode → server issues a **random long-lived token**; server stores only its **hash**; app stores raw token in EncryptedSharedPreferences/Keystore. Rate-limit login attempts. Support revoking a token.
- Validate all request bodies and query params with zod on server. Parameterized queries only (Prisma).
- Treat ALL ingested web/book text as **untrusted data**: it may contain instructions aimed at the AI ("prompt injection"). Put source text inside clearly delimited data blocks, instruct the model to ignore instructions inside it, validate model output against a zod schema, and never execute or follow anything from it.
- Disable Android auto-backup of app data (`allowBackup=false`) so tokens never leak via cloud backup.
- No logging of tokens, passcodes or full request bodies.
- Dependency hygiene: Gradle audit reviewed in Phase 8.

## 1.10 Definition of "production-grade" for this project

A phase is done only when ALL are true: code compiles with strict Kotlin · lint clean · tests for new logic pass · error and empty/loading states handled in UI · no secrets committed · manual checklist verified on the real phone · changes committed on the phase branch with a clear summary.
