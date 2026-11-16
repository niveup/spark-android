// READ-ONLY content provenance report. Answers "where did every card come
// from?" straight from the DB: published deck by type and source, each card's
// sourceRef/license/AI-grounded flags, the books table, and the ingest jobs.
// Nothing is written or deleted.
//
// Usage (from api/):  node --env-file=.env prisma/deck-report.mjs
//
// Plain .mjs on purpose: the tsx runner needs to spawn its esbuild helper,
// which is blocked in this sandboxed shell.
import pg from "pg";

const client = new pg.Client({ connectionString: process.env.DATABASE_URL });
await client.connect();

const q = async (sql) => (await client.query(sql)).rows;

console.log("== status / type / count ==");
for (const r of await q(`select status, type, count(*)::int as n from feed_items group by 1,2 order by 1,2`)) {
  console.log(`  ${r.status.padEnd(10)} ${r.type.padEnd(12)} ${r.n}`);
}

console.log("\n== status / sourceType / count ==");
for (const r of await q(`select status, "sourceType", count(*)::int as n from feed_items group by 1,2 order by 1,2`)) {
  console.log(`  ${r.status.padEnd(10)} ${String(r.sourceType).padEnd(20)} ${r.n}`);
}

const pub = await q(`
  select f.type, f.title, f."sourceType", f."sourceRef", f.license, f."isAiGenerated", f."isGrounded",
         f."bodyMd", b.title as book_title
  from feed_items f left join books b on b.id = f."bookId"
  where f.status = 'published'
  order by f.type, f."createdAt"`);
console.log(`\n== published items: ${pub.length} ==`);
for (const p of pub) {
  console.log(`  [${p.type}/${p.sourceType}]${p.isAiGenerated ? " AI" : ""}${p.isGrounded ? " grounded" : ""} ${p.title}${p.book_title ? `  book=${p.book_title}` : ""}`);
  console.log(`      body: ${String(p.bodyMd).slice(0, 120).replace(/\n/g, " ")}`);
  console.log(`      src : ${p.sourceRef} | ${p.license}`);
}

console.log("\n== books ==");
for (const b of await q(`select title, source, "sourceId", license from books order by title`)) {
  console.log(`  ${b.title} (${b.source} #${b.sourceId}) ${b.license}`);
}

console.log("\n== ingest jobs ==");
for (const j of await q(`select source, status, "itemsCreated", "startedAt" from ingest_jobs order by "startedAt" desc limit 25`)) {
  console.log(`  ${new Date(j.startedAt).toISOString().slice(0, 10)} ${String(j.source).padEnd(30)} ${j.status} created=${j.itemsCreated}`);
}

await client.end();
