"""
SPARK Autonomous Book Addition Pipeline (All-In-One Master CLI)
SOP Compliance:
  1. Downloads pure 128 kbps MP3 & timed transcript (VTT).
  2. Slices audio chapterwise, strictly dividing any chapter > 18 MB into <= 18 MB parts.
  3. Uploads audio slices, HD cover, and transcript directly to Telegram Cloud.
  4. Stitches multi-part chapters on backend so frontend displays ONLY single chapters.
  5. Cleans previous duplicates and maps clean chapters to Supabase.

Usage:
  python auto_add_book.py --url "https://youtu.be/..." --title "The 48 Laws of Power" --author "Robert Greene"
"""

import argparse
import os
import sys
import subprocess
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def run_command(cmd: list, cwd: str = None) -> bool:
    print(f"\n>>> Running: {' '.join(cmd)}")
    res = subprocess.run(cmd, cwd=cwd)
    if res.returncode != 0:
        print(f"[!] Step failed with exit code {res.returncode}", file=sys.stderr)
        return False
    return True

def auto_add_book(url: str, title: str, author: str, book_id: str = None, work_dir: str = None):
    slug = title.lower().replace(" ", "-").replace("'", "")
    if not work_dir:
        work_dir = os.path.join(r"D:\books", slug)
    os.makedirs(work_dir, exist_ok=True)

    print("=" * 60)
    print(f"SPARK AUTONOMOUS BOOK PIPELINE: {title}")
    print(f"Working Directory: {work_dir}")
    print("=" * 60)

    # 1. Extract Audio
    audio_script = os.path.join(SCRIPT_DIR, "01_extract_audio.py")
    if not run_command([sys.executable, audio_script, "--url", url, "--out-dir", work_dir]):
        sys.exit(1)

    mp3_file = os.path.join(work_dir, "master_audio.mp3")
    if not os.path.exists(mp3_file):
        for f in os.listdir(work_dir):
            if f.endswith(".mp3"):
                mp3_file = os.path.join(work_dir, f)
                break

    # 2. Extract Transcript
    transcript_script = os.path.join(SCRIPT_DIR, "02_extract_transcript.py")
    run_command([sys.executable, transcript_script, "--url", url, "--out-dir", work_dir])

    vtt_file = ""
    for f in os.listdir(work_dir):
        if f.endswith(".vtt"):
            vtt_file = os.path.join(work_dir, f)
            break

    # 3. Find info.json and thumbnail/cover
    info_json = ""
    cover_file = ""
    for f in os.listdir(work_dir):
        if f.endswith(".info.json"):
            info_json = os.path.join(work_dir, f)
        elif f.endswith(".jpg") or f.endswith(".webp") or f.endswith(".png"):
            cover_file = os.path.join(work_dir, f)

    # 4. Slice Audio (<= 18 MB)
    slices_dir = os.path.join(work_dir, "slices_18mb")
    slice_script = os.path.join(SCRIPT_DIR, "03_slice_audio_18mb.py")
    slice_cmd = [sys.executable, slice_script, "--input-mp3", mp3_file, "--out-dir", slices_dir]
    if info_json:
        slice_cmd.extend(["--info-json", info_json])
    if not run_command(slice_cmd):
        sys.exit(1)

    slices_manifest = os.path.join(slices_dir, "slices_manifest.json")

    # 5. Upload to Telegram
    upload_script = os.path.join(SCRIPT_DIR, "04_upload_to_telegram.py")
    upload_cmd = [sys.executable, upload_script, "--slices-manifest", slices_manifest]
    if cover_file:
        upload_cmd.extend(["--cover-image", cover_file])
    if vtt_file:
        upload_cmd.extend(["--transcript", vtt_file])
    if not run_command(upload_cmd):
        sys.exit(1)

    progress_file = slices_manifest.replace(".json", "_progress.json")

    # 6. Map to SPARK Database
    map_script = os.path.join(SCRIPT_DIR, "05_map_to_spark.ts")
    api_dir = r"D:\new something\api"
    map_cmd = [
        "npx.cmd", "tsx", map_script,
        "--book-id", book_id or "auto",
        "--title", title,
        "--author", author,
        "--progress-file", progress_file
    ]
    if not run_command(map_cmd, cwd=api_dir):
        sys.exit(1)

    print("\n" + "=" * 60)
    print(f"SUCCESS: \"{title}\" successfully processed, uploaded & mapped to SPARK!")
    print("=" * 60)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Autonomous end-to-end book addition pipeline")
    parser.add_argument("--url", required=True, help="YouTube video or media URL")
    parser.add_argument("--title", required=True, help="Book title")
    parser.add_argument("--author", default="Unknown", help="Book author")
    parser.add_argument("--book-id", help="Existing Book ID in Supabase/Postgres")
    parser.add_argument("--work-dir", help="Directory to store intermediate audio and slices")
    args = parser.parse_args()

    auto_add_book(args.url, args.title, args.author, args.book_id, args.work_dir)
