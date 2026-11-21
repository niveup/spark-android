# SPARK API — Vercel Deployment & Cloud Sync Guide

This guide explains how to deploy the SPARK Next.js API to **Vercel** (free tier) and connect your SPARK Android app to it so you no longer need a USB cable or local server.

---

## 1. Prerequisites (What you need)
- A free **GitHub** account (where this repository is hosted: `niveup/spark-android`).
- A free **Vercel** account ([vercel.com](https://vercel.com)).
- Your existing **Supabase** database credentials and **Telegram Bot Token** from `api/.env`.

---

## 2. Step-by-Step Vercel Deployment (3 Minutes)

### Step 2.1: Import the Repository
1. Log in to [vercel.com](https://vercel.com).
2. Click **Add New...** &rarr; **Project**.
3. Select your repository: **`spark-android`**.

### Step 2.2: Configure Project Settings (IMPORTANT)
1. **Framework Preset:** Next.js (automatically detected).
2. **Root Directory:** Click **Edit** and set it to **`api`**.
   *(Because the Next.js API lives inside the `api` folder of the repo).*
3. **Build Command:** Leave default (`prisma generate && next build` is already configured in `package.json`).
4. **Install Command:** Leave default (`npm install`).

### Step 2.3: Add Environment Variables
Under the **Environment Variables** section on Vercel, add these 3 variables:

| Variable Name | Description & Recommended Value |
|---|---|
| `DATABASE_URL` | **Use Supabase Connection Pooler URL** (port 6543):<br>`postgresql://postgres.[PROJECT-REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres?pgbouncer=true`<br>*(Found in Supabase Dashboard &rarr; Project Settings &rarr; Database &rarr; Connection Pooling)*. |
| `SPARK_PASSCODE` | Your secret mobile login passcode (e.g. copied from your `api/.env`). |
| `TELEGRAM_BOT_TOKEN` | Your Telegram Bot token for fetching audio chapters and Ultra HD book covers. |

### Step 2.4: Deploy
- Click **Deploy**.
- Vercel will install dependencies, run `prisma generate`, compile the Next.js API, and provide a live URL like:
  `https://spark-api-[username].vercel.app`

---

## 3. Verify Your Live Deployment
Once deployed, open your browser and visit:
1. `https://your-spark-api.vercel.app/api/mobile/v1/health`
   - Should return: `{"data":{"ok":true,"version":"v1"}}`
2. `https://your-spark-api.vercel.app/api/mobile/v1/books` (with Authorization header) or health check.

---

## 4. Connect Your Android App to the Cloud API

You can switch the Android app to use your cloud API in one step without modifying any git-tracked files!

### Option A: Via `local.properties` (Recommended)
Open `local.properties` in your project root and add this single line:
```properties
spark.api.url=https://your-spark-api.vercel.app
```
Then run:
```powershell
.\scripts\refresh-app.ps1 -KeepData
```
*Your phone will now connect directly to your live Vercel cloud API over Wi-Fi / 5G!*

*(To switch back to local dev at any time, just comment out or delete that line).*

### Option B: Via Command Line
You can pass the URL directly during build:
```powershell
.\gradlew assembleDebug -Pspark.api.url=https://your-spark-api.vercel.app
```

---

## 5. Architecture & Serverless Safeguards Built Into SPARK

- **Fluid Compute (300s timeout):** Audio streaming routes (`/api/mobile/v1/audio/[fileId]`) have `maxDuration = 300` configured, taking full advantage of Vercel's 5-minute serverless execution window.
- **HTTP Chunked Streaming:** Audio is streamed via `Readable.toWeb` with HTTP Range headers (`206 Partial Content`), bypassing Vercel's static 4.5 MB payload limit.
- **Serverless-Safe Caching:** Downloaded audio chunks and book covers are cached in `os.tmpdir()` (`/tmp`), respecting Vercel's read-only container filesystem.
- **Static Assets Bundling:** `outputFileTracingIncludes` in `next.config.ts` ensures all chapter audio manifests and subtitle JSON files are packaged directly into the serverless function bundles.
