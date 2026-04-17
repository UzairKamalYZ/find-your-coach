import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Star, MapPin, Award, CheckCircle2 } from "lucide-react";
import Navbar from "@/components/Navbar";
import BookingCalendar from "@/components/BookingCalendar";
import { TRAINERS } from "@/data/mock";

const TrainerDetail = () => {
  const { id } = useParams();
  const trainer = TRAINERS.find((t) => t.id === id);

  if (!trainer) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="container pt-40 text-center">
          <h1 className="font-display text-5xl text-primary">Coach not found</h1>
          <Link to="/trainers" className="mt-6 inline-block text-accent underline">Back to trainers</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <section className="relative bg-hero pb-16 pt-32 text-primary-foreground md:pb-24 md:pt-40">
        <div className="grain absolute inset-0" />
        <div className="container relative">
          <Link to="/trainers" className="inline-flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-primary-foreground/80 hover:text-accent">
            <ArrowLeft className="h-4 w-4" /> All coaches
          </Link>

          <div className="mt-8 grid gap-10 md:grid-cols-[300px_1fr] md:items-end">
            <div className="relative overflow-hidden rounded-3xl shadow-elevate">
              <img
                src={trainer.image}
                alt={trainer.name}
                width={640}
                height={800}
                className="aspect-[4/5] w-full object-cover"
              />
            </div>
            <div>
              <span className="inline-block rounded-full bg-gradient-accent px-4 py-1.5 text-xs font-bold uppercase tracking-wider text-primary">
                {trainer.sport} Coach
              </span>
              <h1 className="mt-4 font-display text-6xl leading-none md:text-8xl">{trainer.name}</h1>
              <div className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-2 text-primary-foreground/80">
                <span className="flex items-center gap-2"><MapPin className="h-4 w-4 text-accent" /> {trainer.city}</span>
                <span className="flex items-center gap-2"><Star className="h-4 w-4 fill-accent text-accent" /> {trainer.rating} ({trainer.reviews} reviews)</span>
                <span className="flex items-center gap-2"><Award className="h-4 w-4 text-accent" /> {trainer.experience} yrs experience</span>
              </div>
              <p className="mt-6 max-w-2xl text-lg text-primary-foreground/85">{trainer.bio}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="container -mt-10 grid gap-8 pb-20 md:grid-cols-[1fr_420px] md:pb-28">
        <div className="rounded-3xl border border-border bg-card p-8 shadow-card">
          <h2 className="font-display text-3xl text-primary">Specialties</h2>
          <ul className="mt-5 space-y-3">
            {trainer.specialties.map((s) => (
              <li key={s} className="flex items-center gap-3 text-primary">
                <CheckCircle2 className="h-5 w-5 text-accent" />
                <span className="font-semibold">{s}</span>
              </li>
            ))}
          </ul>

          <h2 className="mt-10 font-display text-3xl text-primary">What to expect</h2>
          <p className="mt-3 text-muted-foreground">
            Sessions run for 60 minutes and include a structured warm-up, technique work tailored to your level, and live feedback. Bring your own gear or ask about rentals at the venue.
          </p>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            {[["60 min", "Session"], ["1-on-1", "Format"], ["All levels", "Welcome"]].map(([n, l]) => (
              <div key={l} className="rounded-xl bg-muted p-4">
                <p className="font-display text-2xl text-primary">{n}</p>
                <p className="text-xs uppercase tracking-wider text-muted-foreground">{l}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="md:sticky md:top-24 md:self-start">
          <BookingCalendar trainer={trainer} />
        </div>
      </section>
    </div>
  );
};

export default TrainerDetail;
