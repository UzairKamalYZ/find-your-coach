export const money = (n: number | undefined | null) =>
  `£${Number(n ?? 0).toLocaleString("en-GB", { minimumFractionDigits: Number.isInteger(Number(n ?? 0)) ? 0 : 2, maximumFractionDigits: 2 })}`;

export const initials = (name?: string) =>
  (name ?? "").split(" ").filter(Boolean).map((w) => w[0]).slice(0, 2).join("").toUpperCase();

export const SPORT_EMOJI: Record<string, string> = {
  Tennis: "🎾", Football: "⚽", Cricket: "🏏", Basketball: "🏀", Badminton: "🏸", Swimming: "🏊",
};
