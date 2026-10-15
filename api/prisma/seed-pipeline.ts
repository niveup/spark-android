// Phase 3 seed: 5 public-domain books (via Gutendex adapter) + 20 GATE
// Mechanical cards (thermo/SOM, original concept content). DEV only.
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
  "pride and prejudice",
  "frankenstein",
  "moby dick",
  "sherlock holmes",
  "jane eyre",
];

type GateCard = {
  type: "concept" | "quiz";
  title: string;
  bodyMd: string;
  actionLine: string;
  topicTags: string[];
  quiz?: { options: string[]; correctIndex: number; explanation: string };
};

const GATE_CARDS: GateCard[] = [
  { type: "concept", title: "Zeroth law of thermodynamics", bodyMd: "If two bodies are each in thermal equilibrium with a third, they are in equilibrium with each other. This defines temperature: the thing thermometers measure.", actionLine: "Write the zeroth law in one sentence from memory.", topicTags: ["thermo"] },
  { type: "concept", title: "Enthalpy in one line", bodyMd: "H = U + PV: internal energy plus flow work. For constant-pressure processes, heat added equals enthalpy change. Turbines and compressors speak enthalpy.", actionLine: "Rewrite one first-law problem using h instead of u.", topicTags: ["thermo"] },
  { type: "quiz", title: "Quiz: cp vs cv", bodyMd: "For an ideal gas, cp − cv equals…?", actionLine: "Prove cp − cv = R starting from definitions.", topicTags: ["thermo"], quiz: { options: ["R", "γR", "R/γ", "Zero"], correctIndex: 0, explanation: "Mayer's relation: cp − cv = R for ideal gases." } },
  { type: "concept", title: "Entropy generation", bodyMd: "Real processes generate entropy: Sgen ≥ 0, zero only for reversible. Lost work = T0 × Sgen. Find the irreversibility, find the loss.", actionLine: "Compute Sgen for throttling in one solved example.", topicTags: ["thermo"] },
  { type: "quiz", title: "Quiz: throttling enthalpy", bodyMd: "Across a throttling valve, enthalpy…?", actionLine: "Draw the throttling process on an h-s diagram.", topicTags: ["thermo"], quiz: { options: ["Stays constant", "Always rises", "Always falls", "Becomes zero"], correctIndex: 0, explanation: "Throttling is isenthalpic: h1 = h2 (kinetic changes negligible)." } },
  { type: "concept", title: "Otto cycle efficiency", bodyMd: "η = 1 − 1/r^(γ−1). Only compression ratio and γ matter — not fuel, not size. Raise r (knock permitting) to raise efficiency.", actionLine: "Compute η for r=8, γ=1.4.", topicTags: ["thermo"] },
  { type: "concept", title: "Mean effective pressure", bodyMd: "MEP = work per cycle ÷ swept volume. It measures how well you use engine size. Same power from smaller engine = higher MEP.", actionLine: "Find MEP for one GATE previous-year problem.", topicTags: ["thermo"] },
  { type: "quiz", title: "Quiz: Diesel vs Otto at same r", bodyMd: "At the same compression ratio, which is more efficient?", actionLine: "Sketch both cycles on p-v and compare areas.", topicTags: ["thermo"], quiz: { options: ["Otto", "Diesel", "Equal", "Depends on fuel"], correctIndex: 0, explanation: "At same r, Otto wins (heat added at higher average temperature)." } },
  { type: "concept", title: "Psychrometry: WBT vs DBT", bodyMd: "Wet-bulb ≤ dry-bulb, equal only at saturation (100% RH). The gap measures dryness of air. Cooling towers live on this gap.", actionLine: "Read one state point off a psychrometric chart.", topicTags: ["thermo"] },
  { type: "concept", title: "Critical point of water", bodyMd: "374°C, 221 bar: liquid and vapor become indistinguishable. Above it, no boiling — only supercritical fluid. Power plants flirt with it for efficiency.", actionLine: "Mark critical point on a T-v diagram from memory.", topicTags: ["thermo"] },
  { type: "concept", title: "Axial stress and strain", bodyMd: "σ = P/A, ε = δL/L, linked by E: σ = Eε. Three equations, three unknowns for most bars — equilibrium, compatibility, constitutive.", actionLine: "Solve one statically-indeterminate bar problem.", topicTags: ["som"] },
  { type: "quiz", title: "Quiz: thermal stress", bodyMd: "A bar fixed at both ends is heated uniformly. The stress is…?", actionLine: "Derive σ = −EαΔT once without looking.", topicTags: ["som"], quiz: { options: ["Compressive", "Tensile", "Zero", "Shear"], correctIndex: 0, explanation: "Prevented expansion → compressive stress σ = EαΔT." } },
  { type: "concept", title: "Torsion: angle of twist", bodyMd: "θ = TL/GJ. Twist grows with torque and length, shrinks with stiffness GJ. Hollow shafts maximize J per kilogram.", actionLine: "Compute θ for a 1 m steel shaft under 500 N·m.", topicTags: ["som"] },
  { type: "concept", title: "Euler buckling in one line", bodyMd: "Pcr = π²EI/(KL)². Long columns fail by buckling far below yield. Halve the length → 4× the load.", actionLine: "List K values for all four end conditions.", topicTags: ["som"] },
  { type: "quiz", title: "Quiz: slenderness ratio", bodyMd: "Slenderness ratio λ is…?", actionLine: "Compute λ for one column problem and classify it.", topicTags: ["som"], quiz: { options: ["Le/k", "k/Le", "EI/L²", "P/A"], correctIndex: 0, explanation: "λ = effective length ÷ radius of gyration. High λ → Euler applies." } },
  { type: "concept", title: "Principal stresses", bodyMd: "Rotate the element until shear vanishes: what remains are σ1, σ2. Max shear = (σ1−σ2)/2 at 45°. Every stress state has this calm orientation.", actionLine: "Find principal stresses for σx=80, σy=20, τ=30 MPa.", topicTags: ["som"] },
  { type: "concept", title: "Deflection by integration", bodyMd: "EIv'''' = q(x): integrate four times, apply boundary conditions. Symmetry halves the work — always exploit it first.", actionLine: "Derive cantilever tip deflection δ = PL³/3EI.", topicTags: ["som"] },
  { type: "quiz", title: "Quiz: point of contraflexure", bodyMd: "At the point of contraflexure, bending moment is…?", actionLine: "Mark it on one overhanging-beam BMD.", topicTags: ["som"], quiz: { options: ["Zero", "Maximum", "Minimum", "Equal to shear"], correctIndex: 0, explanation: "Contraflexure = BM changes sign, so it crosses zero there." } },
  { type: "concept", title: "Strain energy in torsion", bodyMd: "U = T²L/2GJ. Energy loves high torque and compliance. Springs are just torsion bars in costume.", actionLine: "Compare strain energy of solid vs hollow shaft.", topicTags: ["som"] },
  { type: "concept", title: "Fatigue: S-N curve", bodyMd: "Below the endurance limit, steel survives infinite cycles. Design rotating shafts under it and fatigue stops being a question.", actionLine: "Sketch an S-N curve and mark Se.", topicTags: ["som"] },
];

async function main(): Promise<void> {
  // 1. Books from the real Gutendex catalog (public domain only).
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

  // 2. GATE cards through the ingest runner (validate -> dedupe -> drafts).
  const result = await runIngestJob(
    "gate-manual-v1",
    GATE_CARDS.map((c, i) => ({
      type: c.type,
      title: c.title,
      bodyMd: c.bodyMd,
      actionLine: c.actionLine,
      topicTags: c.topicTags,
      language: "en",
      estSeconds: 45,
      sourceType: "manual",
      sourceRef: "GATE mechanical deck v1",
      license: "original",
      isAiGenerated: false,
      isGrounded: true,
      modelUsed: null,
      quiz: c.quiz ?? null,
      audioUrl: null,
      durationMs: null,
    })),
  );
  console.log(`GATE ingest: ${result.created} created, ${result.duplicates} dupes, ${result.rejected} rejected.`);

  // 3. Publish the seeded GATE cards (owner-reviewed seed content).
  const published = await db.feedItem.updateMany({
    where: { status: "draft", sourceRef: "GATE mechanical deck v1" },
    data: { status: "published", publishedAt: new Date() },
  });
  console.log(`Published ${published.count} GATE cards.`);
}

await main();
await db.$disconnect();
