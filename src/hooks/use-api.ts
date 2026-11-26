import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { DayAvailability, Page, PublicStats, Review, ReviewSummary, Sport, Trainer, Venue, VenueDayAvailability } from "@/lib/types";
import { SPORTS } from "@/data/mock";

export type TrainerFilters = { sport?: string; q?: string; city?: string; sort?: string; maxRate?: number; minRating?: number; featured?: boolean; size?: number };

export const useTrainers = (f: TrainerFilters = {}) =>
  useQuery({
    queryKey: ["trainers", f],
    queryFn: () => api<Page<Trainer>>("/trainers", { auth: false, query: { size: 50, ...f } }),
  });

export const useTrainer = (id?: string) =>
  useQuery({ queryKey: ["trainer", id], enabled: !!id, retry: false, queryFn: () => api<Trainer>(`/trainers/${id}`, { auth: false }) });

export const useTrainerAvailability = (id: string, from?: string, to?: string) =>
  useQuery({
    queryKey: ["availability", id, from, to],
    queryFn: () => api<DayAvailability[]>(`/trainers/${id}/availability`, { auth: false, query: { from, to } }),
  });

export const useTrainerReviews = (id: string) =>
  useQuery({ queryKey: ["reviews", id], queryFn: () => api<Page<Review>>(`/trainers/${id}/reviews`, { auth: false, query: { size: 20 } }) });

export const useReviewSummary = (id: string) =>
  useQuery({ queryKey: ["review-summary", id], queryFn: () => api<ReviewSummary>(`/trainers/${id}/reviews/summary`, { auth: false }) });

export const useVenues = (f: { sport?: string; q?: string; type?: string; sort?: string; size?: number } = {}) =>
  useQuery({ queryKey: ["venues", f], queryFn: () => api<Page<Venue>>("/venues", { auth: false, query: { size: 50, ...f } }) });

export const useVenueAvailability = (id: string | undefined, from?: string, to?: string) =>
  useQuery({
    queryKey: ["venue-availability", id, from, to],
    enabled: !!id,
    queryFn: () => api<VenueDayAvailability[]>(`/venues/${id}/availability`, { auth: false, query: { from, to } }),
  });

/** Active sports from the API, falling back to the built-in list so filters render instantly. */
export const useSports = () => {
  const q = useQuery({ queryKey: ["sports"], staleTime: 5 * 60_000, queryFn: () => api<Sport[]>("/sports", { auth: false }) });
  const sports = q.data?.length ? q.data.map((s) => ({ name: s.name, emoji: s.emoji ?? "🏅" })) : SPORTS;
  return { ...q, sports };
};

export const usePublicStats = () =>
  useQuery({ queryKey: ["public-stats"], staleTime: 60_000, queryFn: () => api<PublicStats>("/stats", { auth: false }) });
