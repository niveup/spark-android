// Phase 5b unit tests: ranking v1 + SRS scheduler. Pure logic (no DB, no
// server needed) so they run anywhere — same runner as the other test files.
// Usage: npm test
import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import {
  hashSeed,
  mulberry32,
  rankFeed,
  type RankableItem,
  type RankableState,
} from "../src/lib/feed";
import { nextSrsDue } from "../src/lib/srs";

const item = (id: string, topicTags: string[] = []): RankableItem => ({ id, topicTags });

const state = (itemId: string, over: Partial<RankableState> = {}): [string, RankableState] => [
  itemId,
  { itemId, seenAt: null, saved: false, liked: null, srsDueAt: null, ...over },
];

const stateMap = (...pairs: [string, RankableState][]) => new Map(pairs);

const past = (days: number) => new Date(Date.now() - days * 24 * 60 * 60 * 1000);
const future = (days: number) => new Date(Date.now() + days * 24 * 60 * 60 * 1000);
const ids = (items: RankableItem[]) => items.map((i) => i.id);

describe("rankFeed", () => {
  it("returns every eligible item exactly once", () => {
    const items = [item("a", ["x"]), item("b"), item("c"), item("d")];
    const states = stateMap(state("a"), state("b"), state("c"), state("d"));
    const ranked = rankFeed(items, states, "seed-1");
    assert.deepEqual([...ids(ranked)].sort(), ["a", "b", "c", "d"]);
  });

  it("puts SRS-due items first — even when recently seen (rule 1 > rule 4)", () => {
    const items = [item("fresh"), item("due"), item("older")];
    const states = stateMap(
      state("fresh"),
      state("due", { seenAt: past(2), srsDueAt: past(0.5) }),
      state("older", { seenAt: past(40) }),
    );
    const ranked = rankFeed(items, states, "seed-2");
    assert.equal(ranked[0]!.id, "due");
  });

  it("does not boost items whose SRS date is still in the future", () => {
    const items = [item("later"), item("due")];
    const states = stateMap(
      state("later", { srsDueAt: future(3) }),
      state("due", { srsDueAt: past(1) }),
    );
    assert.deepEqual(ids(rankFeed(items, states, "seed-3")), ["due", "later"]);
  });

  it("excludes items seen within 30 days unless saved", () => {
    const items = [item("seenRecent"), item("kept"), item("seenOld")];
    const states = stateMap(
      state("seenRecent", { seenAt: past(10) }),
      state("kept", { seenAt: past(10), saved: true }),
      state("seenOld", { seenAt: past(40) }),
    );
    const ranked = ids(rankFeed(items, states, "seed-4"));
    assert.ok(!ranked.includes("seenRecent"));
    assert.ok(ranked.includes("kept"));
    assert.ok(ranked.includes("seenOld"));
  });

  it("prefers unseen items from topics saved/liked before", () => {
    const items = [item("likedTopic", ["habits"]), item("other", ["misc"])];
    const states = stateMap(state("likedTopic", { saved: true }));
    const ranked = rankFeed(items, states, "seed-5");
    assert.deepEqual(ids(ranked), ["likedTopic", "other"]);
  });

  it("is deterministic for the same seed", () => {
    const items = Array.from({ length: 12 }, (_, i) =>
      item(`i${i}`, i % 2 ? ["a"] : ["b"]),
    );
    const states = stateMap(state("i1", { saved: true }), state("i5", { seenAt: past(40) }));
    const one = rankFeed(items, states, "stable");
    const two = rankFeed(items, states, "stable");
    assert.deepEqual(ids(one), ids(two));
  });
});

describe("seed helpers", () => {
  it("hashSeed is stable and seed-sensitive", () => {
    assert.equal(hashSeed("spark"), hashSeed("spark"));
    assert.notEqual(hashSeed("a"), hashSeed("b"));
  });

  it("mulberry32 with the same seed produces the same sequence", () => {
    const a = mulberry32(hashSeed("s"));
    const b = mulberry32(hashSeed("s"));
    for (let i = 0; i < 8; i++) assert.equal(a(), b());
  });
});

describe("nextSrsDue", () => {
  it("advances through the fixed steps on correct answers", () => {
    assert.equal(nextSrsDue(null, true).interval, 1);
    assert.equal(nextSrsDue(1, true).interval, 3);
    assert.equal(nextSrsDue(3, true).interval, 7);
    assert.equal(nextSrsDue(7, true).interval, 14);
    assert.equal(nextSrsDue(14, true).interval, 30);
    assert.equal(nextSrsDue(30, true).interval, 60);
    assert.equal(nextSrsDue(60, true).interval, 60); // capped at the last step
  });

  it("resets to tomorrow on a wrong answer", () => {
    assert.equal(nextSrsDue(30, false).interval, 1);
    assert.equal(nextSrsDue(null, false).interval, 1);
  });

  it("due date is interval days from now", () => {
    const { due, interval } = nextSrsDue(null, true);
    const delta = due.getTime() - Date.now();
    assert.ok(Math.abs(delta - interval * 24 * 60 * 60 * 1000) < 5000);
  });
});
