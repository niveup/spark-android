// Phase 3b tests: growth builders + Bartlett's parser + estSeconds rules.
// Pure logic only (no network, no DB) so CI-safe. Fixture mimics the real
// PG #27889 plaintext structure verified live in Oct 2026.
import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { CardSchema } from "../src/lib/cards";
import { parseBartlettQuotes } from "../src/lib/bartlett";
import {
  chapterCandidates,
  conceptCandidates,
  openingExcerpt,
  quoteCandidates,
} from "../src/lib/growth";
import { BASE_CARD } from "./fixtures";

const FIXTURE = `
JUNK INDEX TABLE
WOLFE, CHARLES                                563

MARIA QUOTATIONHOUSE. 1899-1902.

    Unrelated author to keep parsing honest.

_His Works._

MARCUS AURELIUS ANTONINUS. 121-180.

    The universe is change; our life is what our thoughts make it.

_Meditations. iv._

    Though your light smoulder in its socket, keep the flame alive.

_Meditations. v._

SENECA.

    We suffer more often in imagination than in reality.

_Letters. 5._

    A rx mis-OCR line with <junk> guard should fail length/layout checks.

_Letters. 6._
`;

describe("parseBartlettQuotes", () => {
  it("extracts only target authors with attribution, in order, capped", () => {
    const quotes = parseBartlettQuotes(
      FIXTURE,
      ["AURELIUS", "SENECA"],
      3,
    );
    assert.equal(quotes.length, 3);
    assert.ok(quotes.every((q) => q.work.length > 0));
    assert.equal(quotes[0]?.header, "MARCUS AURELIUS ANTONINUS");
    assert.ok((quotes[0]?.quote ?? "").startsWith("The universe is change"));
    assert.equal(quotes[2]?.header, "SENECA");
    assert.ok((quotes[2]?.quote ?? "").startsWith("We suffer more often"));
  });

  it("respects capPerAuthor", () => {
    const quotes = parseBartlettQuotes(FIXTURE, ["AURELIUS", "SENECA"], 1);
    assert.equal(quotes.length, 2);
    assert.ok(quotes.every((q) => !q.quote.includes("light smoulder")));
  });
});

describe("growth builders", () => {
  const entry = {
    slug: "as-a-man-thinketh",
    title: "As a Man Thinketh",
    author: "James Allen",
    year: 1903,
    topicTags: ["mindset"],
    actionIdea: "Try this today: audit your loop.",
    gutenbergId: 4507,
    librivoxId: "1968",
  };

  it("chapter candidates stream URLs and cap sections, audio estSeconds allowed >600", () => {
    const lvx = {
      librivoxId: "1968",
      title: "As a Man Thinketh",
      description: null,
      author: "James Allen",
      gutenbergId: 4507,
      pageUrl: "https://librivox.org/as-a-man-thinketh/",
      zipUrl: null,
      totalSeconds: 3247,
      numSections: 8,
      sections: [
        { sectionNumber: 0, title: "Foreword", listenUrl: "https://archive.org/dl/a.mp3", playtimeSec: 68 },
        { sectionNumber: 1, title: "Thought and Character", listenUrl: "https://archive.org/dl/b.mp3", playtimeSec: 311 },
        { sectionNumber: 2, title: "Effect", listenUrl: "https://archive.org/dl/c.mp3", playtimeSec: 1153 },
      ],
    };
    const cards = chapterCandidates(entry, lvx, 2);
    assert.equal(cards.length, 2);
    const first = cards[0]?.card as { audioUrl: string; estSeconds: number; sourceRef: string };
    assert.equal(first.audioUrl, "https://archive.org/dl/a.mp3");
    assert.equal(first.estSeconds, 68);
    assert.ok(CardSchema.safeParse(cards[0]?.card).success);
    assert.ok(CardSchema.safeParse(cards[1]?.card).success);
    const long = chapterCandidates(entry, lvx, 3)[2]?.card as { estSeconds: number };
    assert.equal(long.estSeconds, 1153);
    assert.ok(CardSchema.safeParse(long).success);
  });

  it("quote candidates keep attribution in sourceRef and pass schema", () => {
    const quotes = parseBartlettQuotes(FIXTURE, ["AURELIUS"], 3);
    const cards = quoteCandidates(
      [{ match: "AURELIUS", authorName: "Marcus Aurelius", topicTags: ["mindset"], actionLine: "Try it." }],
      quotes,
      3,
    );
    assert.equal(cards.length, 2);
    assert.ok(CardSchema.safeParse(cards[0]?.card).success);
    const body = (cards[0]?.card as { bodyMd: string }).bodyMd;
    assert.ok(body.startsWith('"'));
    assert.ok(!body.startsWith('""')); // never double-wrapped
    assert.ok(!body.endsWith('""'));
    const ref = (cards[0]?.card as { sourceRef: string }).sourceRef;
    assert.ok(ref.includes("PG #27889"));
    assert.ok(ref.includes("Meditations. iv."));
  });

  it("concept candidates skip nulls, short extracts and disambiguation pages", () => {
    const cards = conceptCandidates(
      [{ wiki: "Habit", topicTags: ["habits"], actionLine: "Try it." }],
      [
        null,
      ],
    );
    assert.equal(cards.length, 0);
    const okCards = conceptCandidates(
      [{ wiki: "Habit", topicTags: ["habits"], actionLine: "Try it." }],
      [
        {
          title: "Habit",
          extract:
            "A habit is a routine of behavior that is repeated regularly and tends to occur subconsciously, over time becoming automatic.",
          pageUrl: "https://en.wikipedia.org/wiki/Habit",
        },
      ],
    );
    assert.equal(okCards.length, 1);
    assert.ok(CardSchema.safeParse(okCards[0]?.card).success);
    const ambi = conceptCandidates(
      [{ wiki: "Focus", topicTags: ["focus"], actionLine: "Try it." }],
      [
        {
          title: "Focus (disambiguation)",
          extract: "Focus may refer to: Focus (optics), Focusing, and many more uses.",
          pageUrl: "https://en.wikipedia.org/wiki/Focus",
        },
      ],
    );
    assert.equal(ambi.length, 0);
  });

  it("reading cards stay ≤600 estSeconds; audio cards may exceed", () => {
    assert.equal(CardSchema.safeParse({ ...BASE_CARD, estSeconds: 700 }).success, false);
    assert.equal(
      CardSchema.safeParse({ ...BASE_CARD, type: "audio", audioUrl: "https://x/y.mp3", estSeconds: 2000 }).success,
      true,
    );
  });
});

describe("openingExcerpt", () => {
  it("skips front matter, illustrations and takes real paragraphs under the cap", () => {
    const raw =
      "*** START OF THE PROJECT GUTENBERG EBOOK TEST ***\n\n" +
      "Produced by Someone.\n\n" +
      "[Illustration: FRANKLIN ARMS]\n\n" +
      "AUTOBIOGRAPHY OF SOMEBODY WITH ILLUSTRATIONS AND A VERY LONG TITLE THAT PASSES EIGHTY CHARS FOR SURE\n\n" +
      "Real opening paragraph about life and thought which is long enough to be kept in the final excerpt because the hard engineering here demands real prose of this size.\n\n" +
      "Second paragraph also explains the book in plain English and carries enough plain words to be kept inside the opening-lines card excerpt in our library too.\n\n";
    // Small cap: the boundary must be a completed sentence, not a fragment.
    const tight = openingExcerpt(raw, 300);
    assert.ok(tight?.endsWith("real prose of this size."));
    // Larger cap: both paragraphs fit whole.
    const roomy = openingExcerpt(raw, 430);
    assert.ok(roomy?.startsWith("Real opening paragraph"));
    assert.ok(roomy?.includes("Second paragraph also explains the book"));
    assert.ok((roomy?.length ?? 0) <= 430);
  });

  it("strips `_` emphasis markers from Gutenberg text", () => {
    const raw =
      "Foreword poem line in italics _Mind is the Master power that moulds and makes, and Man is Mind, and evermore he takes the tool of thought_, then plain words follow to keep the block long enough for the excerpt rules in the pipeline to accept it as real prose content here.\n\n";
    const excerpt = openingExcerpt(raw, 430);
    assert.ok(!excerpt?.includes("_"));
    assert.ok(excerpt?.includes("Mind is the Master power"));
  });

  it("ends at a sentence boundary when the cap truncates", () => {
    let sentence = "";
    for (let i = 0; i < 12; i++) {
      sentence +=
        "A complete sentence telling the story of this paragraph follows one inside another, sir. ";
    }
    const excerpt = openingExcerpt(`${sentence}\n\n`, 200);
    assert.ok((excerpt?.length ?? 0) <= 200);
    assert.ok(excerpt?.endsWith("sir.")); // not "...another, s"
  });
});
