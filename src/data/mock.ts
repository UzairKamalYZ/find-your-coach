// Coaches, venues and bookings now come from the backend API (see src/hooks/use-api.ts).
// This list is only a fallback so sport filters render before GET /api/v1/sports responds.

export type Sport = "Tennis" | "Football" | "Cricket" | "Basketball" | "Badminton" | "Swimming";

export const SPORTS: { name: Sport; emoji: string }[] = [
  { name: "Tennis", emoji: "🎾" },
  { name: "Football", emoji: "⚽" },
  { name: "Cricket", emoji: "🏏" },
  { name: "Basketball", emoji: "🏀" },
  { name: "Badminton", emoji: "🏸" },
  { name: "Swimming", emoji: "🏊" },
];
