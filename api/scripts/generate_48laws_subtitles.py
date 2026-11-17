import os
import json
import re
from youtube_transcript_api import YouTubeTranscriptApi

CHAPTERS_FILE = r"D:\books\48-laws-of-power\all_48_laws_chapters.json"
OUT_DIR = r"D:\new something\api\public\subtitles"
VIDEO_ID = "2P1-SunHlZg"
LAG_COMPENSATION_MS = 1800 # Compensates for YouTube ASR token emission latency

os.makedirs(OUT_DIR, exist_ok=True)

with open(CHAPTERS_FILE, "r", encoding="utf-8") as f:
    chapters = json.load(f)

print(f"Fetching clean YouTube transcript for video {VIDEO_ID}...")
ytt = YouTubeTranscriptApi()
transcript_list = ytt.list(VIDEO_ID)
transcript = transcript_list.find_transcript(["en", "en-US", "en-orig"])
snippets = transcript.fetch()
print(f"Fetched {len(snippets)} snippets.")

def group_into_sentences(snippets_for_ch, ch_start_sec):
    words_timed = []
    for s in snippets_for_ch:
        text = s.text.replace("\n", " ").strip()
        if not text or text == "[Music]":
            continue
        words = text.split()
        if not words:
            continue
        
        # Compensate for YouTube auto-caption lag
        raw_start = (s.start - ch_start_sec) * 1000
        raw_end = (s.start + s.duration - ch_start_sec) * 1000
        start_ms = max(0, round(raw_start - LAG_COMPENSATION_MS))
        end_ms = max(start_ms + 400, round(raw_end - LAG_COMPENSATION_MS))
        dur_per_word = (end_ms - start_ms) / len(words)

        for idx, w in enumerate(words):
            w_start = round(start_ms + idx * dur_per_word)
            w_end = round(start_ms + (idx + 1) * dur_per_word)
            words_timed.append((w, w_start, w_end))

    # Clean out stray leading artifacts (like "time law", "preface")
    while words_timed and words_timed[0][0].lower() in ["time", "c", "/c"]:
        words_timed.pop(0)

    sentences = []
    current_sentence = []

    for i, (word, w_start, w_end) in enumerate(words_timed):
        current_sentence.append((word, w_start, w_end))
        
        is_end_punct = any(word.endswith(p) for p in [".", "?", "!", ":"])
        is_clause_length = len(current_sentence) >= 12
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
        if stext:
            stext = stext[0].upper() + stext[1:]
            if stext[-1] not in [".", "?", "!", ":", ","]:
                stext += "."
            sentences.append({
                "startMs": current_sentence[0][1],
                "endMs": current_sentence[-1][2],
                "text": stext
            })

    # Enforce strictly contiguous and non-overlapping cue boundaries
    sentences.sort(key=lambda x: x["startMs"])
    for i in range(len(sentences)):
        if i + 1 < len(sentences):
            next_start = sentences[i+1]["startMs"]
            if sentences[i]["endMs"] > next_start or (next_start - sentences[i]["endMs"] <= 1200):
                sentences[i]["endMs"] = next_start
        else:
            sentences[i]["endMs"] = max(sentences[i]["endMs"], sentences[i]["startMs"] + 1500)

    return sentences

print("Processing all 49 chapters...")
for ch in chapters:
    c_num = ch["chapter_num"]
    start_sec = ch["start_sec"]
    end_sec = ch["end_sec"]
    title = ch["title"]

    ch_snippets = [s for s in snippets if start_sec <= s.start < end_sec]
    sentences = group_into_sentences(ch_snippets, start_sec)

    out_file = os.path.join(OUT_DIR, f"48laws_chapter_{c_num:02d}.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(sentences, f, indent=2, ensure_ascii=False)

    print(f"Chapter {c_num:02d} ({title}): {len(ch_snippets)} snippets -> {len(sentences)} full sentences.")

print("All 49 chapters generated with precision timing and zero overlap!")
