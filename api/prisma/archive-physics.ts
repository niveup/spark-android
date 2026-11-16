// Archive (or restore) the OpenStax physics concept cards so they leave /
// re-enter the feed.
//
// Why: "Carnot efficiency", "First law of thermodynamics" and "Second law:
// entropy" came from an earlier pipeline pass (OpenStax University Physics).
// No current ingest script produces them, and thermodynamics sits outside a
// personal-growth deck, so the owner asked for them to go (Nov 2026).
//
// Safe by design (same rules as archive-audio.ts / archive-legacy-quotes.ts):
//   - Nothing is deleted: rows, content hash and saved/liked state stay put.
//   - Only `status` moves (published <-> archived). The feed serves only
//     status = 'published', so the cards leave Reels but can come back.
//   - No schema change, no migration, no destructive SQL.
//
// Usage:
//   npx tsx prisma/archive-physics.ts            # archive (default)
//   npx tsx prisma/archive-physics.ts --restore  # publish them again
//
// Plain `node prisma/archive-physics.ts` runs the same file (Node 24 strips
// the types); that is the path used when the tsx runner cannot spawn its
// esbuild helper, as in the sandboxed shell this was first run from.
import "dotenv/config";
import pg from "pg";

if (process.env.NODE_ENV === "production") {
  throw new Error("archive-physics refuses to run in production (dev DB only).");
}

const restore = process.argv.includes("--restore");
const to = restore ? "published" : "archived";
const from = restore ? "archived" : "published";

const db = new pg.Client({ connectionString: process.env.DATABASE_URL });
await db.connect();

// Scoped by sourceType as well as type, so a future *growth* concept card that
// happens to share the `concept` type is never swept up by this script.
const scope = `"sourceType" = 'openstax' and type = 'concept'`;

async function report(label: string): Promise<void> {
  const res = await db.query(
    `select status, count(*)::int as n from feed_items where ${scope} group by 1 order by 1`,
  );
  console.log(`${label}:`);
  for (const row of res.rows) console.log(`  ${row.status}: ${row.n}`);
}

async function main(): Promise<void> {
  await report("openstax concept cards before");

  const moved = await db.query(
    `update feed_items set status = $1 where ${scope} and status = $2 returning title`,
    [to, from],
  );
  console.log(`\n${moved.rowCount} card(s) moved ${from} -> ${to}.`);
  for (const row of moved.rows) console.log(`  ${row.title}`);

  await report("openstax concept cards after");

  const deck = await db.query(
    `select type, count(*)::int as n from feed_items where status = 'published' group by 1 order by 2 desc`,
  );
  console.log("\npublished deck by type:");
  for (const row of deck.rows) console.log(`  ${row.type}: ${row.n}`);
}

await main();
await db.end();
