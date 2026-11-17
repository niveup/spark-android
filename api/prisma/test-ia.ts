async function testIa(id: string) {
  const url = `https://archive.org/services/img/${id}`;
  try {
    const res = await fetch(url);
    const buf = await res.arrayBuffer();
    console.log(`IA ${id} -> ${res.status} ${buf.byteLength} bytes (${res.headers.get("content-type")})`);
  } catch (e: any) {
    console.log(e.message);
  }
}

async function main() {
  await testIa("thescienceofgettingrich");
  await testIa("scienceofgetting00watt");
  await testIa("asamanthinketh00alle");
  await testIa("acresofdiamonds00conw");
}

main();
