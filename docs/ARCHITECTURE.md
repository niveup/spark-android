# docs/ARCHITECTURE.md — SPARK system design (Phase 1, no backend yet)

## Diagram

```
[Android app, Kotlin]
   |  HTTPS, Bearer token, /api/mobile/v1/*
   v
[API host, Phase 2: Next.js + Prisma] ---> [Supabase Postgres]
   ^  zod validation, Addison-only migrations      ^
   |  passcode in server env only                  |
[Worker / cron: content pipeline] -----------------+  (ingest → AI cards → DB)
```

No web app exists yet. When one is built, it uses the same Postgres + tables;
anything saved on one appears on the other. No cookie auth to coexist with.

## Token auth (single user)

1. User types passcode → `POST /auth/login`. Server compares (timing-safe) to
   env var, rate-limits 5/min/IP.
2. Server mints random 256-bit token, stores **SHA-256 hash** in `device_tokens`
   with label, expiry (90 days), `last_used_at`. Raw token returned once.
3. App keeps raw token in EncryptedSharedPreferences/Keystore, sends
   `Authorization: Bearer`. Never logs it. `allowBackup=false` so it never
   reaches cloud backup.
4. Logout/revoke deletes by hash. Lost phone ⇒ revoke by `deviceLabel`
   (Phase 9 runbook). Passcode rotation revokes all tokens.

## Backend layout (Phase 2 target)

- Host: any Node host (Vercel/Render/fly). Same repo or separate `spark-api`
  repo — decided in Phase 2; contract is host-independent.
- `Postgres`: one Supabase project for DEV, one for PROD (created in Phase 2).
  Prisma is the only SQL writer; all queries parameterized.
- Tables (AGENTS.md 1.6): `books`, `feed_items` (unique `content_hash`),
  `item_state`, `feed_sessions`, `device_tokens` (hash only), `ingest_jobs`.
- `feed_items.status`: `draft` (worker/review) → `published` (app-visible) →
  `archived`. App queries only `published`.
- Worker pre-generates cards in batches with daily AI cap; app never calls AI.

## App side (later phases)

- Phase 4: Retrofit client generated from API_CONTRACT, kotlinx.serialization
  models mirroring the contract types, timeouts + backoff, offline banner via
  `/health`.
- Phase 5: swipe batching via `POST /items/state` with `clientEventId` per event.
- Phase 7: Room cache of 50 items + event queue; replay is idempotent by design.
- Ranking v1 lives server-side (contract §GET /feed); app orders quiz/SRS only
  from `srsDueAt` read-only fields.
- Lost-phone revoke = `DELETE /auth/tokens/:id` (contract); no separate tool needed.
- Audiobook resume = `PUT /items/:id/progress` + `progressMs` on items (contract).

## Assumptions confirmed / proven wrong

- WRONG: "existing web app + backend". Corrected: no backend exists; Phases 1–2
  design-then-build it. Nothing to audit, nothing to avoid breaking.
- CONFIRMED: single user, sideloaded APK, finite feed, legal-sources-only,
  token-not-cookie auth, additive migrations.

## Risks kept from AGENTS.md Part 4

Hallucination → grounded/AI labels; copyright → license columns + review step;
cost → daily caps; secrets-in-APK → server-env-only; lost signing key → backup ×2
(Phase 9); content drift → draft/publish gate; habit loop → hard session limit.
