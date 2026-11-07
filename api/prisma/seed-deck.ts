// DEV-ONLY canonical hand-written deck, shared by seed.ts (direct insert) and
// seed-pipeline.ts (validated ingest path). One copy means the two bootstraps
// cannot drift into near-duplicate cards again, and every card keeps the
// sourceType / sourceRef / license it was written with.

export type SeedQuiz = {
  options: string[];
  correctIndex: number;
  explanation: string;
};

export type SeedItem = {
  type: string;
  title: string;
  bodyMd: string;
  actionLine: string;
  topicTags: string[];
  sourceType: string;
  sourceRef: string;
  license: string;
  isAiGenerated: boolean;
  isGrounded: boolean;
  quizJson?: SeedQuiz;
  audioUrl?: string;
  durationMs?: number;
  bookIdx?: number;
};

export const SEED_BOOKS = [
  { source: "gutenberg", sourceId: "4507", title: "As a Man Thinketh", author: "James Allen", license: "public domain", textUrl: "https://www.gutenberg.org/ebooks/4507" },
  { source: "gutenberg", sourceId: "2274", title: "How to Live on Twenty-Four Hours a Day", author: "Arnold Bennett", license: "public domain", textUrl: "https://www.gutenberg.org/ebooks/2274" },
  { source: "gutenberg", sourceId: "20203", title: "The Autobiography of Benjamin Franklin", author: "Benjamin Franklin", license: "public domain", textUrl: "https://www.gutenberg.org/ebooks/20203" },
];

export const SEED_DECK: SeedItem[] = [
  // --- Book idea notes for modern (copyrighted) books: AI-written, unverified ---
  { type: "book_idea", title: "Atomic Habits: 5 core ideas", bodyMd: "Tiny changes compound. Make good habits obvious and easy, bad ones invisible and hard. Identity first: each small action votes for the person you want to be.", actionLine: "Try this today: make one good habit 2 minutes easier to start.", topicTags: ["habits"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "book_idea", title: "Deep Work: 2 rules", bodyMd: "Work deeply in long unbroken blocks; drain the shallows (email, meetings) to make room. Attention is the scarce resource.", actionLine: "Try this today: one 50-minute no-phone block.", topicTags: ["focus"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "book_idea", title: "Thinking, Fast and Slow: 2 systems", bodyMd: "System 1 is fast and intuitive; System 2 is slow and effortful. Errors come from System 1 answering hard questions with easy substitutes.", actionLine: "Try this today: recheck one hasty judgement with System 2.", topicTags: ["mindset"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "book_idea", title: "Ultralearning: 3 moves", bodyMd: "Learn by doing real projects, drill your weakest sub-skill directly, and get harsh feedback fast. Intensity plus feedback beats passive rereading.", actionLine: "Try this today: drill your weakest sub-skill for 25 minutes.", topicTags: ["learning"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },

  // --- Summaries grounded in public-domain books (pipeline source texts) ---
  { type: "summary", title: "As a Man Thinketh, in 60 seconds", bodyMd: "James Allen (1903): thought is the seed of character and circumstance. You do not attract what you want, you attract what you are — master your morning thoughts and the day follows.", actionLine: "Try this today: pick the one thought you will refuse to repeat.", topicTags: ["mindset"], sourceType: "gutenberg", sourceRef: "ebook #4507", license: "public domain", isAiGenerated: true, isGrounded: true, bookIdx: 0 },
  { type: "summary", title: "How to Live on 24 Hours a Day", bodyMd: "Arnold Bennett (1908): you get the same 24 hours as everyone else, and your evening hours decide who you become. Stop treating the day as a corridor to the next.", actionLine: "Try this today: give 90 evening minutes one real project.", topicTags: ["time"], sourceType: "gutenberg", sourceRef: "ebook #2274", license: "public domain", isAiGenerated: true, isGrounded: true, bookIdx: 1 },
  { type: "summary", title: "Franklin's Autobiography, in 60 seconds", bodyMd: "A printer's apprentice engineers himself: 13 virtues tracked weekly, one at a time, with a small book of faults. Evidence that systems beat resolutions.", actionLine: "Try this today: track one virtue of yours on paper for a week.", topicTags: ["habits"], sourceType: "gutenberg", sourceRef: "ebook #20203", license: "public domain", isAiGenerated: true, isGrounded: true, bookIdx: 2 },

  // --- Quotes (Bartlett's / Wikiquote, attribution kept) ---
  { type: "quote", title: "James Allen", bodyMd: "As a man thinketh in his heart, so is he.", actionLine: "Try this today: audit the sentence you repeat to yourself most.", topicTags: ["mindset"], sourceType: "gutenberg", sourceRef: "As a Man Thinketh, ebook #4507", license: "public domain", isAiGenerated: false, isGrounded: true },
  { type: "quote", title: "Marcus Aurelius", bodyMd: "You have power over your mind — not outside events. Realize this, and you will find strength.", actionLine: "Try this today: name one outside event you will stop arguing with.", topicTags: ["mindset"], sourceType: "wikiquote", sourceRef: "Meditations", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "quote", title: "Seneca", bodyMd: "We suffer more often in imagination than in reality.", actionLine: "Try this today: write the worst case on paper instead of rehearsing it.", topicTags: ["mindset"], sourceType: "wikiquote", sourceRef: "Seneca", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "quote", title: "William James", bodyMd: "The greatest weapon against stress is our ability to choose one thought over another.", actionLine: "Try this today: swap one 'what if' for one 'what now'.", topicTags: ["mindset"], sourceType: "wikiquote", sourceRef: "William James", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "quote", title: "Richard Feynman", bodyMd: "If you can't explain something in simple terms, you don't understand it.", actionLine: "Try this today: explain one idea you learned in one simple sentence.", topicTags: ["learning"], sourceType: "wikiquote", sourceRef: "Feynman", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },

  // --- Concept cards: growth life-skills (AI-written where ungrounded) ---
  { type: "concept", title: "The 2-Minute Rule", bodyMd: "Shrink any habit until starting costs two minutes. Read one page, one push-up, one sentence. The goal is showing up — intensity comes later.", actionLine: "Try this today: two-minute version of one habit you skipped.", topicTags: ["habits"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Habit stacking", bodyMd: "Attach a new habit to an existing one: 'After I pour morning coffee, I will read a page.' The old habit becomes the cue; no motivation needed.", actionLine: "Try this today: write one 'after X, I will Y' sentence.", topicTags: ["habits"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Pay yourself first", bodyMd: "Move a fixed share of income to savings the day you are paid, before spending. What remains is your true budget — discipline by design, not willpower.", actionLine: "Try this today: set up one small automatic transfer.", topicTags: ["money"], sourceType: "ai-notes", sourceRef: "idea from Wikibooks Personal Finance (CC BY-SA)", license: "original (concept)", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Timeboxing beats to-do lists", bodyMd: "A to-do says what; a timebox says when. Put the task on the calendar with a start time, and unfinished work gets an honest next slot.", actionLine: "Try this today: timebox tomorrow's hardest task for 25 minutes.", topicTags: ["time"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Spaced repetition in 30 seconds", bodyMd: "Reviewing an idea just before you forget it multiplies memory. 1 day, 3 days, 7 days, 3 weeks — short sessions beat one cram forever.", actionLine: "Try this today: re-read one saved card from three days ago.", topicTags: ["learning"], sourceType: "gutenberg", sourceRef: "Bartlett's use of recency, practice deck v1", license: "original (concept)", isAiGenerated: false, isGrounded: true },
  { type: "concept", title: "Attention residue", bodyMd: "Switching tasks leaves part of your mind stuck on the last one. Batch similar work and give each block a clean start to think clearly.", actionLine: "Try this today: batch reading, then batch chatting, one after the other.", topicTags: ["focus"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Systems, not goals", bodyMd: "Goals set direction; systems decide the outcome. You don't rise to the level of your goals, you fall to the level of your daily systems.", actionLine: "Try this today: fix the smallest daily input behind one goal.", topicTags: ["habits"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Segmented sleep is old news", bodyMd: "Pre-industrial adults slept in two shifts with a calm hour between. If you wake at 3am, tiredness is not failure — read, write, then return to sleep.", actionLine: "Try this tonight: keep the wake-up calm, no screen, 20 minutes of reading.", topicTags: ["mindset"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },

  // --- Challenges (do-it-now micro-commitments) ---
  { type: "challenge", title: "24-Hour audit", bodyMd: "Record how your last 24 hours actually went, hour by hour, and mark each block 'alive' or 'drained'. Bennett's method: see the day before designing tomorrow's.", actionLine: "Do it now: 10 minutes, mark blocks, find your free 90 minutes.", topicTags: ["time"], sourceType: "gutenberg", sourceRef: "How to Live on 24 Hours a Day, ebook #2274", license: "public domain", isAiGenerated: true, isGrounded: true },
  { type: "challenge", title: "5-minute desk reset", bodyMd: "Set a timer. Clear every object off your desk, wipe it, put back only what you use daily. Stop when the timer rings.", actionLine: "Do it now: 5 minutes, timer on.", topicTags: ["focus"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "challenge", title: "One page, right now", bodyMd: "Open any public-domain book on your shelf and read exactly one page slowly. Keep one sentence.", actionLine: "Do it now: one page, one kept sentence.", topicTags: ["reading"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "challenge", title: "Recall before scroll", bodyMd: "Before opening Instagram or Shorts, say out loud one thing you learned yesterday. If you can't, open your SRS card first — then scroll consciously.", actionLine: "Do it now: one recall, then decide.", topicTags: ["learning"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },

  // --- Quizzes (owner-reviewed, manual grounded) ---
  { type: "quiz", title: "Quiz: which is an asset?", bodyMd: "You receive 5,000. Which choice builds an asset under 'pay yourself first' rules?", actionLine: "Try this today: name one asset you will start this month.", topicTags: ["money"], sourceType: "manual", sourceRef: "growth deck v1", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["A phone upgrade", "A recurring savings transfer", "New sneakers", "A gaming skin"], correctIndex: 1, explanation: "An asset is stored value or income-producing. A fixed automatic transfer converts income into stored value before spending." } },
  { type: "quiz", title: "Quiz: habit loop broken", bodyMd: "You want a reading habit but skip it nightly. Which fix targets the cue, not the motivation?", actionLine: "Try this today: pick the fix that needs no willpower.", topicTags: ["habits"], sourceType: "manual", sourceRef: "growth deck v1", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["Read a whole chapter", "Place a book +1 page on your pillow", "Promise a friend", "Download a speed-reading app"], correctIndex: 1, explanation: "Anchor the new habit to an existing moment (getting into bed) and shrink it to one page — cue + easy start." } },
];
