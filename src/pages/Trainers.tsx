import { useMemo, useState } from "react";
import Navbar from "@/components/Navbar";
import TrainerCard from "@/components/TrainerCard";
import { SPORTS, TRAINERS } from "@/data/mock";
import { Search } from "lucide-react";

const Trainers = () => {
  const [sport, setSport] = useState<string>("All");
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    return TRAINERS.filter((t) => {
      const matchSport = sport === "All" || t.sport === sport;
      const q = query.toLowerCase();
      const matchQuery = !q || t.name.toLowerCase().includes(q) || t.city.toLowerCase().includes(q);
      return matchSport && matchQuery;
    });
  }, [sport, query]);

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <section className="bg-hero pb-12 pt-32 text-primary-foreground md:pb-16 md:pt-40">
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Browse</p>
          <h1 className="mt-2 font-display text-6xl md:text-7xl">All Coaches</h1>
          <p className="mt-3 max-w-xl text-primary-foreground/80">Filter by sport, search by name or city, and book your next session.</p>
        </div>
      </section>

      <section className="border-b border-border bg-card">
        <div className="container flex flex-col gap-4 py-6 md:flex-row md:items-center">
          <div className="flex flex-1 items-center gap-3 rounded-xl border border-border bg-background px-4 py-3">
            <Search className="h-5 w-5 text-accent" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by name or city..."
              className="w-full bg-transparent font-semibold placeholder:text-muted-foreground focus:outline-none"
            />
          </div>
          <div className="flex gap-2 overflow-x-auto">
            {["All", ...SPORTS.map((s) => s.name)].map((s) => (
              <button
                key={s}
                onClick={() => setSport(s)}
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
        {filtered.length === 0 ? (
          <p className="py-20 text-center text-muted-foreground">No coaches match your filters.</p>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((t, i) => <TrainerCard key={t.id} trainer={t} index={i} />)}
          </div>
        )}
      </section>
    </div>
  );
};

export default Trainers;
