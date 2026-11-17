async function main() {
  const res = await fetch("https://openlibrary.org/works/OL3792044W/editions.json?limit=30");
  const d = await res.json() as any;
  for (const ed of d.entries || []) {
    if (ed.covers && ed.covers.length > 0) {
      console.log(`Science of Getting Rich -> ${ed.title} cover: https://covers.openlibrary.org/b/id/${ed.covers[0]}-L.jpg`);
    }
  }
}
main();
