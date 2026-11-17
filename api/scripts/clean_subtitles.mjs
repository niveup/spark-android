import fs from 'fs';
import path from 'path';

const SUBTITLES_DIR = path.resolve('public/subtitles');

function cleanAndGroupSentences(cues) {
  const result = [];
  let currentWords = [];

  for (const cue of cues) {
    const text = cue.text.replace(/[\r\n]+/g, ' ').trim();
    const words = text.split(/\s+/).filter(Boolean);
    if (!words.length) continue;
    const duration = Math.max(cue.endMs - cue.startMs, 500);
    const perWord = duration / words.length;

    words.forEach((w, i) => {
      currentWords.push({
        word: w,
        startMs: Math.round(cue.startMs + i * perWord),
        endMs: Math.round(cue.startMs + (i + 1) * perWord),
      });
    });
  }

  // Filter out stray 1-2 word fragments at the very beginning if they don't form a sentence (e.g. "entire life.")
  if (currentWords.length > 2 && currentWords[0].word.toLowerCase() === 'entire' && currentWords[1].word.toLowerCase().startsWith('life')) {
    currentWords.splice(0, 2);
  }

  let sentenceWords = [];

  for (let i = 0; i < currentWords.length; i++) {
    const item = currentWords[i];
    sentenceWords.push(item);

    const isPunctuationEnd = /[.!?]$/.test(item.word);
    const isTooLong = sentenceWords.length >= 18;
    const isLast = i === currentWords.length - 1;

    if ((isPunctuationEnd || isTooLong || isLast) && sentenceWords.length > 0) {
      const sentenceText = sentenceWords.map(w => w.word).join(' ').trim();
      const startMs = sentenceWords[0].startMs;
      const endMs = sentenceWords[sentenceWords.length - 1].endMs;
      if (sentenceText.length > 1) {
        result.push({ startMs, endMs, text: sentenceText });
      }
      sentenceWords = [];
    }
  }

  return result;
}

const files = fs.readdirSync(SUBTITLES_DIR).filter(f => f.endsWith('.json'));
console.log(`Processing ${files.length} subtitle files...`);

for (const file of files) {
  const filePath = path.join(SUBTITLES_DIR, file);
  try {
    const content = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    if (!Array.isArray(content) || content.length === 0) continue;

    // Only process files with very short cue fragments (< 8 words on average)
    const avgWords = content.reduce((sum, c) => sum + (c.text ? c.text.split(/\s+/).length : 0), 0) / content.length;
    if (avgWords < 12) {
      const cleaned = cleanAndGroupSentences(content);
      fs.writeFileSync(filePath, JSON.stringify(cleaned, null, 2), 'utf-8');
      console.log(`Cleaned ${file}: ${content.length} fragments -> ${cleaned.length} complete sentences.`);
    }
  } catch (err) {
    console.error(`Error processing ${file}:`, err);
  }
}

console.log('All subtitle files processed.');
