import os
import json
import re
from youtube_transcript_api import YouTubeTranscriptApi

OUT_DIR = r"D:\new something\api\public\subtitles"
VIDEO_ID = "4Lz3wSrTn5o"
CHAPTERS_FILE = r"D:\books\the-art-of-war\chapters.json"

os.makedirs(OUT_DIR, exist_ok=True)

with open(CHAPTERS_FILE, "r", encoding="utf-8") as f:
    chapters = json.load(f)

print(f"Loaded {len(chapters)} chapters.")
print(f"Fetching clean YouTube transcript for video {VIDEO_ID}...")
ytt = YouTubeTranscriptApi()
transcript = list(ytt.list(VIDEO_ID).find_transcript(["en", "en-US", "en-orig"]).fetch())
print(f"Fetched {len(transcript)} clean snippets.")

def group_into_sentences(snippets_for_ch, ch_start_sec):
    words_timed = []
    for s in snippets_for_ch:
        text = s.text.replace("\n", " ").strip()
        if not text or text == "[Music]":
            continue
        words = text.split()
        if not words:
            continue
        
        start_ms = max(0, round((s.start - ch_start_sec) * 1000))
        end_ms = max(start_ms + 400, round((s.start + s.duration - ch_start_sec) * 1000))
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

total_generated = 0
for idx, ch in enumerate(chapters):
    ch_num = idx
    start_sec = ch["start_time"]
    end_sec = ch["end_time"]
    
    # Filter snippets within [start_sec, end_sec)
    ch_snippets = [
        s for s in transcript 
        if s.start >= (start_sec - 0.5) and s.start < (end_sec - 0.2)
    ]

    sentences = group_into_sentences(ch_snippets, start_sec)
    
    key = f"the-art-of-war_ch_{ch_num:02d}"
    out_file = os.path.join(OUT_DIR, f"{key}.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(sentences, f, indent=2, ensure_ascii=False)
        
    print(f"  [+] Saved {len(sentences):>3} sentences for [{key}] ({ch['title']})")
    total_generated += len(sentences)

print(f"\n[+] SUCCESS! Generated subtitles for all {len(chapters)} chapters ({total_generated} sentences total).")
