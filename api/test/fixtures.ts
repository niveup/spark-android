// Shared test fixture: a card that satisfies CardSchema in every optional
// field, so each test can override exactly one property.
export const BASE_CARD = {
  type: "concept",
  title: "The 2-Minute Rule",
  bodyMd: "Shrink any habit until starting costs two minutes.",
  actionLine: "Try the two-minute version.",
  topicTags: ["habits"],
  language: "en",
  estSeconds: 45,
  sourceType: "manual",
  sourceRef: "growth deck",
  license: "original",
  isAiGenerated: false,
  isGrounded: true,
  modelUsed: null,
  quiz: null,
  audioUrl: null,
  durationMs: null,
};
