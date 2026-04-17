import { useMemo, useState } from "react";
import Navbar from "@/components/Navbar";
import VenueCard from "@/components/VenueCard";
import { SPORTS, VENUES } from "@/data/mock";

const Venues = () => {
  const [sport, setSport] = useState<string>("All");
  const filtered = useMemo(
    () => VENUES.filter((v) => sport === "All" || v.sport === sport),
    [sport]
  );

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <section className="bg-hero pb-12 pt-32 text-primary-foreground md:pb-16 md:pt-40">
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Train anywhere</p>
          <h1 className="mt-2 font-display text-6xl md:text-7xl">Courts, Pitches & Clubs</h1>
          <p className="mt-3 max-w-xl text-primary-foreground/80">Book premium venues across the country, by the hour.</p>
        </div>
      </section>

      <section className="border-b border-border bg-card">
        <div className="container flex gap-2 overflow-x-auto py-6">
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
      </section>

      <section className="container py-12 md:py-16">
        {filtered.length === 0 ? (
          <p className="py-20 text-center text-muted-foreground">No venues for this sport yet.</p>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((v, i) => <VenueCard key={v.id} venue={v} index={i} />)}
          </div>
        )}
      </section>
    </div>
  );
};

export default Venues;
