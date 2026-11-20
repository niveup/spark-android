"""
Step 4: Upload Audio Slices, Cover Art & Transcript to Telegram Cloud
Usage:
  python 04_upload_to_telegram.py --slices-manifest "slices/slices_manifest.json" [--cover-image "cover.jpg"] [--transcript "transcript.vtt"]
"""

import argparse
import os
import sys
import json
import subprocess
import time
import shutil

ENV_FILE = r"D:\new something\api\.env"
CACHE_DIR = r"D:\new something\api\public\audio"
CHAT_ID = "5356318710"

os.makedirs(CACHE_DIR, exist_ok=True)

def load_bot_token() -> str:
    if not os.path.exists(ENV_FILE):
        print(f"[!] .env file not found at {ENV_FILE}", file=sys.stderr)
        return ""
    with open(ENV_FILE, "r", encoding="utf-8") as f:
        for line in f:
            if line.startswith("TELEGRAM_BOT_TOKEN="):
                return line.strip().split("=", 1)[1].strip("\"'")
    return ""

def upload_to_telegram(slices_manifest: str, cover_image: str = None, transcript_file: str = None):
    token = load_bot_token()
    if not token:
        print("[!] Error: TELEGRAM_BOT_TOKEN missing in api/.env", file=sys.stderr)
        sys.exit(1)

    with open(slices_manifest, "r", encoding="utf-8") as f:
        slices = json.load(f)

    progress_file = slices_manifest.replace(".json", "_progress.json")
    uploaded_map = {}
    if os.path.exists(progress_file):
        try:
            with open(progress_file, "r", encoding="utf-8") as f:
                for item in json.load(f):
                    if item.get("telegram_file_id"):
                        uploaded_map[item["id"]] = item["telegram_file_id"]
        except:
            pass

    # 1. Upload Cover Image (sendPhoto)
    cover_file_id = ""
    if cover_image and os.path.exists(cover_image):
        print(f"[*] Uploading Cover Art: {cover_image}...")
        cmd = [
            "curl.exe", "-s", "--noproxy", "*",
            "-F", f"chat_id={CHAT_ID}",
            "-F", f"photo=@{cover_image}",
            f"https://api.telegram.org/bot{token}/sendPhoto"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        try:
            data = json.loads(res.stdout)
            if data.get("ok"):
                photos = data["result"]["photo"]
                cover_file_id = photos[-1]["file_id"] # highest resolution
                print(f"[+] Cover uploaded! file_id: {cover_file_id[:16]}...")
        except Exception as e:
            print(f"[!] Cover upload failed: {e}")

    # 2. Upload Transcript Document (sendDocument)
    transcript_file_id = ""
    if transcript_file and os.path.exists(transcript_file):
        print(f"[*] Uploading Transcript: {transcript_file}...")
        cmd = [
            "curl.exe", "-s", "--noproxy", "*",
            "-F", f"chat_id={CHAT_ID}",
            "-F", f"document=@{transcript_file}",
            f"https://api.telegram.org/bot{token}/sendDocument"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        try:
            data = json.loads(res.stdout)
            if data.get("ok"):
                transcript_file_id = data["result"]["document"]["file_id"]
                print(f"[+] Transcript uploaded! file_id: {transcript_file_id[:16]}...")
        except Exception as e:
            print(f"[!] Transcript upload failed: {e}")

    # 3. Upload Slices (sendAudio)
    total = len(slices)
    print(f"\n[*] Uploading {total} audio slices to Telegram (Strictly <= 18 MB)...")

    for idx, s in enumerate(slices):
        sid = s["id"]
        title = s["title"]
        file_path = s["file_path"]
        size_mb = s["size_mb"]

        if size_mb > 18.0:
            print(f"[!] REJECTED: {title} is {size_mb} MB (>18MB limit). Slicing bug!")
            continue

        if sid in uploaded_map and uploaded_map[sid]:
            fid = uploaded_map[sid]
            s["telegram_file_id"] = fid
            local_cache = os.path.join(CACHE_DIR, f"{fid}.mp3")
            if not os.path.exists(local_cache) and os.path.exists(file_path):
                shutil.copyfile(file_path, local_cache)
            print(f"[{idx+1}/{total}] {title} ({size_mb} MB) -> ALREADY UPLOADED ({fid[:14]}...)")
            continue

        print(f"[{idx+1}/{total}] Uploading {title} ({size_mb} MB)...")
        cmd = [
            "curl.exe", "-s", "--noproxy", "*",
            "-F", f"chat_id={CHAT_ID}",
            "-F", f"title={title}",
            "-F", f"audio=@{file_path}",
            f"https://api.telegram.org/bot{token}/sendAudio"
        ]

        success = False
        for attempt in range(3):
            try:
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
                if res.returncode == 0 and res.stdout:
                    data = json.loads(res.stdout)
                    if data.get("ok"):
                        fid = data["result"]["audio"]["file_id"]
                        uploaded_map[sid] = fid
                        s["telegram_file_id"] = fid

                        local_cache = os.path.join(CACHE_DIR, f"{fid}.mp3")
                        shutil.copyfile(file_path, local_cache)
                        print(f"  [+] SUCCESS! file_id: {fid[:16]}...")
                        success = True
                        break
                    elif data.get("error_code") == 429:
                        retry_after = data.get("parameters", {}).get("retry_after", 10)
                        print(f"  [!] Rate limited! Sleeping {retry_after}s...")
                        time.sleep(retry_after)
                    else:
                        print(f"  [!] Telegram error: {data.get('description')}")
            except Exception as e:
                print(f"  [!] Attempt {attempt+1} error: {e}")
            time.sleep(2)

        if not success:
            print(f"[!] FAILED to upload {title} after 3 attempts.")

        # Save progress
        with open(progress_file, "w", encoding="utf-8") as f:
            json.dump(slices, f, indent=2, ensure_ascii=False)

    print(f"\n[+] Telegram uploads complete! Saved to {progress_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Upload audio slices and media to Telegram cloud")
    parser.add_argument("--slices-manifest", required=True, help="slices_manifest.json path")
    parser.add_argument("--cover-image", help="Cover art image path")
    parser.add_argument("--transcript", help="VTT transcript file path")
    args = parser.parse_args()

    upload_to_telegram(args.slices_manifest, args.cover_image, args.transcript)
