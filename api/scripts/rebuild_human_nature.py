import os
import re
import json
import subprocess

FFMPEG = r"C:\Users\Lenovo\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe"
MASTER_MP3 = r"D:\books\the-laws-of-human-nature\master_audio.mp3"
VTT_FILE = r"D:\books\the-laws-of-human-nature\transcript.en.vtt"
AUDIO_OUT_DIR = r"D:\new something\api\public\audio"
SUB_OUT_DIR = r"D:\new something\api\public\subtitles"

os.makedirs(AUDIO_OUT_DIR, exist_ok=True)
os.makedirs(SUB_OUT_DIR, exist_ok=True)

# Precision clean chapter boundaries (verified from transcript & audio speech waveforms)
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
    {"num": 18, "title": "Chapter 18: The Law of Death Denial", "start": 10509.96, "end": 11022.54},
]

def to_sec(t_str):
    parts = t_str.split(":")
    return float(parts[0]) * 3600 + float(parts[1]) * 60 + float(parts[2])

print("1. Parsing master VTT transcript...")
with open(VTT_FILE, "r", encoding="utf-8") as f:
    vtt_text = f.read()

# Extract word tokens with timestamps
lines = vtt_text.split("\n")
raw_cues = []
for i, line in enumerate(lines):
    m = re.match(r"(\d{2}:\d{2}:\d{2}\.\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2}\.\d{3})", line)
    if m:
        t_start = to_sec(m.group(1))
        t_end = to_sec(m.group(2))
        next_line = lines[i+1] if i + 1 < len(lines) else ""
        clean_text = re.sub(r"<[^>]+>", "", next_line).strip()
        if clean_text and clean_text != "[Music]":
            raw_cues.append((t_start, t_end, clean_text))

print(f"Loaded {len(raw_cues)} raw cues from VTT.")

print("\n2. Processing audio slices & precision subtitles for all 19 chapters...")

for ch in CHAPTERS:
    c_num = ch["num"]
    pad = f"{c_num:02d}"
    start_s = ch["start"]
    end_s = ch["end"]
    dur_s = end_s - start_s
    title = ch["title"]

    audio_file = os.path.join(AUDIO_OUT_DIR, f"human_nature_ch_{pad}.mp3")
    print(f"\n--- Chapter {pad}: {title} ({dur_s:.2f}s) ---")

    # Slice clean audio without cutoffs
    # Use re-encoding to 128k mp3 with exact timestamps to avoid copy keyframe misalignment
    cmd = [
        FFMPEG, "-y",
        "-ss", f"{start_s:.3f}",
        "-to", f"{end_s:.3f}",
        "-i", MASTER_MP3,
        "-c:a", "libmp3lame",
        "-b:a", "128k",
        audio_file
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    size_mb = os.path.getsize(audio_file) / (1024 * 1024)
    print(f"  [Audio] Sliced clean mp3: {size_mb:.2f} MB")

    # Extract cues within [start_s, end_s]
    ch_raw = [c for c in raw_cues if (c[0] >= start_s - 0.2 and c[0] < end_s)]

    # Group words with relative timestamps
    words_timed = []
    for t_s, t_e, txt in ch_raw:
        words = txt.split()
        if not words:
            continue
        rel_start = max(0, round((t_s - start_s) * 1000))
        rel_end = max(rel_start + 400, round((t_e - start_s) * 1000))
        dur_w = (rel_end - rel_start) / len(words)
        for idx, w in enumerate(words):
            w_start = round(rel_start + idx * dur_w)
            w_end = round(rel_start + (idx + 1) * dur_w)
            words_timed.append((w, w_start, w_end))

    # Clean out stray leading outro words if any bled from boundary
    # e.g. "Episode X now ends", "forward", "orchestrating it", "clear", etc.
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
                print(f"  [Clean] Removed stray leading fragment: {' '.join(prefix)}")
                words_timed = words_timed[p_len:]
                break

    # Group into complete grammatical sentences ending in . ! ?
    sentences = []
    curr_sentence = []

    for i, (word, w_start, w_end) in enumerate(words_timed):
        curr_sentence.append((word, w_start, w_end))

        is_end_punct = any(word.endswith(p) for p in [".", "!", "?"])
        is_too_long = len(curr_sentence) >= 22 # Only split very long sentences
        is_last = (i == len(words_timed) - 1)

        if (is_end_punct or is_too_long or is_last) and len(curr_sentence) >= 5:
            stext = " ".join(w for w, _, _ in curr_sentence).strip()
            if stext:
                stext = stext[0].upper() + stext[1:]
                if stext[-1] not in [".", "!", "?", ":", ","]:
                    stext += "."
                sentences.append({
                    "startMs": curr_sentence[0][1],
                    "endMs": curr_sentence[-1][2],
                    "text": stext
                })
            curr_sentence = []

    if curr_sentence:
        # Merge leftover into previous sentence if too short (< 5 words) to avoid cutoffs
        stext = " ".join(w for w, _, _ in curr_sentence).strip()
        if sentences and len(curr_sentence) < 6:
            prev = sentences[-1]
            prev["text"] = prev["text"].rstrip(".") + " " + stext
            if prev["text"][-1] not in [".", "!", "?"]:
                prev["text"] += "."
            prev["endMs"] = curr_sentence[-1][2]
        else:
            if stext:
                stext = stext[0].upper() + stext[1:]
                if stext[-1] not in [".", "!", "?"]:
                    stext += "."
                sentences.append({
                    "startMs": curr_sentence[0][1],
                    "endMs": curr_sentence[-1][2],
                    "text": stext
                })

    # Contiguous & monotonic
    sentences.sort(key=lambda x: x["startMs"])
    for i in range(len(sentences)):
        if i + 1 < len(sentences):
            next_start = sentences[i+1]["startMs"]
            if sentences[i]["endMs"] > next_start or (next_start - sentences[i]["endMs"] <= 1200):
                sentences[i]["endMs"] = next_start
        else:
            sentences[i]["endMs"] = max(sentences[i]["endMs"], sentences[i]["startMs"] + 2000)

    sub_file = os.path.join(SUB_OUT_DIR, f"human_nature_ch_{pad}.json")
    with open(sub_file, "w", encoding="utf-8") as f:
        json.dump(sentences, f, indent=2, ensure_ascii=False)

    print(f"  [Subtitles] Generated {len(sentences)} complete sentences.")
    print(f"    FIRST: \"{sentences[0]['text'][:65]}\"")
    print(f"    LAST:  \"{sentences[-1]['text'][-65:]}\"")

print("\nAll 19 chapters of The Laws of Human Nature rebuilt with pristine boundaries!")
