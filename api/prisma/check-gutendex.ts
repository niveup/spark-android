async function main() {
  const res = await fetch("https://gutendex.com/books/?search=The+Science+of+Getting+Rich");
  const d = await res.json() as any;
  console.log("Gutendex formats:", d.results?.[0]?.formats);
}
main();
