import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import Navbar from "@/components/Navbar";
import TrainerCard from "@/components/TrainerCard";
import { useSports, useTrainers } from "@/hooks/use-api";
import { Search } from "lucide-react";

const SORTS = [
  ["recommended", "Recommended"],
  ["rating", "Top rated"],
  ["price_asc", "Price: low to high"],
  ["price_desc", "Price: high to low"],
  ["experience", "Most experienced"],
] as const;

const Trainers = () => {
  const [params, setParams] = useSearchParams();
  const sport = params.get("sport") || "All";
  const sort = params.get("sort") || "recommended";
  const [query, setQuery] = useState(params.get("q") ?? "");
  const [debounced, setDebounced] = useState(query);
  const { sports } = useSports();

  useEffect(() => {
    const id = setTimeout(() => setDebounced(query.trim()), 250);
    return () => clearTimeout(id);
  }, [query]);

  const update = (key: string, value: string | null) => {
    const next = new URLSearchParams(params);
    if (!value || value === "All" || value === "recommended") next.delete(key);
    else next.set(key, value);
    setParams(next, { replace: true });
  };
  useEffect(() => { update("q", debounced || null); }, [debounced]); // eslint-disable-line react-hooks/exhaustive-deps

  const { data, isLoading, isError, refetch } = useTrainers({ sport: sport === "All" ? undefined : sport, q: debounced || undefined, sort });
  const trainers = data?.content ?? [];

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <section className="bg-hero pb-12 pt-32 text-primary-foreground md:pb-16 md:pt-40">
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Browse</p>
          <h1 className="mt-2 font-display text-6xl md:text-7xl">All Coaches</h1>
          <p className="mt-3 max-w-xl text-primary-foreground/80">Filter by sport, search by name, city or specialty, and book your next session.</p>
        </div>
      </section>

      <section className="border-b border-border bg-card">
        <div className="container flex flex-col gap-4 py-6 md:flex-row md:items-center">
          <div className="flex flex-1 items-center gap-3 rounded-xl border border-border bg-background px-4 py-3">
            <Search className="h-5 w-5 text-accent" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by name, city or specialty..."
              aria-label="Search coaches"
              className="w-full bg-transparent font-semibold placeholder:text-muted-foreground focus:outline-none"
            />
          </div>
          <select value={sort} onChange={(e) => update("sort", e.target.value)} aria-label="Sort coaches"
            className="h-12 rounded-xl border border-border bg-background px-3 text-sm font-semibold text-primary">
            {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
          <div className="flex gap-2 overflow-x-auto">
            {["All", ...sports.map((s) => s.name)].map((s) => (
              <button
                key={s}
                onClick={() => update("sport", s)}
                className={`shrink-0 rounded-full border px-4 py-2 text-xs font-bold uppercase tracking-wider transition-all ${
                  sport === s
                    ? "border-accent bg-gradient-accent text-primary shadow-glow"
                    : "border-border bg-background text-primary hover:border-accent"
                }`}
              >
                {s}
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="container py-12 md:py-16">
        {isLoading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => <div key={i} className="aspect-[4/5] animate-pulse rounded-2xl bg-muted" />)}
          </div>
        ) : isError ? (
          <p className="py-20 text-center text-muted-foreground">Couldn't load coaches. <button className="font-semibold text-accent underline" onClick={() => refetch()}>Try again</button></p>
        ) : trainers.length === 0 ? (
          <p className="py-20 text-center text-muted-foreground">No coaches match your filters.</p>
        ) : (
          <>
            <p className="mb-6 text-sm text-muted-foreground">{data?.totalElements} coach{data?.totalElements === 1 ? "" : "es"}</p>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {trainers.map((t, i) => <TrainerCard key={t.id} trainer={t} index={i} />)}
            </div>
          </>
        )}
      </section>
    </div>
  );
};

export default Trainers;
