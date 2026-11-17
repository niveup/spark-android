async function inspectImage(name: string, url: string) {
  try {
    const res = await fetch(url);
    const buf = await res.arrayBuffer();
    console.log(`${name}: ${buf.byteLength} bytes (${url})`);
  } catch (e: any) {
    console.log(`${name} error:`, e.message);
  }
}

async function main() {
  await inspectImage("Meditations (StandardEbooks)", "https://standardebooks.org/ebooks/marcus-aurelius/meditations/george-long/downloads/cover.jpg");
  await inspectImage("Benjamin Franklin (StandardEbooks)", "https://standardebooks.org/ebooks/benjamin-franklin/the-autobiography-of-benjamin-franklin/downloads/cover.jpg");
  await inspectImage("Frankenstein (StandardEbooks)", "https://standardebooks.org/ebooks/mary-shelley/frankenstein/downloads/cover.jpg");
  await inspectImage("Pride and Prejudice (StandardEbooks)", "https://standardebooks.org/ebooks/jane-austen/pride-and-prejudice/downloads/cover.jpg");
  await inspectImage("Enchiridion (StandardEbooks)", "https://standardebooks.org/ebooks/epictetus/the-enchiridion/elizabeth-carter/downloads/cover.jpg");
  await inspectImage("As a Man Thinketh 15259383", "https://covers.openlibrary.org/b/id/15259383-L.jpg");
  await inspectImage("Science of Getting Rich 2889500", "https://covers.openlibrary.org/b/id/2889500-L.jpg");
  await inspectImage("Science of Getting Rich 2098726", "https://covers.openlibrary.org/b/id/2098726-L.jpg");
  await inspectImage("How to Live 24 Hours", "https://upload.wikimedia.org/wikipedia/commons/7/7f/How_to_Live_on_24_Hours_a_Day.png");
  await inspectImage("Acres of Diamonds", "https://covers.openlibrary.org/b/id/5833160-L.jpg");
}

main();
