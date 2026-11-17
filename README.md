# SPARK (working name) — Personal Productive Feed, Android App

Native Kotlin + Jetpack Compose, Android-only.

See `AGENTS.md` for full brief and `docs/HANDOFF.md` for phase plan.

## Run it on the phone

Start the API and configure the USB tunnel in one step:

```powershell
.\scripts\dev.ps1                      # starts API, opens USB tunnel, checks /health and /books
```

Then, from the repo root, rebuild and reinstall with a clean slate:

```powershell
.\scripts\refresh-app.ps1              # removes the old app first (also logs you out)
.\scripts\refresh-app.ps1 -KeepData    # replace the APK but keep the token + offline cache
.\scripts\refresh-app.ps1 -NoBuild     # reuse the last APK
```

The script refuses to run if no phone is connected, so a stale phone build
cannot be mistaken for the current code.
