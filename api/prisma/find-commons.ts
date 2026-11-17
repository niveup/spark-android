async function searchCommons(query: string) {
  try {
    const url = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=${encodeURIComponent(query)}&gsrlimit=3&prop=imageinfo&iiprop=url|size|mime&format=json`;
    const res = await fetch(url);
    const data = await res.json() as any;
    const pages = data.query?.pages || {};
    for (const id of Object.keys(pages)) {
      const info = pages[id].imageinfo?.[0];
      if (info && (info.mime === "image/jpeg" || info.mime === "image/png")) {
        console.log(`[Commons] ${query} -> ${info.width}x${info.height} (${info.size} bytes): ${info.url}`);
      }
    }
  } catch (e: any) {
    console.error(e.message);
  }
}

async function main() {
  await searchCommons("Science of Getting Rich Wallace Wattles cover");
  await searchCommons("As a Man Thinketh James Allen book cover");
  await searchCommons("How to live on 24 hours a day Arnold Bennett cover");
  await searchCommons("Acres of Diamonds Russell Conwell book cover");
  await searchCommons("Self-Help Samuel Smiles cover");
}

main();
