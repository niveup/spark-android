// Phase 2 contract tests: auth, feed pagination, state upsert + idempotency.
// Needs the DEV server running (npm run dev) with DATABASE_URL + SPARK_PASSCODE.
// Usage: npm test
import "dotenv/config";
import { strict as assert } from "node:assert";
import { randomUUID } from "node:crypto";
import { after, before, describe, it } from "node:test";

const BASE = process.env.API_BASE ?? "http://localhost:3000/api/mobile/v1";
const PASSCODE = process.env.SPARK_PASSCODE ?? "dev-only-change-me";

let token = "";

async function api(path: string, init?: RequestInit) {
  // Unique spoofed IP per test process so the dev server's in-memory rate
  // limiter (5 logins/min per IP) never carries state across test runs.
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      "x-forwarded-for": `test-run-${process.pid}`,
      ...(init?.headers ?? {}),
    },
  });
  const json = (await res.json()) as Record<string, unknown>;
  return { status: res.status, json };
}

function authHeaders() {
  return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
}

before(async () => {
  const { status, json } = await api("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ passcode: PASSCODE, deviceLabel: `test-${randomUUID().slice(0, 8)}` }),
  });
  assert.equal(status, 200);
  token = (json.data as { token: string }).token;
  assert.ok(token.length >= 32);
});

after(async () => {
  await api("/auth/logout", { method: "POST", headers: authHeaders() });
});

describe("auth", () => {
  it("wrong passcode -> 401 UNAUTHORIZED", async () => {
    const { status, json } = await api("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ passcode: "nope", deviceLabel: "t" }),
    });
    assert.equal(status, 401);
    assert.equal((json.error as { code: string }).code, "UNAUTHORIZED");
  });

  it("missing fields -> 400 VALIDATION", async () => {
    const { status, json } = await api("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    assert.equal(status, 400);
    assert.equal((json.error as { code: string }).code, "VALIDATION");
  });

  it("no token -> 401 on feed", async () => {
    const { status } = await api("/feed");
    assert.equal(status, 401);
  });
});

describe("feed pagination", () => {
  it("first page shape + deterministic order per session", async () => {
    const s = await api("/sessions", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ deviceId: "test", cardLimit: 20, minuteLimit: 15 }),
    });
    assert.equal(s.status, 201);
    const sessionId = (s.json.data as { sessionId: string }).sessionId;

    const p1 = await api(`/feed?limit=10&sessionId=${sessionId}`, { headers: authHeaders() });
    assert.equal(p1.status, 200);
    const d1 = p1.json as { data: { id: string }[]; nextCursor: string | null };
    assert.equal(d1.data.length, 10);
    assert.ok(d1.nextCursor);

    const p1b = await api(`/feed?limit=10&sessionId=${sessionId}`, { headers: authHeaders() });
    const d1b = p1b.json as { data: { id: string }[]; nextCursor: string | null };
    assert.deepEqual(d1b.data.map((i) => i.id), d1.data.map((i) => i.id));

    const p2 = await api(`/feed?limit=25&cursor=${d1.nextCursor}&sessionId=${sessionId}`, { headers: authHeaders() });
    const d2 = p2.json as { data: { id: string }[]; nextCursor: string | null };
    assert.ok(d2.data.length > 0);
    const overlap = d2.data.filter((i) => d1.data.some((j) => j.id === i.id));
    assert.equal(overlap.length, 0);

    const close = await api(`/sessions/${sessionId}`, {
      method: "PATCH",
      headers: authHeaders(),
      body: JSON.stringify({ itemsSeen: 10, limitReached: false }),
    });
    assert.equal(close.status, 200);
  });

  it("unknown session close -> 404", async () => {
    const { status } = await api("/sessions/does-not-exist", {
      method: "PATCH",
      headers: authHeaders(),
      body: JSON.stringify({ itemsSeen: 1, limitReached: false }),
    });
    assert.equal(status, 404);
  });
});

describe("item state", () => {
  it("single upsert + replay is idempotent", async () => {
    const feed = await api("/feed?limit=1", { headers: authHeaders() });
    const itemId = ((feed.json as { data: { id: string }[] }).data[0] as { id: string }).id;
    const eventId = randomUUID();

    const u1 = await api(`/items/${itemId}/state`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ seen: true, saved: true, dwellMs: 5000, clientEventId: eventId }),
    });
    assert.equal(u1.status, 200);
    const s1 = u1.json.data as { saved: boolean; dwellMs: number };
    assert.equal(s1.saved, true);
    assert.equal(s1.dwellMs, 5000);

    const u2 = await api(`/items/${itemId}/state`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ seen: true, saved: true, dwellMs: 5000, clientEventId: eventId }),
    });
    const s2 = u2.json.data as { dwellMs: number };
    assert.equal(s2.dwellMs, 5000); // not doubled

    const saved = await api("/items/saved?q=&limit=50", { headers: authHeaders() });
    const savedItems = (saved.json as { data: { id: string; saved: boolean }[] }).data;
    assert.ok(savedItems.some((i) => i.id === itemId));
    assert.equal(savedItems.find((i) => i.id === itemId)?.saved, true); // saved list carries state

    // /feed carries per-device state too (5b): a saved item comes back marked.
    const refeed = await api("/feed?limit=50&excludeSeen=false", { headers: authHeaders() });
    const fedItem = (refeed.json as { data: { id: string; saved: boolean }[] }).data.find(
      (i) => i.id === itemId,
    );
    assert.equal(fedItem?.saved, true);

    const u3 = await api(`/items/${itemId}/state`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ saved: false, clientEventId: randomUUID() }),
    });
    assert.equal((u3.json.data as { saved: boolean }).saved, false);
  });

  it("batch rejects unknown ids", async () => {
    const { status, json } = await api("/items/state", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ updates: [{ itemId: "missing", clientEventId: randomUUID(), seen: true }] }),
    });
    assert.equal(status, 200);
    assert.deepEqual((json.data as { rejected: string[] }).rejected, ["missing"]);
  });

  it("unknown item -> 404", async () => {
    const { status } = await api("/items/missing/state", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ seen: true }),
    });
    assert.equal(status, 404);
  });
});

describe("single item", () => {
  it("fetches one published card by id, with the caller's state", async () => {
    const feed = await api("/feed?limit=1", { headers: authHeaders() });
    const itemId = ((feed.json as { data: { id: string }[] }).data[0] as { id: string }).id;

    const res = await api(`/items/${itemId}`, { headers: authHeaders() });
    assert.equal(res.status, 200);
    const item = res.json.data as { id: string; title: string; saved: boolean };
    assert.equal(item.id, itemId);
    assert.ok(item.title.length > 0);
    assert.equal(item.saved, false);

    await api(`/items/${itemId}/state`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ saved: true, clientEventId: randomUUID() }),
    });
    const saved = await api(`/items/${itemId}`, { headers: authHeaders() });
    assert.equal((saved.json.data as { saved: boolean }).saved, true);
  });

  it("unknown item -> 404, no token -> 401", async () => {
    const missing = await api("/items/does-not-exist", { headers: authHeaders() });
    assert.equal(missing.status, 404);
    assert.equal((missing.json.error as { code: string }).code, "NOT_FOUND");

    const anonymous = await api("/items/does-not-exist");
    assert.equal(anonymous.status, 401);
  });
});

describe("batch state", () => {
  it("uploads a batch and replays it without double-counting dwell", async () => {
    const feed = await api("/feed?limit=2", { headers: authHeaders() });
    const items = (feed.json as { data: { id: string }[] }).data;
    const item0 = items[0]!;
    const item1 = items[1]!;
    const batch = {
      updates: [
        { itemId: item0.id, clientEventId: randomUUID(), seen: true, dwellMs: 3000 },
        { itemId: item1.id, clientEventId: randomUUID(), seen: true, dwellMs: 1000 },
      ],
    };
    const r1 = await api("/items/state", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(batch),
    });
    assert.equal(r1.status, 200);
    const d1 = r1.json.data as {
      states: { itemId: string; dwellMs: number }[];
      rejected: string[];
    };
    assert.equal(d1.rejected.length, 0);
    assert.equal(d1.states.length, 2);
    const dwellBefore = new Map(d1.states.map((s) => [s.itemId, s.dwellMs]));

    const r2 = await api("/items/state", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(batch), // exact replay
    });
    assert.equal(r2.status, 200);
    const d2 = r2.json.data as { states: { itemId: string; dwellMs: number }[] };
    for (const s of d2.states) {
      assert.equal(s.dwellMs, dwellBefore.get(s.itemId)); // unchanged by replay
    }
  });
});

describe("stats", () => {
  it("returns personal totals", async () => {
    const { status, json } = await api("/stats", { headers: authHeaders() });
    assert.equal(status, 200);
    const d = json.data as Record<string, number>;
    for (const key of ["seenTotal", "savedTotal", "likedTotal", "quizzesDue", "sessionsTotal"]) {
      assert.equal(typeof d[key], "number");
      assert.ok((d[key] as number) >= 0);
    }
    assert.ok(d.seenTotal! >= 1); // earlier tests in this file already marked items seen
  });

  it("no token -> 401", async () => {
    const { status } = await api("/stats");
    assert.equal(status, 401);
  });
});
