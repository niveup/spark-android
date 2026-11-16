# AI content inbox — the exact JSON your AI must produce

This is the whole loop, no code needed after setup:

```
you (or a chat AI)  →  cards.json  →  node prisma/import-cards.ts  →  Supabase  →  phone
```

The importer is **`api/prisma/import-cards.ts`**. The inbox file is
**`api/prisma/inbox/cards.json`** (start from `cards.example.json`).

---

## 1. The format (this is all the AI has to satisfy)

```json
{
  "cards": [
    {
      "type": "quote",
      "person": "Epictetus",
      "work": "Enchiridion of Epictetus",
      "chapter": "V",
      "gutenbergId": 45109,
      "match": "Men are disturbed not by things",
      "action": "Try this today: name the events of one argument and the opinion you added to it.",
      "topics": ["mindset", "focus"]
    },
    {
      "type": "concept",
      "title": "The cost of an unfinished decision",
      "body": "An open decision keeps charging rent …",
      "action": "Try this today: close one decision you have re-opened three times.",
      "topics": ["focus", "time"]
    }
  ]
}
```

### Fields

| field | required | rule |
|---|---|---|
| `type` | yes | `quote`, `concept`, `challenge`, `book_idea`, `summary` |
| `title` | except quotes | ≤ 80 characters, a claim not a label |
| `body` | except quotes | ≤ 600 characters, one idea, plain language, no emoji |
| `action` | yes | ≤ 200 characters, starts with `Try this today:` or `Do it now:` |
| `topics` | yes | 1–5 of `habits, discipline, mindset, money, focus, communication, time, health, learning` |
| `person`, `work`, `chapter` | quotes | who and where the line comes from |
| `gutenbergId` | quotes | the Project Gutenberg **number** of that book |
| `match` | quotes | the first 8–12 words of the sentence, verbatim |

### What the importer does with it

- **`type: "quote"`** → downloads that Gutenberg book, finds the sentence that
  starts with `match`, and stores **that** sentence. Your AI's wording is never
  trusted: if the sentence is not in the book, the card is **rejected** as
  invented or misquoted. Verified quotes are marked grounded (no "unverified"
  chip) and attributed for real.
- **every other type** → stored exactly as written, marked
  `isAiGenerated = true, isGrounded = false`. The app shows the
  **"AI-written, unverified"** chip, because no source backs it. No author name
  is accepted on these cards — that's the line that keeps the app honest.

---

## 2. The prompt to give your AI (copy everything in the box)

```text
You are filling a JSON inbox for SPARK, a personal reading app. Output ONLY
valid JSON in the exact shape below — no markdown fences, no commentary.

{
  "cards": [
    { "type": "quote", "person": "...", "work": "...", "chapter": "...",
      "gutenbergId": 0, "match": "...", "action": "...", "topics": ["..."] },
    { "type": "concept", "title": "...", "body": "...", "action": "...",
      "topics": ["..."] }
  ]
}

RULES
1. `type` is one of: quote, concept, challenge, book_idea, summary.
2. `title` <= 80 chars, `body` <= 600 chars, `action` <= 200 chars.
3. `topics` is 1-5 of: habits, discipline, mindset, money, focus,
   communication, time, health, learning.
4. `action` starts with "Try this today:" or "Do it now:" and is doable today.
5. QUOTES: only quote authors whose books are on Project Gutenberg
   (public domain — typically people who died before ~1930, e.g. Epictetus,
   Marcus Aurelius, Seneca, Benjamin Franklin, Emerson, Thoreau, James Allen,
   Arnold Bennett, Samuel Smiles, Russell Conwell, Wallace Wattles).
   For each one give: the Gutenberg book number, and `match` = the first
   8-12 words of the sentence EXACTLY as printed in that book.
   If you are not certain the words are verbatim in that text, do not
   include the quote.
6. Do NOT quote living or recent figures (Jobs, Naval, Peterson, Huberman …):
   their books are copyrighted and cannot be verified. Their ideas may appear
   only as `concept` / `book_idea` cards written in your own words, with no
   claim that they said it.
7. Never invent statistics, studies, or book claims. One idea per card.
8. Every card must be distinct: different angle, different opening words.
   Avoid these existing titles: <paste your current titles here>.

Write N cards about: <paste topics here>.
```

Find a book's Gutenberg number from its page URL: `gutenberg.org/ebooks/45109` → `45109`.

---

## 3. Run it

```powershell
cd api
node prisma/import-cards.ts                                   # publishes every valid card
node prisma/import-cards.ts prisma/inbox/cards.json --draft    # hold as drafts instead
node prisma/import-cards.ts --dry-run                         # test: full run, rolled back
```

Output looks like this:

```
cards.json: 3 card(s) — mode published

  ACCEPTED  verified quote Epictetus → published
  ACCEPTED  original       The cost of an unfinished decision → published
  REJECTED  Steve Jobs
            quote without a gutenbergId cannot be verified — add the Project Gutenberg id

summary: 2 stored, 1 rejected
```

Then open Reels on the phone (a fresh feed load) and they are there. Rejected
cards are never stored — fix the JSON and run again. Re-running is safe:
identical cards are skipped as duplicates.

---

## 4. Why the quote gate exists

An AI remembers *the gist* of a sentence, not the sentence. Asked for a quote,
it produces something plausible and attributes it to a real person. Ten of
those and the app teaches you things that were never written.

So the pipeline splits the job:

- the AI does the part it is genuinely good at — **finding** interesting lines
  and writing original cards;
- the machine does the part an AI is bad at — **checking** the words against
  the actual book.

A rejected quote is not lost work; it is the gate doing its job.
