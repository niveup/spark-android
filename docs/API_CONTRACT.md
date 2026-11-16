# docs/API_CONTRACT.md — SPARK mobile API v1 (frozen for Phase 2 build)

Base: `https://<host>/api/mobile/v1`. All JSON. Times are UTC ISO-8601 strings.
IDs are opaque strings (cuid/uuid, server-chosen). Single user: no user id in URLs.

## Conventions

- Auth (all except `POST /auth/login`, `GET /health`): header
  `Authorization: Bearer <raw-token>`. Server stores only SHA-256 hash.
- Success: `200` (+ `201` on create) with `{ "data": ... }`.
- Failure: `{ "error": { "code": ..., "message": "..." } }`, never a stack trace.
  Codes: `UNAUTHORIZED` 401 · `VALIDATION` 400 · `NOT_FOUND` 404 ·
  `RATE_LIMITED` 429 · `CONFLICT` 409 · `INTERNAL` 500.
- Pagination: `?limit=` (default 20, max 50) + `?cursor=` (opaque, from prior
  `nextCursor`). Response: `{ "data": [...], "nextCursor": "abc" | null }`.
- Rate limits: login 5/min per IP; writes 60/min per token; feed 30/min per token.
- The app validates every response with kotlinx.serialization + manual checks and
  treats unknown enum values as "skip this item, keep scrolling".

## Types

```ts
FeedItemType = "book_idea" | "summary" | "quote" | "concept" | "quiz" | "audio" | "challenge"
ItemStatus = "published" // app only ever sees published
FeedItem = {
  id: string, type: FeedItemType, title: string,       // title ≤ 80 chars
  bodyMd: string,                                      // ≤ 600 chars, plain markdown subset
  actionLine: string,                                  // may be ""
  topicTags: string[], language: string,               // e.g. "en"
  estSeconds: number,                                   // reading cards 10–600; audio cards up to 7200 (true listen length)
  progressMs: number | null,                            // audiobook resume, Phase 6+
  quiz: null | { options: string[],                     // quiz cards only (type=quiz),
                 correctIndex: number,                  // else null; options 2–4,
                 explanation: string },                 // correctIndex is 0-based
  audioUrl: string | null,                              // audio cards only (type=audio),
  durationMs: number | null,                            // else null; stream, never re-host
  book: null | { id: string, title: string, author: string, coverUrl: string | null },
  sourceType: string, sourceRef: string, license: string,
  isAiGenerated: boolean, isGrounded: boolean,         // false,false never occurs
  // per-device interaction state (additive v1.1, present on /feed and /items/saved):
  saved: boolean, liked: boolean | null,               // null = no rating
  dismissed: boolean,
  publishedAt: string
}
ItemState = {
  itemId: string, seenAt: string | null,
  saved: boolean, liked: boolean | null,               // null = no rating
  dismissed: boolean, dwellMs: number,
  // quiz/SRS fields, server-computed, read-only for app:
  srsDueAt: string | null
}
```

## Endpoints

### POST /auth/login — exchange passcode for token
Request: `{ "passcode": "string, 1..128 chars", "deviceLabel": "string, 1..64 chars" }`
Response `200`: `{ "data": { "token": "<raw, shown once>", "expiresAt": "string" } }`
(expires 90 days after login; matches ARCHITECTURE.md token section).
Errors: `UNAUTHORIZED` (wrong passcode — same message every time), `RATE_LIMITED`.
Note: passcode lives in server env only. Token is random 256-bit, hex/base64url.

### POST /auth/logout — revoke current token
Auth required. Response `200`: `{ "data": { "revoked": true } }`.
Also revokes on: user rotates passcode (all tokens), explicit revoke (see below).

### GET /auth/tokens — list devices (lost-phone case)
Auth required. Response `200`:
`{ "data": [{ "id": string, "deviceLabel": string, "createdAt": string, "lastUsedAt": string | null }] }`.
Raw tokens never listed. Used by Settings to show "logged-in devices".

### DELETE /auth/tokens/:id — revoke one device
Auth required. Response `200`: `{ "data": { "revoked": true } }`.
Unknown/foreign id → `NOT_FOUND`. Revoking the current token logs the app out
(app treats `UNAUTHORIZED` mid-session as "logged out elsewhere" → Login screen).

### GET /feed — ranked session feed, cursor pages
Auth required. Query: `limit`, `cursor`, `sessionId=` (from `POST /sessions`,
optional but recommended), `excludeSeen=true` (default; `false` refills
from saved/older), `topics=` (comma list, optional boost filter).
Response `200`: `{ "data": FeedItem[], "nextCursor": string | null }`.
Ranking (server, v1, no ML): SRS-due first → unseen from liked/saved topics →
~20% exploration → never seen-within-30d unless saved (a due review resurfaces
even if recently seen — rule 1 outranks rule 4, or SRS could never fire).
Deterministic per token
within a session (seeded by `sessionId` when given, otherwise by token) so pages
don't reshuffle mid-scroll.

### POST /items/:id/state — upsert interaction state (single)
Auth required. Request (all fields optional except at least one):
`{ "seen": boolean, "saved": boolean, "liked": boolean | null,
   "dismissed": boolean, "dwellMs": number, "quizCorrect": boolean | null,
   "clientEventId": string }` (opaque, app-generated UUID per tap/swipe;
optional but recommended — replays with the same id are ignored so `dwellMs`
never double-counts).
Response `200`: `{ "data": ItemState }`. Unknown item id → `NOT_FOUND`.
`quizCorrect` feeds the SRS scheduler; app never sends SRS dates.

### POST /items/state — batch upsert (offline sync + swipe batching)
Auth required. Request: `{ "updates": [{ "itemId": string, "clientEventId": string,
...same fields... }] }` (max 100, `clientEventId` required per update).
Response `200`: `{ "data": { "states": ItemState[], "rejected": string[] } }`.
Idempotent: replaying the same batch changes nothing the second time.
Last-write-wins per field; `dwellMs` sums (never double-counts on replay —
server keys on `(token, itemId, clientEventId)`; replays with the same
`clientEventId` are ignored).

### GET /items/saved — saved list for Saved tab
Auth required. Query: `limit`, `cursor`, `q=` (substring search over title/body).
Response `200`: `{ "data": FeedItem[], "nextCursor": string | null }`,
most-recently-saved first.

### GET /items/:id — one card by id (saved card → Reels)
Auth required. Response `200`: `{ "data": FeedItem }` — the published card with
the caller's own state (`saved`, `liked`, `dismissed`, `progressMs`).
Unknown, draft or archived id → `NOT_FOUND`.
The Saved tab stores only the card id; opening a saved card in the Reels tab
maps that id back to a card with this endpoint (offline the app looks the same
id up in its local cache instead).

### PUT /items/:id/progress — audiobook resume position (Phase 6)
Auth required. Request: `{ "positionMs": number, "durationMs": number }`
(`positionMs <= durationMs`, both ≥ 0).
Response `200`: `{ "data": { "itemId": string, "positionMs": number, "updatedAt": string } }`.
Unknown item id → `NOT_FOUND`. Last-write-wins; the app sends it at most every
10 seconds and always on pause/kill, so conflicts are rare and harmless.
GET side: progress rides inside `GET /feed` / `GET /items/saved` items as
`progressMs: number | null` (added to `FeedItem`, optional, default null).

### POST /sessions — open a feed session
Auth required. Request: `{ "deviceId": "string", "cardLimit": number, "minuteLimit": number }`.
Response `201`: `{ "data": { "sessionId": string, "startedAt": "string" } }`.

### PATCH /sessions/:id — close a session
Auth required. Request: `{ "itemsSeen": number, "limitReached": boolean }`.
Response `200`: `{ "data": { "sessionId": string, "endedAt": "string" } }`.
Unknown/foreign session id → `NOT_FOUND`.

### GET /stats — personal totals (additive v1.1, used by the Me tab)
Auth required. Response `200`:
`{ "data": { "seenTotal": number, "savedTotal": number, "likedTotal": number,
             "quizzesDue": number, "sessionsTotal": number } }`.
Item totals are token-scoped; `quizzesDue` counts items whose SRS due date has
passed; `sessionsTotal` counts closed sessions (single-user app: all of them).

### GET /health — unauthenticated probe
Response `200`: `{ "data": { "ok": true, "version": "v1" } }`. Used by the app's
offline banner to distinguish "server down" from "phone offline".

## Compatibility promises (v1 frozen)

- Additive only: new optional fields/endpoints; never rename, retype, or delete.
- `isGrounded=false` always pairs with `isAiGenerated=true`; app shows
  "AI-written, unverified" whenever `isGrounded` is false.
- Breaking change ⇒ new `/api/mobile/v2/` prefix; v1 keeps working.
