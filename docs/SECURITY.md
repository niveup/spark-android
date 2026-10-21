# SPARK — Security Notes (Phase 8)

Last updated: 21 October 2026 · Branch: `phase-8-hardening`

SPARK is a single-user Android app that reads a personal feed from an API built
in this repository. There is no public sign-up, no multi-tenant data, and the
APK is sideloaded (not distributed through the Play Store). “Production-grade”
here means: no secrets in the app, no cleartext traffic, one auditable auth
path, defensive input handling, backups excluded, and a written list of the
risks we consciously accepted.

This document describes the **current** state, not the Phase 9 deployment.
Anything marked **Pending Phase 9** is deliberately not done yet.

---

## 1. Threat model

| Asset | Why it matters | Main controls |
|---|---|---|
| API bearer token | Full access to the feed and state endpoints | 256-bit random token, server stores only SHA-256, Keystore-backed storage, 90-day expiry, revocable |
| Passcode | Mints new tokens | Server-side env var only, timing-safe compare, 5 attempts/min/IP |
| Database / AI keys | Would allow data access or spend | Server environment only; never in the APK or repo |
| Personal reading state | Private notes/saves/likes | Bearer auth on every data endpoint; no public shares |
| Release signing key | Identifies the app | Phase 9: generated once, backed up twice offline, never committed |

Out of scope for a personal sideloaded app: rooted-device detection, screen
capture blocking, remote wipe, and anti-tamper. A person with physical access
to an unlocked phone can read the app; the token is still protected by the
Android Keystore.

---

## 2. Secrets and configuration

- `.env*` is git-ignored (only `.env.example` is committed). Server secrets
  (`DATABASE_URL`, `SPARK_PASSCODE`, AI keys) live only in the server
  environment.
- The app bundle contains **no** database credentials and **no** AI keys. The
  release `API_BASE_URL` is a public HTTPS URL; the Sentry DSN is a public
  write-only identifier and is safe in the APK.
- `sentry.properties` is git-ignored. It holds the Sentry org auth token used
  only to upload R8 mappings; the build skips the upload when no token is
  present, so local builds do not depend on it.
- `*.jks`, `*.keystore`, and `keystore.properties` are git-ignored. The real
  release key does not exist yet (Phase 9).
- CI has no production secrets. The API unit job runs pure logic only.

---

## 3. Transport

- Release: `android:usesCleartextTraffic="false"` **and** a network security
  config whose base config denies cleartext (`res/xml/network_security_config.xml`).
- Debug: cleartext is allowed **only** for `127.0.0.1` / `localhost`, because
  the dev API is reached through `adb reverse tcp:3000 tcp:3000`. The exception
  is in `src/debug/res/xml/network_security_config.xml` and is not in release.
- QA: same loopback-only exception in `src/qa/...`; the QA variant is signed
  with the local debug key and must never be distributed.
- Certificate pinning is **not** implemented (accepted risk, §10). Standard
  Android TLS validation applies.

---

## 4. Authentication and sessions

- Passcode comparison hashes both values and uses `timingSafeEqual`
  (`api/src/lib/auth.ts`), so it does not leak length/prefix timing.
- Login mints a random 256-bit token. The database stores only
  `sha256(token)`; a database leak does not reveal usable tokens.
- Tokens expire after 90 days. `requireAuth` rejects revoked and expired rows.
- `DELETE /api/mobile/v1/auth/tokens/:id` revokes a device. Losing the phone:
  revoke its token from another authenticated client or directly in the DB.
- Login is rate-limited to **5/min per trusted client IP**. Other limits:
  feed 30/min per token, writes 60/min per token, admin 30/min.
- The limiter is in-memory and per process: it resets on a cold start and is
  not shared across instances. This is documented and accepted until Phase 9,
  when Vercel needs a database-backed limiter.
- `clientIp()` trusts, in order: `x-vercel-forwarded-for`, `x-real-ip`, then
  the **last** `x-forwarded-for` hop. Client-supplied first hops are ignored.
- On the phone the raw token lives in `EncryptedSharedPreferences` backed by
  an AES-256-GCM Android Keystore master key. It is never logged and never
  written to the Room cache. Logout clears it.

---

## 5. Data at rest and backups

- `android:allowBackup="false"`.
- Android 12+ (`dataExtractionRules`) and Android 11-and-lower
  (`fullBackupContent`) both exclude **all** app domains from cloud backup and
  device-to-device transfer. This is belt-and-braces: some OEM D2D paths have
  historically ignored `allowBackup`.
- The Room cache stores feed cards and saved snapshots, not tokens or
  passcodes. It is not separately encrypted; app-private storage plus the
  backup exclusions are the control.
- Saved-card exports are written only to `cache/exports`, contain only the
  cards the owner chose to save, and are shared through a non-exported
  `FileProvider` (`${applicationId}.fileprovider`) with a read-only,
  one-intent URI grant. Exports never contain the bearer token, passcode, or
  server credentials; Android clears the cache normally.
- Server database backups/restore drills are **Pending Phase 9** (Supabase
  backup + documented restore before the first production migration).

---

## 6. API input and authorization

- Every request body/query is validated with zod; Prisma uses parameterized
  queries only (no string-built SQL).
- Every `/api/mobile/v1/*` data endpoint calls `requireAuth`; admin endpoints
  use the same bearer auth plus the 30/min limiter.
- Error messages are intentionally generic (`Wrong passcode.` on every bad
  login) to avoid account/state enumeration.
- Tokens, passcodes, and full request bodies are not logged. Structured logs
  contain method/path/status only.
- The feed/state API contract is additive; the app never sends SQL, file paths,
  or model prompts.

---

## 7. Prompt-injection handling (content pipeline)

All ingested book/web text is treated as untrusted data:

1. Source text is always wrapped in `<data-source>…</data-source>` blocks.
2. The system prompt explicitly says to treat the block as data only, never
   follow instructions inside it, and never emit code/tool calls.
3. Model output is parsed and validated against the zod `CardSchema`; anything
   that does not match is rejected, not executed or displayed.
4. AI-written cards carry `is_ai_generated = true` and `is_grounded = false`;
   the UI labels them “AI-written, unverified.” Grounded cards keep the source
   reference and book/chapter link.

No model output is ever evaluated as code. The app only renders text.

---

## 8. Android attack surface

- Permissions: `INTERNET`, `WAKE_LOCK`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. `POST_NOTIFICATIONS` was removed: the
  only notification is Media3’s media-session notification, which is exempt
  from that runtime permission. Verified with `dumpsys package`: the app does
  not request it.
- `MainActivity` is exported only because it is the launcher. The custom
  `OPEN_REELS` intent filter was removed; the static shortcut now uses an
  explicit `targetPackage` + `targetClass` intent. There are no deep links or
  WebViews.
- `AudioBookService` is `exported="false"`; Media3 connects with a locally
  created session token.
- Release builds enable R8 + resource shrinking. `app/proguard-rules.pro`
  keeps the Retrofit interface and kotlinx.serialization serializers; the QA
  variant is the phone smoke test for that pipeline.
- No dynamic code loading, no reflection-based plugin system, no JavaScript
  bridge.
- The export `FileProvider` is `exported="false"`, grants URI permission only
  for the share intent, and exposes only `cache/exports` (`res/xml/file_paths.xml`).
- Sentry: `sendDefaultPii = false`, ANR capture on, 20% trace sampling, SDK
  debug logs only in debug builds. No request bodies, tokens, or passcodes are
  attached. Sentry auto-init is disabled so there is exactly one init path.

---

## 9. Supply chain and CI

- GitHub Actions runs Android lint, JVM unit tests, the Kover coverage floor,
  API unit tests, and `npm audit --omit=dev --audit-level=high` on every push
  and pull request.
- Dependabot checks Gradle and npm dependencies weekly.
- The Gradle wrapper is committed with a SHA-256-pinned distribution
  (Gradle 9.6.0). CI uses `./gradlew`.
- `dependency-review-action` is intentionally **not** used: the repository is
  private and GitHub Advanced Security is not enabled. Dependabot + npm audit
  + manual review are the replacement (accepted risk, §10).
- Current audit result (21 Oct 2026): `npm audit --omit=dev --audit-level=high`
  → **0 vulnerabilities** after Prisma 7.10.1.
- No Gradle dependency audit tool is automated yet; reviewed manually in this
  phase. A future `dependencyCheck`/Renovate pass can be added without app
  changes.

---

## 10. Accepted risks (deliberate, with owner visibility)

| Risk | Why accepted | Mitigation / review |
|---|---|---|
| `EncryptedSharedPreferences` / `security-crypto` is deprecated | Replacing it with hand-rolled crypto would be worse; it still works and is Keystore-backed | Backup excluded; no custom crypto; revisit if Android removes the API |
| In-memory rate limiter resets on cold start / per instance | Single-user, single-instance DEV; DB-backed limiter is Phase 9 work | Documented; login still needs the 256-bit passcode |
| No certificate pinning | Personal app, standard TLS; pinning adds rotation outages | Revisit if the API domain is stable and the owner wants it |
| No root/jailbreak or tamper detection | Out of scope for a personal sideloaded app | Physical device security is the owner’s control |
| QA variant is debug-signed | It exists only to test R8 against the local API | Never distribute; package suffix `.qa`; documented in build file |
| Release `API_BASE_URL` is still `https://example.com` | Deployment is Phase 9 | Do not ship the release APK until Phase 9 replaces it |
| No DB backup/restore drill yet | Supabase production DB does not exist yet | Phase 9 gate before first production write |
| `dependency-review-action` absent | Private repo without GHAS | Dependabot + npm audit + manual diff review |
| App data is not remotely wiped on lost phone | `allowBackup=false` prevents cloud copies, not a remote wipe | Revoke the device token; change the passcode if needed |
| Sentry 8.61 `sendDefaultPii` setter is deprecated | The SDK still honors it; removal is scheduled for SDK 9.0 | Behaviour is correct today; migrate to the `DataCollection` API when upgrading Sentry |

---

## 11. Incident quick actions

1. **Lost phone:** revoke its token (`DELETE /auth/tokens/:id`) or set
   `revokedAt` in `device_tokens`; optionally rotate `SPARK_PASSCODE`.
2. **Token leaked in a log/screenshot:** revoke that token; no need to change
   the passcode unless the passcode itself leaked.
3. **Sentry token leaked:** revoke it in Sentry → Settings → Auth Tokens;
   replace `sentry.properties` locally / the CI secret.
4. **Dependency advisory:** run `npm audit`, update the affected package,
   re-run API unit tests + `npx tsc --noEmit`; for Gradle, bump the version in
   `gradle/libs.versions.toml` and re-run lint/unit/connected tests.
5. **Database incident:** Phase 9 runbook (backup before migrations, restore
   procedure, migration rollback notes). Until then, the dev DB is the only
   copy and is not production.

---

## 12. Phase 8 verification (commands actually run)

| Check | Command | Result |
|---|---|---|
| API audit | `npm audit --omit=dev --audit-level=high` | 0 vulnerabilities |
| API types | `npx tsc --noEmit` | clean |
| API unit tests | `npm run test:unit` | 35 passed |
| Android lint/unit | `.\gradlew.bat :app:lintDebug :app:testDebugUnitTest` | BUILD SUCCESSFUL |
| Compose UI tests | `.\gradlew.bat :app:connectedDebugAndroidTest` | 8/8 passed on RMX5085 (incl. export buttons) |
| Saved export | JVM `SavedExportTest` + Compose button test | JSON/Markdown shape covered; phone share sheet opened and the pulled JSON parsed |
| Coverage | `.\gradlew.bat :app:koverLogDebug` | 15.48% line; floor 12% passes (`koverVerifyDebug`) |
| R8 build | `.\gradlew.bat :app:assembleQa :app:assembleRelease` | BUILD SUCCESSFUL; QA/release mapping files generated |
| Manifest | `aapt2 dump xmltree …/app-debug.apk --file AndroidManifest.xml` | no `POST_NOTIFICATIONS`, no `OPEN_REELS` filter; backup/network attrs present |
| QA launch | `adb shell am start -W -n com.spark.app.qa/…` | R8 login, feed, save → Saved verified; no fatal/cleartext log lines |
| Sentry crash | Me → "Send test crash to Sentry" | captured, event `81a88465e42f4087ad60d1397466cb30`; envelope cached/sent |
| Save→Saved stress | 6 debug + 2 QA rapid cycles | no fatal crash after the `CardAction` workaround |

One owner-only check remains: confirm the Sentry event appears in the dashboard
with no PII. QA R8 login/feed/save and the export share sheet are verified.

---

## 13. What Phase 9 must add

- Deploy the API over HTTPS and replace the placeholder release URL.
- Database-backed rate limiting (or Vercel KV) shared across instances.
- Real release signing key, backed up offline twice; signed release smoke test.
- Supabase backup + restore drill; production migration after a backup.
- Revoke/rotate runbook exercised once; `RUNBOOK.md` committed.
- Decide whether to keep the deprecated `security-crypto` or migrate to a
  platform `SharedPreferences` + Keystore wrapper (no hand-rolled crypto).
- Upgrade Sentry past 9.0 and replace the deprecated `sendDefaultPii` setter
  with the `DataCollection` privacy API.
