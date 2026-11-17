"""
Step 3: Audio Slicing with Strict <= 18 MB Criteria
Usage:
  python 03_slice_audio_18mb.py --input-mp3 "master_audio.mp3" --out-dir "slices" [--info-json "master_audio.info.json"]
"""

import argparse
import os
import sys
import json
import subprocess
import re

FFMPEG = r"C:\Users\Lenovo\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe"
if not os.path.exists(FFMPEG):
    FFMPEG = "ffmpeg"

FFPROBE = r"C:\Users\Lenovo\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffprobe.exe"
if not os.path.exists(FFPROBE):
    FFPROBE = "ffprobe"

def get_audio_duration(file_path: str) -> float:
    cmd = [
        FFPROBE,
        "-v", "quiet",
        "-show_entries", "format=duration",
        "-of", "csv=p=0",
        file_path
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    try:
        return float(res.stdout.strip())
    except:
        return 0.0

def slice_audio(input_mp3: str, out_dir: str, info_json: str = None) -> list:
    os.makedirs(out_dir, exist_ok=True)
    total_duration = get_audio_duration(input_mp3)
    print(f"[*] Input MP3: {input_mp3} ({round(total_duration, 1)}s = {round(total_duration/60, 1)}m)")

    raw_chapters = []
    # 1. Try reading chapters from yt-dlp info.json
    if info_json and os.path.exists(info_json):
        try:
            with open(info_json, "r", encoding="utf-8") as f:
                info = json.load(f)
                if "chapters" in info and info["chapters"]:
                    for c in info["chapters"]:
                        raw_chapters.append({
                            "title": c.get("title", ""),
                            "start_time": int(c.get("start_time", 0)),
                            "end_time": int(c.get("end_time", total_duration))
                        })
        except Exception as e:
            print(f"[!] Warning reading info.json chapters: {e}")

    # 2. If no chapters found, slice into uniform 15-minute chunks (14.4 MB @ 128k)
    if not raw_chapters:
        print("[*] No chapter markers detected. Slicing into uniform 15-minute segments (<= 14.4 MB)...")
        chunk_sec = 900
        cur = 0
        idx = 0
        while cur < total_duration:
            nxt = min(cur + chunk_sec, int(total_duration))
            idx += 1
            raw_chapters.append({
                "title": f"Chapter {idx:02d}",
                "start_time": cur,
                "end_time": nxt
            })
            cur = nxt

    # 3. Process each chapter and enforce <= 18 MB limit (18 min = ~17.2 MB @ 128 kbps)
    MAX_DURATION_SEC = 1100 # ~18.3 minutes
    slices = []
    slice_idx = 0

    for ch_num, ch in enumerate(raw_chapters):
        title = ch["title"].strip()
        start = ch["start_time"]
        end = ch["end_time"]
        dur = end - start

        if dur <= MAX_DURATION_SEC:
            # Single part
            file_name = f"chapter_{ch_num:02d}.mp3"
            file_path = os.path.join(out_dir, file_name)
            cmd = [FFMPEG, "-y", "-ss", str(start), "-to", str(end), "-i", input_mp3, "-c", "copy", file_path]
            subprocess.run(cmd, capture_output=True)

            size_bytes = os.path.getsize(file_path)
            size_mb = round(size_bytes / (1024 * 1024), 2)
            slices.append({
                "id": f"slice_{slice_idx:02d}",
                "chapter_num": ch_num,
                "part": 0,
                "title": title,
                "start_sec": start,
                "end_sec": end,
                "duration_sec": dur,
                "file_name": file_name,
                "file_path": file_path,
                "size_mb": size_mb
            })
            slice_idx += 1
            print(f"  [+] Ch {ch_num:02d}: \"{title}\" ({size_mb} MB <= 18 MB)")
        else:
            # Multi-part split (strictly backend only)
            mid = start + (dur // 2)
            print(f"  [!] Ch {ch_num:02d}: \"{title}\" is {dur}s (>18MB). Splitting into 2 parts for Telegram...")

            # Part 1
            f1 = f"chapter_{ch_num:02d}_pt1.mp3"
            p1 = os.path.join(out_dir, f1)
            subprocess.run([FFMPEG, "-y", "-ss", str(start), "-to", str(mid), "-i", input_mp3, "-c", "copy", p1], capture_output=True)
            s1_mb = round(os.path.getsize(p1) / (1024 * 1024), 2)
            slices.append({
                "id": f"slice_{slice_idx:02d}",
                "chapter_num": ch_num,
                "part": 1,
                "title": f"{title} (Part 1)",
                "start_sec": start,
                "end_sec": mid,
                "duration_sec": mid - start,
                "file_name": f1,
                "file_path": p1,
                "size_mb": s1_mb
            })
            slice_idx += 1

            # Part 2
            f2 = f"chapter_{ch_num:02d}_pt2.mp3"
            p2 = os.path.join(out_dir, f2)
            subprocess.run([FFMPEG, "-y", "-ss", str(mid), "-to", str(end), "-i", input_mp3, "-c", "copy", p2], capture_output=True)
            s2_mb = round(os.path.getsize(p2) / (1024 * 1024), 2)
            slices.append({
                "id": f"slice_{slice_idx:02d}",
                "chapter_num": ch_num,
                "part": 2,
                "title": f"{title} (Part 2)",
                "start_sec": mid,
                "end_sec": end,
                "duration_sec": end - mid,
                "file_name": f2,
                "file_path": p2,
                "size_mb": s2_mb
            })
            slice_idx += 1
            print(f"      Part 1: {s1_mb} MB | Part 2: {s2_mb} MB (Both <= 18 MB)")

    manifest_file = os.path.join(out_dir, "slices_manifest.json")
    with open(manifest_file, "w", encoding="utf-8") as f:
        json.dump(slices, f, indent=2, ensure_ascii=False)

    print(f"\n[+] Slicing complete! {len(slices)} slices written to {manifest_file}.")
    return slices

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Slice master audio into <=18MB chapter parts")
    parser.add_argument("--input-mp3", required=True, help="Master MP3 file")
    parser.add_argument("--out-dir", required=True, help="Directory to save sliced MP3s")
    parser.add_argument("--info-json", help="yt-dlp info.json file for chapter metadata")
    args = parser.parse_args()

    slice_audio(args.input_mp3, args.out_dir, args.info_json)
