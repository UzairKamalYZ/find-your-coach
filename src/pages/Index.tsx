import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Search, Calendar, MapPin, Trophy, ArrowRight } from "lucide-react";
import Navbar from "@/components/Navbar";
import TrainerCard from "@/components/TrainerCard";
import VenueCard from "@/components/VenueCard";
import { SPORTS, TRAINERS, VENUES } from "@/data/mock";
import sportTennis from "@/assets/sport-tennis.jpg";
import sportFootball from "@/assets/sport-football.jpg";
import sportBasketball from "@/assets/sport-basketball.jpg";
import sportCricket from "@/assets/sport-cricket.jpg";

const ROTATING_SPORTS = ["Tennis", "Football", "Cricket", "Basketball", "Badminton", "Swimming"];

const HERO_PHOTOS = [
  { src: sportFootball, alt: "Footballer striking a ball under stadium lights", label: "Football" },
  { src: sportTennis, alt: "Tennis player serving on a clay court", label: "Tennis" },
  { src: sportBasketball, alt: "Basketball player dunking in an arena", label: "Basketball" },
  { src: sportCricket, alt: "Cricket batsman playing a drive at dusk", label: "Cricket" },
];

const Index = () => {
  const [sportIndex, setSportIndex] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setSportIndex((i) => (i + 1) % ROTATING_SPORTS.length), 2200);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* HERO */}
      <section className="relative isolate overflow-hidden bg-hero pb-24 pt-32 text-primary-foreground md:pb-32 md:pt-40">
        <div className="grain absolute inset-0" />
        <div className="absolute inset-0 bg-gradient-to-t from-primary via-primary/40 to-transparent" />

        <div className="container relative grid items-center gap-12 lg:grid-cols-[1.1fr_1fr]">
          <div className="animate-float-up">
            <span className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/5 px-4 py-1.5 text-xs font-bold uppercase tracking-[0.2em] text-accent backdrop-blur">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />
              Multi-sport coaching marketplace
            </span>
            <h1 className="mt-6 font-display text-6xl leading-[0.9] text-balance md:text-8xl">
              Train for<br />
              <span
                key={sportIndex}
                className="inline-block animate-float-up bg-gradient-accent bg-clip-text text-transparent"
              >
                {ROTATING_SPORTS[sportIndex]}.
              </span>
            </h1>
            <p className="mt-6 max-w-xl text-lg text-primary-foreground/80 md:text-xl">
              Discover certified coaches and book courts, pitches and stadiums for tennis, football, cricket and more — all in one place.
            </p>

            {/* Search bar */}
            <div className="mt-10 flex flex-col gap-3 rounded-2xl bg-primary-foreground/10 p-3 backdrop-blur-md md:flex-row md:items-center">
              <div className="flex flex-1 items-center gap-3 rounded-xl bg-background/95 px-4 py-3 text-primary">
                <Trophy className="h-5 w-5 text-accent" />
                <select className="w-full bg-transparent font-semibold focus:outline-none">
                  <option>Any sport</option>
                  {SPORTS.map((s) => <option key={s.name}>{s.name}</option>)}
                </select>
              </div>
              <div className="flex flex-1 items-center gap-3 rounded-xl bg-background/95 px-4 py-3 text-primary">
                <MapPin className="h-5 w-5 text-accent" />
                <input placeholder="City or postcode" className="w-full bg-transparent font-semibold placeholder:text-muted-foreground focus:outline-none" />
              </div>
              <Link
                to="/trainers"
                className="flex items-center justify-center gap-2 rounded-xl bg-gradient-accent px-6 py-3 font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-[1.02]"
              >
                <Search className="h-5 w-5" /> Search
              </Link>
            </div>

            <div className="mt-10 flex flex-wrap gap-x-10 gap-y-4">
              {[["500+", "Coaches"], ["120+", "Venues"], ["12K", "Sessions booked"]].map(([n, l]) => (
                <div key={l}>
                  <p className="font-display text-4xl text-accent">{n}</p>
                  <p className="text-xs uppercase tracking-widest text-primary-foreground/60">{l}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Sports photo collage */}
          <div className="relative hidden animate-float-up grid-cols-2 gap-4 lg:grid" style={{ animationDelay: "150ms" }}>
            {HERO_PHOTOS.map((p, i) => (
              <div
                key={p.label}
                className={`group relative overflow-hidden rounded-2xl border border-primary-foreground/10 shadow-elevate ${
                  i % 2 === 1 ? "translate-y-8" : ""
                }`}
              >
                <img
                  src={p.src}
                  alt={p.alt}
                  width={768}
                  height={1024}
                  className="aspect-[3/4] w-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-primary/80 via-transparent to-transparent" />
                <span className="absolute bottom-3 left-3 font-display text-2xl uppercase tracking-wide text-primary-foreground">
                  {p.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SPORTS STRIP */}
      <section className="border-y border-border bg-card">
        <div className="container flex gap-3 overflow-x-auto py-6">
          {SPORTS.map((s) => (
            <Link
              key={s.name}
              to="/trainers"
              className="group flex shrink-0 items-center gap-3 rounded-full border border-border bg-background px-5 py-3 transition-all hover:border-accent hover:shadow-card"
            >
              <span className="text-2xl">{s.emoji}</span>
              <span className="font-bold uppercase tracking-wider text-primary group-hover:text-accent">{s.name}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* HOW IT WORKS */}
      <section className="container py-20 md:py-28">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">How it works</p>
          <h2 className="mt-3 font-display text-5xl text-primary md:text-6xl">From search to session in minutes</h2>
        </div>
        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {[
            { icon: Search, title: "Find your coach", desc: "Filter by sport, city, price and rating." },
            { icon: Calendar, title: "Pick a time slot", desc: "See live availability on each trainer's calendar." },
            { icon: Trophy, title: "Train & improve", desc: "Show up at the venue. Level up. Repeat." },
          ].map((s, i) => (
            <div
              key={s.title}
              className="group relative animate-float-up overflow-hidden rounded-2xl border border-border bg-card p-8 shadow-card transition-all hover:-translate-y-1 hover:border-accent"
              style={{ animationDelay: `${i * 100}ms` }}
            >
              <div className="absolute -right-6 -top-6 font-display text-[120px] leading-none text-muted opacity-60">
                0{i + 1}
              </div>
              <s.icon className="relative h-10 w-10 text-accent" />
              <h3 className="relative mt-6 font-display text-3xl text-primary">{s.title}</h3>
              <p className="relative mt-2 text-muted-foreground">{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* FEATURED TRAINERS */}
      <section className="bg-muted/40 py-20 md:py-28">
        <div className="container">
          <div className="flex items-end justify-between">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Top rated</p>
              <h2 className="mt-2 font-display text-5xl text-primary md:text-6xl">Featured Coaches</h2>
            </div>
            <Link to="/trainers" className="group hidden items-center gap-2 text-sm font-bold uppercase tracking-wider text-primary md:flex">
              View all <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Link>
          </div>
          <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {TRAINERS.map((t, i) => <TrainerCard key={t.id} trainer={t} index={i} />)}
          </div>
        </div>
      </section>

      {/* VENUES */}
      <section className="container py-20 md:py-28">
        <div className="flex items-end justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Where you train</p>
            <h2 className="mt-2 font-display text-5xl text-primary md:text-6xl">Premium Venues</h2>
          </div>
          <Link to="/venues" className="group hidden items-center gap-2 text-sm font-bold uppercase tracking-wider text-primary md:flex">
            View all <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {VENUES.map((v, i) => <VenueCard key={v.id} venue={v} index={i} />)}
        </div>
      </section>

      {/* CTA */}
      <section className="container pb-24">
        <div className="relative overflow-hidden rounded-3xl bg-hero p-10 text-primary-foreground shadow-elevate md:p-16">
          <div className="grain absolute inset-0" />
          <div className="relative grid items-center gap-8 md:grid-cols-[1fr_auto]">
            <div>
              <h2 className="font-display text-5xl leading-none md:text-6xl">Ready to step up your game?</h2>
              <p className="mt-4 max-w-xl text-primary-foreground/80">Join thousands of athletes training with verified coaches across the UK.</p>
            </div>
            <Link to="/trainers" className="rounded-full bg-gradient-accent px-8 py-4 text-center font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105">
              Book your first session
            </Link>
          </div>
        </div>
      </section>

      <footer className="border-t border-border bg-card py-10">
        <div className="container flex flex-col items-center justify-between gap-4 text-sm text-muted-foreground md:flex-row">
          <p>© {new Date().getFullYear()} FindMyTrainer. All rights reserved.</p>
          <p className="font-bold uppercase tracking-widest text-primary">Train · Compete · Win</p>
        </div>
      </footer>
    </div>
  );
};

export default Index;
