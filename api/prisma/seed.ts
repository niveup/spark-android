// DEV-ONLY seed: 30 published feed items. Refuses to run in production.
// Usage: npm run db:seed  (needs DATABASE_URL in api/.env)
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";

if (process.env.NODE_ENV === "production") {
  throw new Error("Seed refuses to run in production.");
}

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL ?? "" });
const db = new PrismaClient({ adapter });

type SeedItem = {
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
  quizJson?: object;
  audioUrl?: string;
  durationMs?: number;
  bookIdx?: number;
};

const ITEMS: SeedItem[] = [
  { type: "book_idea", title: "Atomic Habits: 5 core ideas", bodyMd: "Tiny changes compound. Make good habits obvious and easy, bad ones invisible and hard. Identity first: each small action votes for the person you want to be.", actionLine: "Try this today: make one good habit 2 minutes easier to start.", topicTags: ["habits"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "summary", title: "Pride and Prejudice in 60 seconds", bodyMd: "Elizabeth Bennet spars with the proud Mr Darcy, misjudges him, reads his letter, and revises everything. A comedy about first impressions being wrong.", actionLine: "Try this today: re-read one message you may have misjudged.", topicTags: ["classics"], sourceType: "gutenberg", sourceRef: "ebook #1342", license: "public domain", isAiGenerated: true, isGrounded: true, bookIdx: 0 },
  { type: "quote", title: "Marcus Aurelius", bodyMd: "You have power over your mind — not outside events. Realize this, and you will find strength.", actionLine: "Try this today: name one outside event you will stop arguing with.", topicTags: ["stoicism"], sourceType: "wikiquote", sourceRef: "Meditations", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "concept", title: "First law of thermodynamics", bodyMd: "Energy is conserved: heat added equals work done plus change in internal energy. Q = ΔU + W. Every joule is accounted for.", actionLine: "Try this today: solve one first-law problem writing the sign convention out.", topicTags: ["thermo"], sourceType: "openstax", sourceRef: "University Physics", license: "CC BY", isAiGenerated: true, isGrounded: true },
  { type: "quiz", title: "Quiz: isentropic process", bodyMd: "An ideal gas expands isentropically. What stays constant?", actionLine: "Try this today: write the T2/T1 relation for isentropic compression.", topicTags: ["thermo"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["Temperature", "Entropy", "Enthalpy", "Pressure"], correctIndex: 1, explanation: "Isentropic = constant entropy: reversible + adiabatic." } },
  { type: "audio", title: "Moby Dick, Chapter 1", bodyMd: "Call me Ishmael. The famous opening chapter read aloud from the public-domain text.", actionLine: "Try this today: listen to one chapter while walking.", topicTags: ["classics"], sourceType: "librivox", sourceRef: "Moby Dick", license: "public domain", isAiGenerated: false, isGrounded: true, audioUrl: "https://example.com/audio/moby-ch1.mp3", durationMs: 724000, bookIdx: 1 },
  { type: "challenge", title: "5-minute desk reset", bodyMd: "Set a timer. Clear every object off your desk, wipe it, put back only what you use daily. Stop when the timer rings.", actionLine: "Do it now: 5 minutes, timer on.", topicTags: ["focus"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Bending stress formula", bodyMd: "Stress varies linearly through beam depth: σ = My/I. Maximum at the outer fiber, zero at the neutral axis.", actionLine: "Try this today: sketch the stress block for a T-section.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "quiz", title: "Quiz: neutral axis", bodyMd: "In symmetric bending of a beam, where is the bending stress zero?", actionLine: "Try this today: derive σ = My/I from first principles once.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["Outer fiber", "Neutral axis", "Supports", "Midspan"], correctIndex: 1, explanation: "Stress is zero at the neutral axis, growing linearly outward." } },
  { type: "quote", title: "Richard Feynman", bodyMd: "If you can't explain something in simple terms, you don't understand it.", actionLine: "Try this today: explain one GATE formula in one simple sentence.", topicTags: ["learning"], sourceType: "wikiquote", sourceRef: "Feynman", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "summary", title: "Frankenstein in 60 seconds", bodyMd: "Victor Frankenstein builds a creature, abandons it, and both destroy each other through revenge. Creating without responsibility is the real monster.", actionLine: "Try this today: finish one thing you started and abandoned.", topicTags: ["classics"], sourceType: "gutenberg", sourceRef: "ebook #84", license: "public domain", isAiGenerated: true, isGrounded: true, bookIdx: 2 },
  { type: "book_idea", title: "Ultralearning: 3 moves", bodyMd: "Learn by doing real projects, drill your weakest sub-skill directly, and get harsh feedback fast. Intensity plus feedback beats passive rereading.", actionLine: "Try this today: drill your weakest GATE sub-topic for 25 minutes.", topicTags: ["learning"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Carnot efficiency", bodyMd: "No heat engine beats 1 − Tc/Th. Efficiency depends only on reservoir temperatures, never on the working fluid.", actionLine: "Try this today: compute Carnot efficiency for Th=600K, Tc=300K.", topicTags: ["thermo"], sourceType: "openstax", sourceRef: "University Physics", license: "CC BY", isAiGenerated: true, isGrounded: true },
  { type: "concept", title: "Torsion formula", bodyMd: "Shear stress in a shaft: τ = Tr/J. Maximum at the outer surface, zero at center — hollow shafts carry nearly equal torque for less weight.", actionLine: "Try this today: compare solid vs hollow shaft weight for equal torque.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "quiz", title: "Quiz: Carnot between same reservoirs", bodyMd: "Two reversible engines run between the same reservoirs. Their efficiencies are…?", actionLine: "Try this today: state Carnot's theorem in one line.", topicTags: ["thermo"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["Always equal", "Higher for gas", "Higher for steam", "Unknowable"], correctIndex: 0, explanation: "All reversible engines between the same reservoirs are equally efficient." } },
  { type: "quote", title: "Leonardo da Vinci", bodyMd: "Simplicity is the ultimate sophistication.", actionLine: "Try this today: delete one unnecessary step from your routine.", topicTags: ["work"], sourceType: "wikiquote", sourceRef: "da Vinci", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "book_idea", title: "Thinking, Fast and Slow: 2 systems", bodyMd: "System 1 is fast and intuitive; System 2 is slow and effortful. Errors come from System 1 answering hard questions with easy substitutes.", actionLine: "Try this today: recheck one calculation with System 2.", topicTags: ["mind"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Second law: entropy", bodyMd: "Entropy of an isolated system never decreases. Every real process generates some — perfection is a limit, never a result.", actionLine: "Try this today: find the entropy generation in one solved example.", topicTags: ["thermo"], sourceType: "openstax", sourceRef: "University Physics", license: "CC BY", isAiGenerated: true, isGrounded: true },
  { type: "concept", title: "Shear force and bending moment", bodyMd: "Shear is the slope of the bending-moment diagram: V = dM/dx. Draw shear first; moments follow by area.", actionLine: "Try this today: draw SFD then BMD for a cantilever with tip load.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "quiz", title: "Quiz: zeroth law", bodyMd: "Two bodies each in thermal equilibrium with a third body are…?", actionLine: "Try this today: write the zeroth law in one sentence.", topicTags: ["thermo"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["In equilibrium with each other", "At the same pressure", "Isolated", "Undergoing change"], correctIndex: 0, explanation: "The zeroth law defines temperature: mutual equilibrium is transitive." } },
  { type: "challenge", title: "One page, right now", bodyMd: "Open any public-domain book and read exactly one page slowly. Mark one sentence worth keeping.", actionLine: "Do it now: one page, one kept sentence.", topicTags: ["reading"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "summary", title: "Sherlock Holmes in 60 seconds", bodyMd: "A consulting detective solves by observation and elimination: eliminate the impossible and whatever remains must be the truth.", actionLine: "Try this today: list facts before guessing on one problem.", topicTags: ["classics"], sourceType: "gutenberg", sourceRef: "ebook #1661", license: "public domain", isAiGenerated: true, isGrounded: true },
  { type: "concept", title: "Rankine cycle in 4 steps", bodyMd: "Pump, boiler, turbine, condenser. Heat in at high pressure, work out at the turbine, reject the rest. Efficiency loves high boiler pressure.", actionLine: "Try this today: label all 4 Rankine states on a T-s diagram.", topicTags: ["thermo"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "concept", title: "Mohr's circle in one line", bodyMd: "Each plane's (σ, τ) is a point on a circle; the circle's extremes are your principal stresses. Rotation doubles angles.", actionLine: "Try this today: draw Mohr's circle for uniaxial tension σ.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "quiz", title: "Quiz: Poisson's ratio of steel", bodyMd: "Poisson's ratio of steel is approximately…?", actionLine: "Try this today: use ν=0.3 to find lateral strain for 0.001 axial.", topicTags: ["som"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true, quizJson: { options: ["0.1", "0.3", "0.5", "0.0"], correctIndex: 1, explanation: "Steel ν ≈ 0.3; 0.5 is the incompressible limit (rubber-like)." } },
  { type: "audio", title: "Pride and Prejudice, Chapter 1", bodyMd: "The famous opening line read aloud — about 8 minutes from the public-domain text.", actionLine: "Try this today: listen to one chapter before sleep.", topicTags: ["classics"], sourceType: "librivox", sourceRef: "P&P", license: "public domain", isAiGenerated: false, isGrounded: true, audioUrl: "https://example.com/audio/pp-ch1.mp3", durationMs: 511000, bookIdx: 0 },
  { type: "book_idea", title: "Deep Work: 2 rules", bodyMd: "Work deeply in long unbroken blocks; drain the shallows (email, meetings) to make room. Attention is the scarce resource.", actionLine: "Try this today: one 50-minute no-phone block.", topicTags: ["focus"], sourceType: "ai-notes", sourceRef: "general knowledge", license: "original", isAiGenerated: true, isGrounded: false },
  { type: "concept", title: "Otto vs Diesel", bodyMd: "Otto: heat at constant volume (spark). Diesel: heat at constant pressure (compression ignition). Diesel wins on compression ratio.", actionLine: "Try this today: compare both efficiencies at r=8.", topicTags: ["thermo"], sourceType: "manual", sourceRef: "GATE deck", license: "original", isAiGenerated: false, isGrounded: true },
  { type: "quote", title: "Antoine de Saint-Exupéry", bodyMd: "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away.", actionLine: "Try this today: remove one thing from your study desk.", topicTags: ["work"], sourceType: "wikiquote", sourceRef: "Wind, Sand and Stars", license: "CC BY-SA", isAiGenerated: false, isGrounded: true },
  { type: "challenge", title: "Explain it to a wall", bodyMd: "Pick one formula. Explain it out loud to an empty room in under 60 seconds. Stuck = gap found.", actionLine: "Do it now: 60 seconds, out loud.", topicTags: ["learning"], sourceType: "manual", sourceRef: "SPARK deck", license: "original", isAiGenerated: true, isGrounded: false },
];

async function main(): Promise<void> {
  const existing = await db.feedItem.count();
  if (existing > 0) {
    console.log(`Seed skipped: ${existing} items already present.`);
    return;
  }
  const books = await db.$transaction([
    db.book.create({ data: { source: "gutenberg", sourceId: "1342", title: "Pride and Prejudice", author: "Jane Austen", license: "public domain", textUrl: "https://www.gutenberg.org/ebooks/1342" } }),
    db.book.create({ data: { source: "librivox", title: "Moby Dick", author: "Herman Melville", license: "public domain", audioUrl: "https://librivox.org/moby-dick-by-herman-melville/" } }),
    db.book.create({ data: { source: "gutenberg", sourceId: "84", title: "Frankenstein", author: "Mary Shelley", license: "public domain", textUrl: "https://www.gutenberg.org/ebooks/84" } }),
  ]);
  const now = new Date();
  await db.feedItem.createMany({
    data: ITEMS.map((item, i) => ({
      type: item.type,
      title: item.title.slice(0, 80),
      bodyMd: item.bodyMd.slice(0, 600),
      actionLine: item.actionLine,
      topicTags: item.topicTags,
      estSeconds: 30 + ((i * 7) % 31),
      bookId: item.bookIdx === undefined ? null : books[item.bookIdx]?.id ?? null,
      sourceType: item.sourceType,
      sourceRef: item.sourceRef,
      license: item.license,
      isAiGenerated: item.isAiGenerated,
      isGrounded: item.isGrounded,
      modelUsed: item.isAiGenerated ? "seed-notes-v1" : null,
      contentHash: `dev-seed-${i + 1}`,
      quizJson: item.quizJson ?? undefined,
      audioUrl: item.audioUrl ?? null,
      durationMs: item.durationMs ?? null,
      status: "published",
      publishedAt: now,
    })),
  });
  const count = await db.feedItem.count();
  console.log(`Seed done: ${count} published items.`);
}

await main();
await db.$disconnect();
