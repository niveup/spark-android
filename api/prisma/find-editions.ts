async function searchOlEditions(workId: string) {
  const res = await fetch(`https://openlibrary.org/works/${workId}/editions.json?limit=10`);
  const data = await res.json() as any;
  for (const ed of data.entries || []) {
    if (ed.covers && ed.covers.length > 0) {
      console.log(`Edition: ${ed.title} cover: https://covers.openlibrary.org/b/id/${ed.covers[0]}-L.jpg`);
    }
  }
}

async function searchWork(title: string) {
  const res = await fetch(`https://openlibrary.org/search.json?title=${encodeURIComponent(title)}&limit=1`);
  const d = await res.json() as any;
  const doc = d.docs?.[0];
  console.log(`Work: ${title} key: ${doc?.key}`);
  if (doc?.key) {
    const workKey = doc.key.replace("/works/", "");
    await searchOlEditions(workKey);
  }
}

async function main() {
  await searchWork("The Science of Getting Rich");
  await searchWork("As a Man Thinketh");
  await searchWork("Acres of Diamonds");
  await searchWork("Self-Help Samuel Smiles");
}

main();
