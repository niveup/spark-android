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
  if (!res.ok) throw new Error(`librivox: HTTP ${res.status}`);
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

// --- Phase 3b additions: pinned-ID sources, LibriVox chapters, Gutenberg text ---

export function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

// Flaky upstreams (Gutendex especially): 3 tries with backoff.
export async function withRetry<T>(fn: () => Promise<T>, attempts = 3, baseDelayMs = 1200): Promise<T> {
  let lastError: unknown = new Error("no attempt");
  for (let i = 0; i < attempts; i++) {
    try {
      return await fn();
    } catch (e) {
      lastError = e;
      if (i < attempts - 1) await sleep(baseDelayMs * (i + 1));
    }
  }
  throw lastError;
}

export async function gutendexById(id: number): Promise<GutendexBook | null> {
  const res = await withRetry(() => fetch(`https://gutendex.com/books/${id}`));
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`gutendex: HTTP ${res.status}`);
  const parsed = GutendexBook.safeParse(await res.json());
  if (!parsed.success) throw new Error("gutendex: unexpected shape");
  const b = parsed.data;
  return {
    gutenbergId: b.id,
    title: b.title,
    author: b.authors.map((a) => a.name).join(", "),
    language: b.languages[0] ?? "en",
    textUrl: b.formats["text/plain; charset=utf-8"] ?? b.formats["text/plain"] ?? null,
    coverUrl: b.formats["image/jpeg"] ?? null,
  };
}

// Gutenberg plain text (transient, in-memory only — never stored to disk).
// Cuts the full page after the "*** START OF THE PROJECT GUTENBERG EBOOK ... ***"
// marker (verified present in pg4507/pg935/pg20203/pg34258/pg59844/pg45109),
// then trims to `limit` chars so we never hold whole multi-MB books longer
// than needed.
export async function gutenbergBody(id: number, limit = 4000): Promise<string | null> {
  const res = await fetch(
    `https://www.gutenberg.org/cache/epub/${id}/pg${id}.txt`,
    { headers: { "User-Agent": "spark-dev-ingest/0.1 (personal app; contact local)" } },
  );
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`gutenberg: HTTP ${res.status}`);
  const text = await res.text();
  const marker = /\*\*\*\s*START OF THE PROJECT GUTENBERG[^*]+\*{3}/i;
  const m = marker.exec(text);
  const body = m !== null && m.index >= 0 ? text.slice(m.index + m[0].length) : text;
  return body.replace(/^[ \n\r\t]+/, "").slice(0, limit);
}

const LibrivoxDetailed = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string().nullable().default(null),
  language: z.string().optional(),
  num_sections: z.string().optional(),
  totaltimesecs: z.number().optional(),
  totaltime: z.string().optional(),
  copyright_year: z.string().optional(),
  url_text_source: z.string().nullable().optional(),
  url_librivox: z.string(),
  url_rss: z.string().optional(),
  url_zip_file: z.string().nullable().optional(),
  url_iarchive: z.string().nullable().optional(),
  authors: z.array(z.object({ first_name: z.string(), last_name: z.string() })).default([]),
  sections: z
    .array(
      z.object({
        section_number: z.string(),
        title: z.string(),
        listen_url: z.string(),
        playtime: z.string().or(z.number()).optional(),
      }),
    )
    .default([]),
});

const LibrivoxDetailedWrapper = z.union([
  z.object({ books: z.array(LibrivoxDetailed) }),
  z.object({ error: z.string() }),
]);

export type LibrivoxSection = {
  sectionNumber: number;
  title: string;
  listenUrl: string;
  playtimeSec: number;
};

export type LibrivoxDetailedBook = {
  librivoxId: string;
  title: string;
  description: string | null;
  author: string;
  gutenbergId: number | null;
  pageUrl: string;
  zipUrl: string | null;
  totalSeconds: number | null;
  numSections: number | null;
  sections: LibrivoxSection[];
};

export async function librivoxBook(id: string | number): Promise<LibrivoxDetailedBook | null> {
  const res = await withRetry(() =>
    fetch(
      `https://librivox.org/api/feed/audiobooks/?id=${encodeURIComponent(id)}&format=json&extended=1`,
    ),
  );
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`librivox: HTTP ${res.status}`);
  const parsed = LibrivoxDetailedWrapper.safeParse(await res.json());
  if (!parsed.success || "error" in parsed.data) return null;
  const b = parsed.data.books[0];
  if (b === undefined) return null;
  const splitPlaytime = (p: string | number | undefined): number =>
    typeof p === "number" ? p : Number(p ?? NaN);
  const gutenberg = b.url_text_source?.match(/e?text\/(\d+)/) ?? b.url_text_source?.match(/ebooks\/(\d+)/);
  return {
    librivoxId: b.id,
    title: b.title,
    description: b.description,
    author: b.authors.map((a) => `${a.first_name} ${a.last_name}`.trim()).join(", "),
    gutenbergId: gutenberg ? Number(gutenberg[1]) : null,
    pageUrl: b.url_librivox,
    zipUrl: b.url_zip_file ?? null,
    totalSeconds: b.totaltimesecs ?? null,
    numSections: b.num_sections ? Number(b.num_sections) : null,
    sections: b.sections
      .filter((s) => !Number.isNaN(splitPlaytime(s.playtime)))
      .map((s) => ({
        sectionNumber: Number(s.section_number),
        title: s.title.trim(),
        listenUrl: s.listen_url,
        playtimeSec: splitPlaytime(s.playtime),
      })),
  };
}

// Title search (author optionally appended), for resolving pinned titles.
// Returns candidates for the caller/eyeball layer to match against.
export async function librivoxTitleSearch(
  title: string,
  limit = 8,
): Promise<LibrivoxDetailedBook[]> {
  const url =
    `https://librivox.org/api/feed/audiobooks/?title=${encodeURIComponent(title)}` +
    `&format=json&limit=${limit}`;
  const res = await withRetry(() => fetch(url));
  if (!res.ok) throw new Error(`librivox: HTTP ${res.status}`);
  const parsed = LibrivoxDetailedWrapper.safeParse(await res.json());
  if (!parsed.success || "error" in parsed.data) return [];
  return parsed.data.books.map((b) => {
    const gutenberg = b.url_text_source?.match(/e?text\/(\d+)/);
    return {
      librivoxId: b.id,
      title: b.title,
      description: b.description,
      author: b.authors.map((a) => `${a.first_name} ${a.last_name}`.trim()).join(", "),
      gutenbergId: gutenberg ? Number(gutenberg[1]) : null,
      pageUrl: b.url_librivox,
      zipUrl: b.url_zip_file ?? null,
      totalSeconds: b.totaltimesecs ?? null,
      numSections: b.num_sections ? Number(b.num_sections) : null,
      sections: [],
    };
  });
}
