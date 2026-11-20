"""
Step 1: Extract Audio (Pure 128 kbps MP3) from YouTube / Media Link
Usage:
  python 01_extract_audio.py --url "https://youtu.be/..." --out-dir "D:/books/my-book"
"""

import argparse
import os
import subprocess
import sys

def extract_audio(url: str, out_dir: str) -> str:
    os.makedirs(out_dir, exist_ok=True)
    out_template = os.path.join(out_dir, "master_audio.%(ext)s")

    cmd = [
        "yt-dlp",
        "--js-runtimes", "node:node",
        "--extract-audio",
        "--audio-format", "mp3",
        "--audio-quality", "128K",
        "--write-info-json",
        "--write-thumbnail",
        "-o", out_template,
        url
    ]

    print(f"[*] Downloading audio and metadata from {url}...")
    res = subprocess.run(cmd)
    if res.returncode != 0:
        print("[!] Error downloading audio with yt-dlp", file=sys.stderr)
        sys.exit(1)

    mp3_path = os.path.join(out_dir, "master_audio.mp3")
    if not os.path.exists(mp3_path):
        # Find any .mp3 in out_dir
        for f in os.listdir(out_dir):
            if f.endswith(".mp3"):
                mp3_path = os.path.join(out_dir, f)
                break

    print(f"[+] Audio successfully extracted to: {mp3_path}")
    return mp3_path

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract pure 128kbps MP3 audio from a media link")
    parser.add_argument("--url", required=True, help="YouTube or media link")
    parser.add_argument("--out-dir", required=True, help="Output directory")
    args = parser.parse_args()

    extract_audio(args.url, args.out_dir)
