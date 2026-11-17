async function checkUrl(url: string) {
  try {
    const res = await fetch(url, { method: "HEAD" });
    console.log(url, "Status:", res.status, "Type:", res.headers.get("content-type"), "Len:", res.headers.get("content-length"));
    return res.status === 200;
  } catch (e: any) {
    console.log(url, "Error:", e.message);
    return false;
  }
}

async function main() {
  const books = [
    "marcus-aurelius/meditations/george-long",
    "james-allen/as-a-man-thinketh",
    "benjamin-franklin/the-autobiography-of-benjamin-franklin",
    "mary-shelley/frankenstein",
    "jane-austen/pride-and-prejudice",
    "arnold-bennett/how-to-live-on-twenty-four-hours-a-day",
    "epictetus/the-enchiridion/elizabeth-carter",
  ];
  for (const b of books) {
    const url = `https://standardebooks.org/ebooks/${b}/downloads/cover.jpg`;
    await checkUrl(url);
  }
}

main();
