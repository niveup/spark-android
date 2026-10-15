import { z } from "zod";

// Phase 3 source adapters. Each fetches a free, legal source (AGENTS.md 1.7)
// and normalizes to plain data. Network errors throw with the source name.
// Shapes below were verified live against the real APIs (Oct 2026).

const GutendexBook = z.object({
  id: z.number(),
  title: z.string(),
  authors: z.array(z.object({ name: z.string() })).default([]),
  languages: z.array(z.string()).default([]),
  copyright: z.boolean().nullable().default(null),
  formats: z.record(z.string(), z.string()).default({}),
});

const GutendexResp = z.object({ results: z.array(GutendexBook).default([]) });

export type GutendexBook = {
  gutenbergId: number;
  title: string;
  author: string;
  language: string;
  textUrl: string | null;
  coverUrl: string | null;
};

export async function gutendexSearch(query: string, limit = 5): Promise<GutendexBook[]> {
  const res = await fetch(`https://gutendex.com/books/?search=${encodeURIComponent(query)}`);
  if (!res.ok) throw new Error(`gutendex: HTTP ${res.status}`);
  const parsed = GutendexResp.safeParse(await res.json());
  if (!parsed.success) throw new Error("gutendex: unexpected shape");
  return parsed.data.results.slice(0, limit).map((b) => ({
    gutenbergId: b.id,
    title: b.title,
    author: b.authors.map((a) => a.name).join(", "),
    language: b.languages[0] ?? "en",
    textUrl: b.formats["text/plain; charset=utf-8"] ?? b.formats["text/plain"] ?? null,
    coverUrl: b.formats["image/jpeg"] ?? null,
  }));
}

const OpenLibraryResp = z.object({
  docs: z
    .array(
      z.object({
        key: z.string(),
        title: z.string(),
        author_name: z.array(z.string()).optional(),
        cover_i: z.number().optional(),
        first_publish_year: z.number().optional(),
      }),
    )
    .default([]),
});

export type OpenLibraryWork = {
  key: string;
  title: string;
  author: string;
  coverUrl: string | null;
};

export async function openLibrarySearch(query: string, limit = 5): Promise<OpenLibraryWork[]> {
  const url =
    `https://openlibrary.org/search.json?q=${encodeURIComponent(query)}` +
    `&limit=${limit}&fields=key,title,author_name,cover_i,first_publish_year`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`openlibrary: HTTP ${res.status}`);
  const parsed = OpenLibraryResp.safeParse(await res.json());
  if (!parsed.success) throw new Error("openlibrary: unexpected shape");
  return parsed.data.docs.map((d) => ({
    key: d.key,
    title: d.title,
    author: d.author_name?.join(", ") ?? "",
    coverUrl: d.cover_i ? `https://covers.openlibrary.org/b/id/${d.cover_i}-L.jpg` : null,
  }));
}

const LibrivoxResp = z.union([
  z.object({ books: z.array(z.object({
    id: z.string(),
    title: z.string(),
    description: z.string().nullable().default(null),
    url_librivox: z.string(),
    url_rss: z.string().optional(),
    totaltime: z.string().optional(),
    authors: z.array(z.object({ first_name: z.string(), last_name: z.string() })).default([]),
  })) }),
  z.object({ error: z.string() }),
]);

export type LibrivoxBook = {
  librivoxId: string;
  title: string;
  author: string;
  pageUrl: string;
  rssUrl: string | null;
};

export async function librivoxSearch(author: string, limit = 5): Promise<LibrivoxBook[]> {
  const res = await fetch(
    `https://librivox.org/api/feed/audiobooks/?author=${encodeURIComponent(author)}&format=json&limit=${limit}`,
  );
  if (!res.ok) throw new Error("librivox: HTTP ${res.status}");
  const parsed = LibrivoxResp.safeParse(await res.json());
  if (!parsed.success || "error" in parsed.data) return [];
  return parsed.data.books.slice(0, limit).map((b) => ({
    librivoxId: b.id,
    title: b.title,
    author: b.authors.map((a) => `${a.first_name} ${a.last_name}`.trim()).join(", "),
    pageUrl: b.url_librivox,
    rssUrl: b.url_rss ?? null,
  }));
}

const WikiSummary = z.object({
  title: z.string(),
  extract: z.string(),
  content_urls: z.object({ desktop: z.object({ page: z.string() }) }),
});

export type WikiTopic = { title: string; extract: string; pageUrl: string };

export async function wikiSummary(topic: string): Promise<WikiTopic | null> {
  const res = await fetch(
    `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(topic)}`,
  );
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`wiki: HTTP ${res.status}`);
  const parsed = WikiSummary.safeParse(await res.json());
  if (!parsed.success) throw new Error("wiki: unexpected shape");
  return {
    title: parsed.data.title,
    extract: parsed.data.extract,
    pageUrl: parsed.data.content_urls.desktop.page,
  };
}
