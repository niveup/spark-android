import { db } from "./db";
import { CardSchema, contentHash, type PipelineCard } from "./cards";

// Phase 3 ingest runner: validate -> dedupe -> insert as drafts -> log job.
// Additive only; never updates published rows.

export type IngestResult = {
  jobId: string;
  received: number;
  created: number;
  duplicates: number;
  rejected: number;
};

export function dailyAiCap(): number {
  const raw = Number(process.env.DAILY_AI_CAP ?? 50);
  return Number.isFinite(raw) && raw > 0 ? Math.floor(raw) : 50;
}

export async function aiCardsToday(): Promise<number> {
  const start = new Date();
  start.setUTCHours(0, 0, 0, 0);
  return db.feedItem.count({
    where: { isAiGenerated: true, createdAt: { gte: start } },
  });
}

export async function runIngestJob(
  source: string,
  candidates: unknown[],
  onCreated?: (card: PipelineCard, id: string) => Promise<void>,
): Promise<IngestResult> {
  const job = await db.ingestJob.create({ data: { source, status: "running" } });
  let created = 0;
  let duplicates = 0;
  let rejected = 0;
  try {
    const cap = dailyAiCap();
    let aiToday = await aiCardsToday();
    for (const raw of candidates) {
      const parsed = CardSchema.safeParse(raw);
      if (!parsed.success) {
        rejected++;
        continue;
      }
      const card: PipelineCard = parsed.data;
      if (card.isAiGenerated && aiToday >= cap) {
        rejected++; // daily AI cap reached
        continue;
      }
      const hash = contentHash(card.type, card.title, card.bodyMd);
      const exists = await db.feedItem.findUnique({ where: { contentHash: hash } });
      if (exists) {
        duplicates++;
        continue;
      }
      await db.feedItem.create({
        data: {
          type: card.type,
          title: card.title,
          bodyMd: card.bodyMd,
          actionLine: card.actionLine,
          topicTags: card.topicTags,
          language: card.language,
          estSeconds: card.estSeconds,
          sourceType: card.sourceType,
          sourceRef: card.sourceRef,
          license: card.license,
          isAiGenerated: card.isAiGenerated,
          isGrounded: card.isGrounded,
          modelUsed: card.modelUsed,
          contentHash: hash,
          quizJson: card.quiz ?? undefined,
          audioUrl: card.audioUrl,
          durationMs: card.durationMs,
          status: "draft", // human/app review before publish (Phase 3 gate)
        },
      });
      created++;
      if (card.isAiGenerated) aiToday++;
    }
    await db.ingestJob.update({
      where: { id: job.id },
      data: { status: "done", finishedAt: new Date(), itemsCreated: created },
    });
  } catch (e) {
    await db.ingestJob.update({
      where: { id: job.id },
      data: {
        status: "failed",
        finishedAt: new Date(),
        itemsCreated: created,
        error: String(e).slice(0, 500),
      },
    });
    throw e;
  }
  return { jobId: job.id, received: candidates.length, created, duplicates, rejected };
}
