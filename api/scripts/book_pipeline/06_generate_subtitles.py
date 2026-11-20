"""
Step 6: Generate Synchronized Chapter Subtitles JSON for SPARK Reader
Parses WebVTT or YouTube transcript and groups text into natural sentences
aligned to each chapter's start offset (starting from 0 ms).

Usage:
  python 06_generate_subtitles.py --vtt-file "transcript.vtt" --slices-manifest "slices_manifest.json" --slug "the-art-of-war" [--out-dir "api/public/subtitles"]
"""

import argparse
import os
import sys
import json
import re

DEFAULT_OUT_DIR = r"D:\new something\api\public\subtitles"

def parse_vtt_timestamp(ts_str: str) -> float:
    ts_str = ts_str.strip()
    parts = ts_str.split(":")
    if len(parts) == 3:
        h, m, s = parts
        return float(h) * 3600 + float(m) * 60 + float(s)
    elif len(parts) == 2:
        m, s = parts
        return float(m) * 60 + float(s)
    return float(ts_str)

def parse_vtt(vtt_path: str):
    snippets = []
    if not os.path.exists(vtt_path):
        return snippets

    with open(vtt_path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()

    # Match 00:00:01.000 --> 00:00:04.000
    pattern = re.compile(
        r"(\d{1,2}:?\d{2}:\d{2}[\.,]\d{3})\s*-->\s*(\d{1,2}:?\d{2}:\d{2}[\.,]\d{3})[^\n]*\n([\s\S]*?)(?=\n\s*\n|\n\d|\Z)",
        re.MULTILINE
    )

    for m in pattern.finditer(content):
        start_sec = parse_vtt_timestamp(m.group(1).replace(",", "."))
        end_sec = parse_vtt_timestamp(m.group(2).replace(",", "."))
        raw_text = m.group(3)
        # Strip WebVTT formatting tags e.g. <c>, </c>, <00:00:01.000>
        clean_text = re.sub(r"<[^>]+>", "", raw_text)
        clean_text = " ".join(clean_text.split()).strip()
        if clean_text and clean_text != "[Music]":
            snippets.append({
                "start": start_sec,
                "duration": max(0.2, end_sec - start_sec),
                "text": clean_text
            })

    # Sort and deduplicate consecutive identical snippets
    deduped = []
    for s in snippets:
        if not deduped or deduped[-1]["text"] != s["text"]:
            deduped.append(s)
    return deduped

def group_into_sentences(snippets_for_ch, ch_start_sec):
    words_timed = []
    for s in snippets_for_ch:
        text = s["text"].replace("\n", " ").strip()
        if not text or text == "[Music]":
            continue
        words = text.split()
        if not words:
            continue

        start_ms = max(0, round((s["start"] - ch_start_sec) * 1000))
        end_ms = max(start_ms + 400, round((s["start"] + s["duration"] - ch_start_sec) * 1000))
        dur_per_word = (end_ms - start_ms) / len(words)

        for idx, w in enumerate(words):
            w_start = round(start_ms + idx * dur_per_word)
            w_end = round(start_ms + (idx + 1) * dur_per_word)
            words_timed.append((w, w_start, w_end))

    sentences = []
    current_sentence = []

    for i, (word, w_start, w_end) in enumerate(words_timed):
        current_sentence.append((word, w_start, w_end))

        is_end_punct = any(word.endswith(p) for p in [".", "?", "!"])
        is_clause_length = len(current_sentence) >= 16
        is_last = i == len(words_timed) - 1

        if (is_end_punct or is_clause_length or is_last) and len(current_sentence) >= 6:
            stext = " ".join(w for w, _, _ in current_sentence).strip()
            if stext:
                stext = stext[0].upper() + stext[1:]
                if stext[-1] not in [".", "?", "!", ":", ","]:
                    stext += "."
                sentences.append({
                    "startMs": current_sentence[0][1],
                    "endMs": current_sentence[-1][2],
                    "text": stext
                })
            current_sentence = []

    if current_sentence:
        stext = " ".join(w for w, _, _ in current_sentence).strip()
        if sentences and len(current_sentence) < 6:
            prev = sentences[-1]
            prev["text"] = prev["text"].rstrip(".") + " " + stext
            if prev["text"][-1] not in [".", "!", "?"]:
                prev["text"] += "."
            prev["endMs"] = current_sentence[-1][2]
        else:
            if stext:
                stext = stext[0].upper() + stext[1:]
                if stext[-1] not in [".", "?", "!", ":", ","]:
                    stext += "."
                sentences.append({
                    "startMs": current_sentence[0][1],
                    "endMs": current_sentence[-1][2],
                    "text": stext
                })

    return sentences

def generate_subtitles(vtt_file: str, slices_manifest: str, slug: str, out_dir: str = DEFAULT_OUT_DIR, video_id: str = None) -> int:
    os.makedirs(out_dir, exist_ok=True)

    snippets = []
    if vtt_file and os.path.exists(vtt_file):
        print(f"[*] Parsing WebVTT transcript: {vtt_file}...")
        snippets = parse_vtt(vtt_file)

    if not snippets and video_id:
        print(f"[*] Fetching transcript via youtube_transcript_api for {video_id}...")
        try:
            from youtube_transcript_api import YouTubeTranscriptApi
            ytt = YouTubeTranscriptApi()
            raw = list(ytt.list(video_id).find_transcript(["en", "en-US", "en-orig"]).fetch())
            snippets = [{"start": s.start, "duration": s.duration, "text": s.text} for s in raw]
        except Exception as e:
            print(f"[!] Warning: youtube_transcript_api error: {e}")

    if not snippets:
        print("[!] No transcript snippets available to generate subtitles.")
        return 0

    with open(slices_manifest, "r", encoding="utf-8") as f:
        slices = json.load(f)

    # Group slices by chapter_num
    chapters_map = {}
    for s in slices:
        c_num = s.get("chapter_num", 0)
        if c_num not in chapters_map:
            chapters_map[c_num] = {
                "chapter_num": c_num,
                "title": s.get("title", f"Chapter {c_num}"),
                "start_sec": s.get("start_sec", 0),
                "end_sec": s.get("end_sec", 0)
            }
        else:
            # Multi-part chapter: expand end_sec
            chapters_map[c_num]["end_sec"] = max(chapters_map[c_num]["end_sec"], s.get("end_sec", 0))

    chapters = sorted(chapters_map.values(), key=lambda x: x["chapter_num"])
    total_sentences = 0

    for ch in chapters:
        c_num = ch["chapter_num"]
        start_sec = ch["start_sec"]
        end_sec = ch["end_sec"]

        ch_snippets = [
            s for s in snippets
            if s["start"] >= (start_sec - 0.5) and s["start"] < (end_sec - 0.2)
        ]

        sentences = group_into_sentences(ch_snippets, start_sec)
        key = f"{slug}_ch_{c_num:02d}"
        out_file = os.path.join(out_dir, f"{key}.json")
        with open(out_file, "w", encoding="utf-8") as f:
            json.dump(sentences, f, indent=2, ensure_ascii=False)

        print(f"  [+] Saved {len(sentences):>3} sentences for [{key}] ({ch['title']})")
        total_sentences += len(sentences)

    print(f"[+] Total synchronized sentences generated: {total_sentences} across {len(chapters)} chapters.")
    return total_sentences

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate synchronized chapter subtitles")
    parser.add_argument("--vtt-file", help="Path to WebVTT transcript file")
    parser.add_argument("--slices-manifest", required=True, help="Path to slices_manifest.json")
    parser.add_argument("--slug", required=True, help="Book slug (e.g. the-art-of-war)")
    parser.add_argument("--video-id", help="YouTube video ID if fallback needed")
    parser.add_argument("--out-dir", default=DEFAULT_OUT_DIR, help="Destination directory for subtitle JSONs")
    args = parser.parse_args()

    generate_subtitles(args.vtt_file, args.slices_manifest, args.slug, args.out_dir, args.video_id)
