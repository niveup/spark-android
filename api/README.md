# SPARK API (Phase 2) — Next.js + Prisma + Supabase Postgres

Implements `docs/API_CONTRACT.md` (`/api/mobile/v1/*`). DEV only for now.

## Setup (on your PC)

1. Install Node 20+ (you have it: `node --version`).
2. Copy env: `copy api\.env.example api\.env` then fill `DATABASE_URL`
   (Supabase dashboard → Project Settings → Database → Connection string,
   replace `[DB-PASSWORD]`) and `SPARK_PASSCODE`.
3. Install + validate (no DB touched):
   `cd api && npm install && npx prisma validate`
4. Run DEV server: `npm run dev` → http://localhost:3000/api/mobile/v1/health
   should return `{"data":{"ok":true,"version":"v1"}}`.

## Rules (from AGENTS.md)

- Additive migrations only. Never `migrate reset` / `db push --force-reset`
  against anything but a local throwaway DB. No PROD database exists yet.
- `api/.env` is git-ignored. Secrets never enter the repo or the APK.
- Every request body/query is validated with zod; contract errors only
  (`UNAUTHORIZED/VALIDATION/NOT_FOUND/RATE_LIMITED/CONFLICT/INTERNAL`).
