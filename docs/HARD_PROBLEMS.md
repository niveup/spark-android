# HARD PROBLEMS JOURNAL — SPARK

> **For the future AI agent (and future me):**
> When we hit a problem that takes more than ~30 minutes or more than one wrong guess,
> we write it down here — **append a new entry at the bottom, in the same template.**
> Never rewrite old entries; edit only to add a "follow-up" line when something new
> is learned about the same problem.

---

## Entry template (copy this for every new entry)

```
### [date] — [short title]            (phase where it happened)
**Symptom:** what the user saw / what failed, in one or two lines.
**Proof (how we found the truth):** the one command or observation that pinned it down.
**Root cause:** plain-language why.
**Fix:** what we actually did (with exact file/command).
**One-line learning:** the rule to keep.
**If it happens again:** the fastest path to the same fix.
STATUS: verified / assumed / open
```

---

## The rules that kept being true tonight (2026-10-17)

1. **"It works from the PC" proves nothing about the phone.** Test from the *far end*
   (the device), not the middle. The killer diagnostic tonight was pinging the PC *from
   the phone* via `adb shell ping` — 100% packet loss ended all theories in seconds.
2. **Compare timestamps before trusting an app's behaviour.** When behaviour doesn't
   match a code change, check whether the *device actually has a build newer than the
   change* — `adb shell dumpsys package <pkg> | findstr lastUpdateTime` vs. the file's
   `LastWriteTime`. Tonight the phone held an older APK than the fix — the "bug" we
   chased was already fixed, just not installed.
3. **Never accept "BUILD SUCCESSFUL" as proof that a helper task ran.** Gradle prints
   success even when your task silently didn't execute. Look for your task's log line
   ("SPARK dev: USB tunnel active…") and the task count (`N actionable tasks`), and
   verify the *side effect* (`adb reverse --list`) — not the exit code.
4. **When a mechanism works in one mode and is silently ignored in another
   (classic Gradle vs configuration cache), choose the boring deterministic option.**
   Half-working cleverness costs more than it saves on a weak PC.
5. **Read the whole error message once.** Kotlin's "None of the following candidates is
   applicable" lists the candidates — that list *is* the answer (wrong receiver type).
   Skimming it cost two extra build cycles tonight.

---

## Entries

### Entry 1 — 2026-10-17 — Phone can't reach the PC's dev API over Wi-Fi  (Phase 4)
**Symptom:** App on phone shows "No connection. Check Wi-Fi." even though phone and PC
are on the same Wi-Fi and the API answers on the PC (`curl` OK, server listening on
`0.0.0.0:3000`, `netstat` shows LISTENING).
**Proof:** From the phone: `adb shell ping 192.168.29.183` → **100% packet loss**.
Also `Get-NetConnectionProfile` showed the Wi-Fi is `NetworkCategory=Public` in Windows.
**Root cause:** Windows treats this Wi-Fi as a Public network; the firewall silently
drops inbound connections (phone→PC). Two network stacks never talked; the app's
`IOException` had nothing to do with passcode or API.
**Fix:** Bypass the firewall entirely — a **USB tunnel**:
`adb reverse tcp:3000 tcp:3000` (run on PC) makes the phone's `http://127.0.0.1:3000`
reach the PC's port 3000 through the cable. App's debug `API_BASE_URL` is now
`http://127.0.0.1:3000` (app/build.gradle.kts).
**One-line learning:** For device↔PC dev traffic, prefer the USB tunnel over Wi-Fi —
it ignores firewalls, router AP-isolation, and hotspot guessing.
**If it happens again:** run `adb reverse tcp:3000 tcp:3000`, confirm with
`adb reverse --list`; the build now re-arms it automatically on every Run (Entry 7).
STATUS: verified

### Entry 2 — 2026-10-17 — "Fixed" code never reached the phone (stale APK trap)  (Phase 4)
**Symptom:** After changing the API URL, the phone app failed exactly as before.
**Proof:** App installed at **08:45:39**
(`adb shell dumpsys package com.spark.app.debug | findstr lastUpdateTime`) but the
URL change was saved at **08:49:08** (file `LastWriteTime`) — the phone had a build
*older than the fix*. No new install had happened since.
**Root cause:** The last Run didn't rebuild/redeploy (slow sync confusion), so we were
debugging the old binary while "testing the fix".
**Fix:** `adb uninstall com.spark.app.debug` + tell the user to press Run (fresh
install). Future: check `lastUpdateTime` vs. edit time *before* theorizing.
**One-line learning:** Behaviour smells stale? Prove the artifact's freshness with
timestamps before hunting for another cause.
**If it happens again:** uninstall the app, press Run once, re-test. Two minutes.
STATUS: verified

### Entry 3 — 2026-10-17 — adb tunnel dies silently; must re-arm on every build  (Phase 4)
**Symptom:** Tunnel worked at setup time, then later builds/deploys can hit "no
connection" again (adb drops `reverse` when its server restarts, on USB replug etc.).
**Fix:** A `adbReverseDev` Gradle task runs on every `preBuild` (i.e., every Studio
Run) and re-arms the tunnel. Wired via `afterEvaluate { tasks.named("preBuild")
{ dependsOn("adbReverseDev") } }` in app/build.gradle.kts. On CI (no
local.properties/adb) the task exits immediately — harmless.
**One-line learning:** Anything that exists only in a machine's runtime memory
(tunnel, sshtunnel, dev server port-forward) needs an *automatic re-arming step* in
the build — manual steps WILL be forgotten at the worst time.
**If it happens again:** build, look for the "SPARK dev: USB tunnel active" line in
the build output; if missing, run `gradle :app:adbReverseDev` directly.
STATUS: verified

### Entry 4 — 2026-10-17 — Gradle Kotlin DSL: `java.util.Properties` = "Unresolved reference 'util'"  (Phase 4)
**Symptom:** First line of the build helper failed to compile with a cascade of
"Unresolved reference" errors.
**Root cause:** Gradle Kotlin DSL reserves names like `java`, `kotlin`, `tasks` as
domain-accessors; `java.util.Properties` resolves against the *extension*, not the
JDK package.
**Fix:** top-of-file `import java.util.Properties` (imports before `plugins {}` are
allowed) and use `Properties()`.
**One-line learning:** In `build.gradle.kts`, never trust fully-qualified JDK names
that start with an accessor-named package — import them instead.
STATUS: verified

### Entry 5 — 2026-10-17 — Gradle 9: `exec {}` gone from `doLast`; `InputStream.readText()` missing  (Phase 4)
**Symptom:** Two script compile failures in the same helper:
(1) "Unresolved reference 'exec'" inside `doLast {}`;
(2) `readText()` on a process stream → "None of the following candidates is
applicable" (candidates were only `File`, `Reader`, `URL`).
**Root cause:** Gradle 9 removed `Task.exec`; and this Kotlin stdlib only defines
`readText()` for `Reader`/`File`/`URL`, not raw `InputStream`.
**Fix:** used plain **`ProcessBuilder`** (redirectErrorStream →
`bufferedReader().readText()` → `waitFor()`), wrapped in try/catch that logs instead
of failing the build.
**One-line learning:** In Gradle 9 task code, shell out with `ProcessBuilder`; read
streams via a `BufferedReader`. And read the candidate list in the error — it names
the receivers that *are* valid.
STATUS: verified

### Entry 6 — 2026-10-17 — Configuration cache refuses closure-based tasks  (Phase 4)
**Symptom:** Task itself worked, then `BUILD FAILED` *after* execution with
"Configuration cache problems found… Task :app:adbReverseDev of type DefaultTask:
cannot serialize Gradle script object references".
**Proof:** The failure listed the exact task and the disallowed-type doc link.
**Root cause:** The `tasks.register("…") { doLast { … } }` lambda captures script-level
values (`sparkAdb`); configuration cache can serialize *task state*, not *closures*.
**Fix:** declared a proper task class:
`abstract class SparkAdbReverseTask : DefaultTask()` with `@get:Input abstract val
adbPath: Property<String>` and a `@TaskAction fun run()` — only plain-string state.
**One-line learning:** With configuration cache on, task classes with serializable
properties are the only robust "do something at execution" pattern; closures that
touch script values are rejected.
STATUS: verified (task class kept; cache later disabled — Entry 7)

### Entry 7 — 2026-10-17 — Configuration cache *silently* drops dynamically added `dependsOn`  (Phase 4)
**Symptom:** helper ran with `--no-configuration-cache` but vanished when the cache was
used — build succeeded, helper never executed, zero warnings.
**Proof:** `gradle :app:preBuild` printed no helper log and `1 actionable task`; the
same command with `--no-configuration-cache` printed "SPARK dev: USB tunnel active".
Tried both `tasks.matching{}.configureEach{dependsOn}` and
`afterEvaluate{ tasks.named("preBuild"){ dependsOn } }`.
**Root cause:** with configuration cache, dependencies attached in
task-realization-time callbacks don't reliably make it into the stored task graph.
**Fix:** set `org.gradle.configuration-cache=false` in gradle.properties (with a
comment explaining why), kept the verified classic wiring, and re-verified everything
with plain flags (no `--no-configuration-cache` needed).
**One-line learning:** Silent no-op > loud error. When a feature's value is small
(a few seconds of sync speed here) and correctness/learning cost is high, turn the
magic off and keep what is demonstrably working. Revisit later from the docs.
**If it happens again:** disable the cache, re-run the *real* command path once
(plain flags), and confirm the side effect is visible in the log.
STATUS: verified

---

## Historical hard-won fixes (earlier phases, kept so we don't repeat them)

### Entry 8 — Oct 2026 (Phase 2) — Supabase direct host unreachable; wrong pooler port breaks Prisma
**Symptom:** two separate failures: (1) direct DB host unresolvable/unreachable;
(2) migrations failing with `prepared statement s1 already exists` through the
transaction pooler.
**Proof:** connection string experiments + Prisma error text pointing at pgbouncer.
**Root cause:** (1) the direct host is IPv6-only and the user's network has no
IPv6; (2) port 6543 = pgbouncer *transaction* mode, incompatible with Prisma's
prepared statements during migrations.
**Fix:** always use the **session pooler** on **port 5432**
(`aws-0-ap-south-1.pooler.supabase.com`, user `postgres.<project-ref>`).
**One-line learning:** Serverless/edge hosts differ from dev laptops: pick
connection modes by what the *tool* needs (prepared statements → session mode),
not by the provider's "recommended" default.
STATUS: verified (in ongoing use — never change without testing migrations)

### Entry 9 — Oct 2026 (Phase 2) — Stale Prisma client after schema change → runtime 500
**Symptom:** `PUT /items/:id/progress` returned 500 though the code was right.
**Fix:** after every `prisma migrate dev`, regenerate the client and **restart
`npm run dev`**.
**One-line learning:** Schema change = regenerate + restart; treat the dev server as
stateful. "It worked before the migration" almost always means stale generated code.
STATUS: verified

### Entry 10 — Oct 2026 (Phases 1–2) — CI file can't be pushed by the agent (missing `workflow` scope)
**Symptom:** `git push` of `.github/workflows/ci.yml` → 403.
**Fix:** the *user* pastes CI-file changes via the GitHub web UI; agent commits
everything else. Four failed attempts before this became the rule.
**One-line learning:** Check what the credential is allowed to do once, and route
around the gap — don't retry the same forbidden action hoping for different results.
STATUS: permanent rule

### Entry 11 — Oct 2026 (Phase 1) — `next-env.d.ts` churns between dev/build
**Fix:** kept it git-ignored; `api/AGENTS.md` carries the Next.js agent rules.
**One-line learning:** Framework-generated files that differ between commands
belong in `.gitignore`, not in review diffs.
STATUS: verified

### Entry 12 — Oct 2026 (Phase 0/1) — Prisma 7 does not work the way pre-7 tutorials say
**Symptom:** `P5001`-style breakage from memory-based setup (no `prisma.config.ts`,
no driver adapter).
**Fix:** `prisma.config.ts` + `dotenv` + `@prisma/adapter-pg` (client constructor
requires `adapter` option); generator output to `api/generated/prisma` (untracked).
**One-line learning:** Prisma versions rewrite setup steps; check current docs for
*that* major version before touching config — never carry old boilerplate forward.
STATUS: verified

---

## General playbook — how to approach any "it doesn't work" here

1. **Restate the failure in one sentence, then ask: what would prove that sentence
   true or false?** Find the *cheapest proof command* (ping, curl, `--list`,
   `dumpsys`, mtime compare). Evidence beats theory.
2. **Test from the far end of the pipe, not the middle.** App↔USB-tunnel↔adb↔Node —
   check both ends and, if needed, hop by hop (PC curl → phone ping → tunnel list).
3. **Change one thing at a time**; after each change, re-prove the failure changed
   (or didn't). If the user is pressing buttons, give *one* instruction.
4. **Prove the build/artifact is fresh** (Entry 2) before re-testing behaviour.
5. **Treat every runtime helper (tunnel, dev server, generated client) as stateful**;
   each needs an automatic re-arm or a restart ritual (Entries 3, 9).
6. **Verify effects, not exit codes** (Entry 3, Rule 3).
7. **When in doubt, boring wins**: prefer the option that keeps desks of moving parts
   down, even if it's a bit slower (Entry 7).
8. **Then write the entry here** with STATUS verified — so no session re-pays the
   tuition.

---

*Last updated: 2026-10-17 (Phase 4, branch `phase-4-app`).*
