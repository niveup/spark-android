import "dotenv/config";

async function main() {
  const res = await fetch("https://openlibrary.org/search.json?q=How+to+Live+on+24+Hours+a+Day+Arnold+Bennett&limit=3");
  const d = await res.json() as any;
  for (const doc of d.docs || []) {
    console.log(doc.title, "by", doc.author_name?.[0], "cover_i:", doc.cover_i);
  }
}
main();
