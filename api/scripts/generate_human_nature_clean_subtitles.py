import os
import json
import re
from youtube_transcript_api import YouTubeTranscriptApi

OUT_DIR = r"D:\new something\api\public\subtitles"
VIDEO_ID = "CgFFvjoiwzo"

os.makedirs(OUT_DIR, exist_ok=True)

CHAPTERS = [
    {"num": 0, "title": "Introduction", "start": 0.0, "end": 60.84},
    {"num": 1, "title": "Chapter 1: The Law of Irrationality", "start": 60.84, "end": 1388.24},
    {"num": 2, "title": "Chapter 2: The Law of Narcissism", "start": 1388.24, "end": 2174.84},
    {"num": 3, "title": "Chapter 3: The Law of Role-Playing", "start": 2174.84, "end": 2886.84},
    {"num": 4, "title": "Chapter 4: The Law of Compulsive Behavior", "start": 2886.84, "end": 3440.16},
    {"num": 5, "title": "Chapter 5: The Law of Covetousness", "start": 3440.16, "end": 3976.64},
    {"num": 6, "title": "Chapter 6: The Law of Shortsightedness", "start": 3976.64, "end": 4314.72},
    {"num": 7, "title": "Chapter 7: The Law of Defensive Behavior", "start": 4314.72, "end": 4898.72},
    {"num": 8, "title": "Chapter 8: The Law of Self-Sabotage", "start": 4898.72, "end": 5492.88},
    {"num": 9, "title": "Chapter 9: The Law of Repression", "start": 5492.88, "end": 6069.40},
    {"num": 10, "title": "Chapter 10: The Law of Envy", "start": 6069.40, "end": 6655.20},
    {"num": 11, "title": "Chapter 11: The Law of Grandiosity", "start": 6655.20, "end": 7212.04},
    {"num": 12, "title": "Chapter 12: The Law of Gender Rigidity", "start": 7212.04, "end": 7757.80},
    {"num": 13, "title": "Chapter 13: The Law of Aimlessness", "start": 7757.80, "end": 8241.36},
    {"num": 14, "title": "Chapter 14: The Law of Conformity", "start": 8241.36, "end": 8768.44},
    {"num": 15, "title": "Chapter 15: The Law of Fickleness", "start": 8768.44, "end": 9403.04},
    {"num": 16, "title": "Chapter 16: The Law of Aggression", "start": 9403.04, "end": 10023.84},
    {"num": 17, "title": "Chapter 17: The Law of Generational Myopia", "start": 10023.84, "end": 10509.96},
    {"num": 18, "title": "Chapter 18: The Law of Death Denial", "start": 10509.96, "end": 11024.28},
]

print(f"Fetching clean YouTube transcript for video {VIDEO_ID}...")
ytt = YouTubeTranscriptApi()
transcript = list(ytt.list(VIDEO_ID).find_transcript(["en", "en-US", "en-orig"]).fetch())
print(f"Fetched {len(transcript)} clean snippets.")

def clean_trailing_announcement(text):
    text = re.sub(r"\s*(Welcome to the complete journey\s*Chapter\.?)$", "", text, flags=re.I)
    text = re.sub(r"\s*(Episode\s+\w+\s+now\s+ends\.?.*)$", "", text, flags=re.I)
    text = re.sub(r"\s*(Chapter\s+(\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen):?.*)$", "", text, flags=re.I)
    text = text.strip()
    if text and text[-1] not in [".", "!", "?"]:
        text += "."
    return text

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

    # Clean out stray leading outro words if any bled from boundary
    stray_prefixes = [
        ["episode", "one", "now", "ends"],
        ["episode", "two", "now", "ends"],
        ["episode", "three", "now", "ends"],
        ["episode", "four", "now", "ends"],
        ["episode", "1", "now", "ends"],
        ["episode", "2", "now", "ends"],
        ["episode", "3", "now", "ends"],
        ["episode", "4", "now", "ends"],
        ["forward"],
        ["orchestrating", "it"],
        ["journey", "toward", "true", "mastery"],
        ["clear"],
        ["and", "undeniable", "success"],
        ["crowd"],
        ["expect"],
        ["emerge"],
        ["entire", "life"],
        ["for", "living", "with", "purpose"],
    ]

    for prefix in stray_prefixes:
        p_len = len(prefix)
        if len(words_timed) > p_len:
            match = True
            for k in range(p_len):
                clean_w = re.sub(r"[^\w]", "", words_timed[k][0].lower())
                if clean_w != prefix[k]:
                    match = False
                    break
            if match:
                words_timed = words_timed[p_len:]
                break

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

    # Clean trailing announcement from the final sentence
    if sentences:
        cleaned_last = clean_trailing_announcement(sentences[-1]["text"])
        if len(cleaned_last) > 3:
            sentences[-1]["text"] = cleaned_last
        elif len(sentences) > 1:
            sentences.pop()

    # Enforce strictly contiguous and non-overlapping cue boundaries
    sentences.sort(key=lambda x: x["startMs"])
    for i in range(len(sentences)):
        if i + 1 < len(sentences):
            next_start = sentences[i+1]["startMs"]
            if sentences[i]["endMs"] > next_start or (next_start - sentences[i]["endMs"] <= 1200):
                sentences[i]["endMs"] = next_start
        else:
            sentences[i]["endMs"] = max(sentences[i]["endMs"], sentences[i]["startMs"] + 2000)

    return sentences

print("Processing all 19 chapters of The Laws of Human Nature...")
for ch in CHAPTERS:
    c_num = ch["num"]
    pad = f"{c_num:02d}"
    start_sec = ch["start"]
    end_sec = ch["end"]
    title = ch["title"]

    ch_snippets = [s for s in transcript if start_sec <= s.start < end_sec]
    sentences = group_into_sentences(ch_snippets, start_sec)

    out_file = os.path.join(OUT_DIR, f"human_nature_ch_{pad}.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(sentences, f, indent=2, ensure_ascii=False)

    print(f"Ch {pad} ({title}): {len(sentences)} sentences.")
    print(f"   START: \"{sentences[0]['text'][:60]}\"")
    print(f"   END:   \"{sentences[-1]['text'][-60:]}\"")

print("\nAll 19 chapters of The Laws of Human Nature regenerated with zero cutoffs!")
