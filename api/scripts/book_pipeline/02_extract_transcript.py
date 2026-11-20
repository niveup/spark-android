"""
Step 2: Extract Timestamped Transcript (VTT) from YouTube / Media Link
Usage:
  python 02_extract_transcript.py --url "https://youtu.be/..." --out-dir "D:/books/my-book"
"""

import argparse
import os
import subprocess
import sys

def extract_transcript(url: str, out_dir: str) -> str:
    os.makedirs(out_dir, exist_ok=True)
    out_prefix = os.path.join(out_dir, "transcript")

    cmd = [
        "yt-dlp",
        "--js-runtimes", "node:node",
        "--write-auto-subs",
        "--write-subs",
        "--sub-lang", "en",
        "--convert-subs", "vtt",
        "--skip-download",
        "-o", out_prefix,
        url
    ]

    print(f"[*] Downloading transcript from {url}...")
    res = subprocess.run(cmd)
    if res.returncode != 0:
        print("[!] Warning: yt-dlp subtitles fetch failed or no English subtitles found.", file=sys.stderr)

    # Locate transcript file
    vtt_file = ""
    for f in os.listdir(out_dir):
        if f.endswith(".vtt"):
            vtt_file = os.path.join(out_dir, f)
            break

    if vtt_file:
        print(f"[+] Transcript successfully extracted to: {vtt_file}")
    else:
        print("[!] No .vtt transcript file was found in directory.")

    return vtt_file

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract timed VTT transcript from a media link")
    parser.add_argument("--url", required=True, help="YouTube or media link")
    parser.add_argument("--out-dir", required=True, help="Output directory")
    args = parser.parse_args()

    extract_transcript(args.url, args.out_dir)
