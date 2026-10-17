// SRS scheduler v1 (server-side only). The app reports quizCorrect; the
// server computes and stores the due dates. Fixed steps in days; a wrong
// answer resets to tomorrow.
export const SRS_STEPS = [1, 3, 7, 14, 30, 60]; // days

export function nextSrsDue(
  currentInterval: number | null,
  correct: boolean,
): { due: Date; interval: number } {
  let interval: number;
  if (!correct) {
    interval = 1;
  } else if (currentInterval == null) {
    interval = SRS_STEPS[0]!;
  } else {
    const idx = SRS_STEPS.indexOf(currentInterval);
    interval = SRS_STEPS[Math.min(idx + 1, SRS_STEPS.length - 1)]!;
  }
  return { due: new Date(Date.now() + interval * 24 * 60 * 60 * 1000), interval };
}
