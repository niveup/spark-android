// Phase 3b dev bootstrap for the growth library (legacy fiction/GATE removed).
// Real library loads come from api/prisma/ingest-growth.ts (pinned IDs +
// live LibriVox/Bartlett's/Wikipedia pulls). This script stays as a search-
// based fallback bootstrap: resolves growth classics via Gutendex and seeds
// the hand-written growth deck. DEV only.
// Usage: npx tsx prisma/seed-pipeline.ts  (needs DATABASE_URL in api/.env)
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { gutendexSearch } from "../src/lib/adapters";
import { runIngestJob } from "../src/lib/ingest";

if (process.env.NODE_ENV === "production") {
  throw new Error("Pipeline seed refuses to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

const BOOK_QUERIES = [
  "as a man thinketh",
  "how to live on 24 hours a day",
  "meditations",
  "self help samuel smiles",
  "the game of life and how to play it",
];

type GrowthCard = {
  type: "concept" | "quiz" | "quote";
  title: string;
  bodyMd: string;
  actionLine: string;
  topicTags: string[];
  quiz?: { options: string[]; correctIndex: number; explanation: string };
};

const DECK: GrowthCard[] = [
  { type: "concept", title: "The 2-Minute Rule", bodyMd: "Shrink any habit until starting costs two minutes: one page, one push-up, one sentence. Intensity comes later — the work is showing up.", actionLine: "Try this today: two-minute version of one habit you skipped.", topicTags: ["habits"] },
  { type: "concept", title: "Habit stacking", bodyMd: "Attach a new habit to an old one: 'After I pour coffee, I will read a page.' The existing routine becomes the cue, so no extra motivation is needed.", actionLine: "Try this today: write one 'after X, I will Y' sentence.", topicTags: ["habits"] },
  { type: "concept", title: "Segmented sleep is old news", bodyMd: "Pre-industrial adults slept in two shifts with a calm hour between. If you wake at 3am, tiredness is not failure — read, write, then return to sleep.", actionLine: "Try this tonight: keep the wake-up calm, no screen,20 minutes of reading.", topicTags: ["mindset"] },
  { type: "concept", title: "Pay yourself first", bodyMd: "Move a fixed share of income to savings the day you are paid, before spending. What remains is the real budget — discipline by design, not willpower.", actionLine: "Try this today: set up one small automatic transfer.", topicTags: ["money"] },
  { type: "concept", title: "Timeboxing beats to-do lists", bodyMd: "A to-do says what; a timebox says when. Put the task on the calendar with a start time — unfinished work gets an honest next slot.", actionLine: "Try this today: timebox tomorrow's hardest task for 25 minutes.", topicTags: ["time"] },
  { type: "concept", title: "Systems, not goals", bodyMd: "Goals set direction; systems decide outcome. You don't rise to the level of your goals, you fall to the level of your daily systems.", actionLine: "Try this today: fix the smallest daily input behind one goal.", topicTags: ["habits"] },
  { type: "quote", title: "James Allen", bodyMd: "As a man thinketh in his heart, so is he.", actionLine: "Try this today: audit the sentence you repeat to yourself most.", topicTags: ["mindset"] },
  { type: "quote", title: "Seneca", bodyMd: "We suffer more often in imagination than in reality.", actionLine: "Try this today: write the worst case on paper instead of rehearsing it.", topicTags: ["mindset"] },
  { type: "quiz", title: "Quiz: habit loop broken", bodyMd: "You want a reading habit but skip it nightly. Which fix targets the cue, not the motivation?", actionLine: "Try this today: pick the fix that needs no willpower.", topicTags: ["habits"], quiz: { options: ["Read a whole chapter", "Place a book +1 page on your pillow", "Promise a friend", "Download a speed-reading app"], correctIndex: 1, explanation: "Anchor the new habit to an existing moment and shrink it to one page — cue + easy start." } },
  { type: "quiz", title: "Quiz: which is an asset?", bodyMd: "You receive 5,000. Which choice builds an asset under 'pay yourself first' rules?", actionLine: "Try this today: name one asset you will start this month.", topicTags: ["money"], quiz: { options: ["A phone upgrade", "A recurring savings transfer", "New sneakers", "A gaming skin"], correctIndex: 1, explanation: "An asset is stored value or income-producing. A fixed automatic transfer converts income into stored value before spending." } },
];

async function main(): Promise<void> {
  // 1. Resolve growth classics via Gutendex (search fallback; pipeline pins IDs).
  for (const q of BOOK_QUERIES) {
    const found = await gutendexSearch(q, 3);
    const pick = found.find((b) => b.textUrl) ?? found[0];
    if (!pick) {
      console.log(`No book for "${q}", skipped.`);
      continue;
    }
    const existing = await db.book.findFirst({
      where: { source: "gutenberg", sourceId: String(pick.gutenbergId) },
    });
    if (!existing) {
      await db.book.create({
        data: {
          source: "gutenberg",
          sourceId: String(pick.gutenbergId),
          title: pick.title,
          author: pick.author,
          language: pick.language,
          coverUrl: pick.coverUrl,
          isPublicDomain: true,
          textUrl: pick.textUrl,
          license: "public domain",
        },
      });
      console.log(`Book added: ${pick.title}`);
    }
  }

  // 2. Growth cards through the ingest runner (validate -> dedupe -> drafts).
  const result = await runIngestJob(
    "growth-manual-v1",
    DECK.map((c) => ({
      type: c.type,
      title: c.title,
      bodyMd: c.bodyMd,
      actionLine: c.actionLine,
      topicTags: c.topicTags,
      language: "en",
      estSeconds: 45,
      sourceType: "manual",
      sourceRef: "growth deck v1",
      license: "original",
      isAiGenerated: false,
      isGrounded: true,
      modelUsed: null,
      quiz: c.quiz ?? null,
      audioUrl: null,
      durationMs: null,
    })),
  );
  console.log(`Growth ingest: ${result.created} created, ${result.duplicates} dupes, ${result.rejected} rejected.`);

  // 3. Publish the seeded growth cards (owner-reviewed seed content).
  const published = await db.feedItem.updateMany({
    where: { status: "draft", sourceRef: "growth deck v1" },
    data: { status: "published", publishedAt: new Date() },
  });
  console.log(`Published ${published.count} growth cards.`);
}

await main();
await db.$disconnect();
