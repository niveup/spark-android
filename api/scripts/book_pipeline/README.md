# SPARK Autonomous Book Addition Pipeline

This directory contains the automated toolchain implementing the `/book-addition-sop` skill. It handles end-to-end extraction, slicing, Telegram cloud hosting, backend stitching, and Supabase database mapping for any book.

---

## 🚀 Quick Start (All-In-One Master Command)

To process and add an entire book in one step:

```powershell
python "D:\new something\api\scripts\book_pipeline\auto_add_book.py" `
  --url "https://youtu.be/..." `
  --title "Atomic Habits" `
  --author "James Clear"
```

The script will automatically:
1. Extract pure 128 kbps MP3 & timed VTT transcript.
2. Detect chapters and slice audio (strictly dividing any chapter $> 18\text{ MB}$ to satisfy Telegram limits).
3. Upload slices, HD cover, and transcript directly to Telegram Cloud.
4. Pre-assemble multi-part chapters on the backend (so the frontend sees **only single chapters**).
5. Clean previous duplicates and map clean chapters to Supabase.

---

## 🛠 Individual Modular Scripts

If you ever want to run or test steps individually:

### 1. Extract Audio
```powershell
python "D:\new something\api\scripts\book_pipeline\01_extract_audio.py" `
  --url "<YOUTUBE_URL>" `
  --out-dir "D:\books\<book-name>"
```
- Extracts pure 128 kbps MP3 directly (`master_audio.mp3`) + metadata + cover art.

### 2. Extract Transcript
```powershell
python "D:\new something\api\scripts\book_pipeline\02_extract_transcript.py" `
  --url "<YOUTUBE_URL>" `
  --out-dir "D:\books\<book-name>"
```
- Downloads timestamped captions in `.vtt` format (`transcript.en.vtt`).

### 3. Slice Audio ($\le 18\text{ MB}$)
```powershell
python "D:\new something\api\scripts\book_pipeline\03_slice_audio_18mb.py" `
  --input-mp3 "D:\books\<book-name>\master_audio.mp3" `
  --out-dir "D:\books\<book-name>\slices" `
  --info-json "D:\books\<book-name>\master_audio.info.json"
```
- Slices chapterwise. If a chapter exceeds 18 MB, it divides it into Part 1 and Part 2.
- Generates `slices_manifest.json`.

### 4. Upload to Telegram Cloud
```powershell
python "D:\new something\api\scripts\book_pipeline\04_upload_to_telegram.py" `
  --slices-manifest "D:\books\<book-name>\slices\slices_manifest.json" `
  --cover-image "D:\books\<book-name>\cover.jpg" `
  --transcript "D:\books\<book-name>\transcript.vtt"
```
- Uploads slices via `sendAudio`, cover via `sendPhoto`, and transcript via `sendDocument`.
- Saves `slices_manifest_progress.json` with all Telegram `file_id`s.

### 5. Map to SPARK Database
```powershell
Set-Location "D:\new something\api"
npx tsx scripts/book_pipeline/05_map_to_spark.ts `
  --title "Atomic Habits" `
  --author "James Clear" `
  --book-id "auto" `
  --progress-file "D:\books\<book-name>\slices\slices_manifest_progress.json"
```
- Stitches multi-part chapters on the backend.
- Purges old duplicate items.
- Inserts clean, pristine chapters in Supabase with zero "(Part 1/2)" exposed to the user.
