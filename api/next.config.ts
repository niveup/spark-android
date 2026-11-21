import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // API only: no pages, no image optimization needed.
  outputFileTracingIncludes: {
    "/api/**/*": [
      "./public/subtitles/**/*",
      "./src/lib/audio_manifests/**/*",
      "./src/lib/chapters_48laws_manifest.json",
    ],
  },
};

export default nextConfig;
