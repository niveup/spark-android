import { db } from "@/lib/db";
import { toItemState } from "@/lib/feed";
import { nextSrsDue } from "./srs";

// Shared single-update logic for POST /items/:id/state and POST /items/state.
// Idempotent per clientEventId: replays never double-count dwellMs.
export type StateUpdate = {
  seen?: boolean;
  saved?: boolean;
  liked?: boolean | null;
  dismissed?: boolean;
  dwellMs?: number;
  quizCorrect?: boolean | null;
  clientEventId?: string;
};

export async function applyStateUpdate(tokenHash: string, itemId: string, update: StateUpdate) {
  const existing = await db.itemState.findUnique({
    where: { tokenHash_itemId: { tokenHash, itemId } },
  });
  if (
    update.clientEventId &&
    existing?.clientEventIds.includes(update.clientEventId)
  ) {
    return { state: existing, replayed: true as const }; // idempotent replay
  }
  const data = {
    seenAt: existing?.seenAt ?? (update.seen ? new Date() : null),
    saved: update.saved ?? existing?.saved ?? false,
    liked: update.liked !== undefined ? update.liked : (existing?.liked ?? null),
    dismissed: update.dismissed ?? existing?.dismissed ?? false,
    dwellMs: (existing?.dwellMs ?? 0) + (update.dwellMs ?? 0),
    quizCorrect: update.quizCorrect ?? existing?.quizCorrect ?? null,
    clientEventIds:
      update.clientEventId != null
        ? [...(existing?.clientEventIds ?? []), update.clientEventId].slice(-50)
        : (existing?.clientEventIds ?? []),
  };
  let srs: { srsDueAt: Date | null; srsIntervalDays: number | null } = {
    srsDueAt: existing?.srsDueAt ?? null,
    srsIntervalDays: existing?.srsIntervalDays ?? null,
  };
  if (update.quizCorrect !== undefined && update.quizCorrect !== null) {
    const next = nextSrsDue(existing?.srsIntervalDays ?? null, update.quizCorrect);
    srs = { srsDueAt: next.due, srsIntervalDays: next.interval };
  }
  const state = existing
    ? await db.itemState.update({
        where: { tokenHash_itemId: { tokenHash, itemId } },
        data: { ...data, ...srs },
      })
    : await db.itemState.create({
        data: { tokenHash, itemId, ...data, ...srs },
      });
  return { state, replayed: false as const };
}

export function serializeState(itemId: string, state: NonNullable<Awaited<ReturnType<typeof applyStateUpdate>>["state"]>) {
  return toItemState(itemId, state);
}
