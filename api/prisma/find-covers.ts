import "dotenv/config";

async function search(title: string) {
  try {
    const res = await fetch(`https://openlibrary.org/search.json?title=${encodeURIComponent(title)}&limit=1`);
    const d = await res.json() as any;
    const doc = d.docs?.[0];
    console.log(`${title} -> cover_i: ${doc?.cover_i}`);
  } catch (e: any) {
    console.error(title, e.message);
  }
}

async function main() {
  await search("Meditations");
  await search("How to Live on Twenty-Four Hours a Day");
  await search("Acres of Diamonds");
  await search("Self Help");
  await search("Enchiridion");
  await search("Pride and Prejudice");
  await search("Frankenstein");
}

main();
