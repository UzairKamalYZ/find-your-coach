import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Star, MapPin, Award, CheckCircle2, Heart, GraduationCap } from "lucide-react";
import { format } from "date-fns";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import Navbar from "@/components/Navbar";
import BookingCalendar from "@/components/BookingCalendar";
import Stars from "@/components/Stars";
import { useReviewSummary, useTrainer, useTrainerReviews } from "@/hooks/use-api";
import { useAuth } from "@/context/AuthContext";
import { api, errorMessage } from "@/lib/api";
import { resolveImage } from "@/lib/images";

const TrainerDetail = () => {
  const { id = "" } = useParams();
  const { data: trainer, isLoading, isError } = useTrainer(id);
  const { user } = useAuth();
  const qc = useQueryClient();
  const { data: favIds } = useQuery({ queryKey: ["favorite-ids"], enabled: !!user, queryFn: () => api<string[]>("/me/favorites/ids") });
  const isFav = !!favIds?.includes(id);

  const toggleFav = async () => {
    if (!user) { toast.error("Sign in to save coaches"); return; }
    try {
      await api(`/me/favorites/${id}`, { method: isFav ? "DELETE" : "PUT" });
      qc.invalidateQueries({ queryKey: ["favorite-ids"] });
      qc.invalidateQueries({ queryKey: ["favorites"] });
      toast.success(isFav ? "Removed from saved coaches" : "Saved to your coaches");
    } catch (e) { toast.error(errorMessage(e)); }
  };

  if (isLoading) {
    return <div className="min-h-screen bg-background"><Navbar /><div className="container pt-40 text-center text-muted-foreground">Loading coach…</div></div>;
  }
  if (isError || !trainer) {
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
              <img src={resolveImage(trainer.image)} alt={trainer.name} width={640} height={800} className="aspect-[4/5] w-full object-cover" />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="inline-block rounded-full bg-gradient-accent px-4 py-1.5 text-xs font-bold uppercase tracking-wider text-primary">
                  {trainer.sport} Coach
                </span>
                <button onClick={toggleFav} aria-pressed={isFav}
                  className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/30 px-4 py-1.5 text-xs font-bold uppercase tracking-wider hover:border-accent">
                  <Heart className={`h-4 w-4 ${isFav ? "fill-accent text-accent" : ""}`} /> {isFav ? "Saved" : "Save"}
                </button>
              </div>
              <h1 className="mt-4 font-display text-6xl leading-none md:text-8xl">{trainer.name}</h1>
              <div className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-2 text-primary-foreground/80">
                <span className="flex items-center gap-2"><MapPin className="h-4 w-4 text-accent" /> {trainer.city}</span>
                <span className="flex items-center gap-2"><Star className="h-4 w-4 fill-accent text-accent" /> {trainer.rating || "New"} ({trainer.reviews} reviews)</span>
                <span className="flex items-center gap-2"><Award className="h-4 w-4 text-accent" /> {trainer.experience} yrs experience</span>
                {trainer.qualifications && <span className="flex items-center gap-2"><GraduationCap className="h-4 w-4 text-accent" /> {trainer.qualifications}</span>}
              </div>
              {trainer.bio && <p className="mt-6 max-w-2xl text-lg text-primary-foreground/85">{trainer.bio}</p>}
            </div>
          </div>
        </div>
      </section>

      <section className="container -mt-10 grid gap-8 pb-20 md:grid-cols-[1fr_420px] md:pb-28">
        <div className="space-y-8">
          <div className="rounded-3xl border border-border bg-card p-8 shadow-card">
            {trainer.specialties.length > 0 && <>
              <h2 className="font-display text-3xl text-primary">Specialties</h2>
              <ul className="mt-5 space-y-3">
                {trainer.specialties.map((s) => (
                  <li key={s} className="flex items-center gap-3 text-primary">
                    <CheckCircle2 className="h-5 w-5 text-accent" />
                    <span className="font-semibold">{s}</span>
                  </li>
                ))}
              </ul>
            </>}

            <h2 className="mt-10 font-display text-3xl text-primary first:mt-0">What to expect</h2>
            <p className="mt-3 text-muted-foreground">
              Sessions run for {trainer.sessionMinutes} minutes and include a structured warm-up, technique work tailored to your level, and live feedback. Bring your own gear or ask about rentals at the venue.
            </p>

            <div className="mt-8 grid gap-4 sm:grid-cols-3">
              {[[`${trainer.sessionMinutes} min`, "Session"], ["1-on-1", "Format"], ["All levels", "Welcome"]].map(([n, l]) => (
                <div key={l} className="rounded-xl bg-muted p-4">
                  <p className="font-display text-2xl text-primary">{n}</p>
                  <p className="text-xs uppercase tracking-wider text-muted-foreground">{l}</p>
                </div>
              ))}
            </div>
          </div>
          <Reviews trainerId={trainer.id} />
        </div>

        <div className="md:sticky md:top-24 md:self-start">
          <BookingCalendar trainer={trainer} />
        </div>
      </section>
    </div>
  );
};

const Reviews = ({ trainerId }: { trainerId: string }) => {
  const { data: summary } = useReviewSummary(trainerId);
  const { data } = useTrainerReviews(trainerId);
  const reviews = data?.content ?? [];
  const written = summary ? Object.values(summary.breakdown).reduce((a, b) => a + b, 0) : 0;
  return (
    <div className="rounded-3xl border border-border bg-card p-8 shadow-card">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <h2 className="font-display text-3xl text-primary">Reviews</h2>
        {summary && <p className="flex items-center gap-2 text-sm text-muted-foreground"><Stars value={summary.rating} /> <span className="font-semibold text-primary">{summary.rating || "–"}</span> · {summary.reviews} ratings</p>}
      </div>
      {summary && written > 0 && (
        <div className="mt-5 space-y-1.5">
          {[5, 4, 3, 2, 1].map((s) => {
            const n = summary.breakdown[s] ?? 0;
            return (
              <div key={s} className="flex items-center gap-3 text-xs text-muted-foreground">
                <span className="w-8">{s}★</span>
                <div className="h-2 flex-1 overflow-hidden rounded-full bg-muted"><div className="h-full bg-accent" style={{ width: `${(n / written) * 100}%` }} /></div>
                <span className="w-6 text-right">{n}</span>
              </div>
            );
          })}
        </div>
      )}
      {reviews.length === 0 ? (
        <p className="mt-6 text-sm text-muted-foreground">No written reviews yet.</p>
      ) : (
        <ul className="mt-6 space-y-5">
          {reviews.map((r) => (
            <li key={r.id} className="border-t border-border pt-5 first:border-0 first:pt-0">
              <div className="flex items-center justify-between gap-3">
                <p className="font-semibold text-primary">{r.authorName}</p>
                <p className="text-xs text-muted-foreground">{format(new Date(r.createdAt), "d MMM yyyy")}</p>
              </div>
              <Stars value={r.rating} size={3} />
              {r.comment && <p className="mt-2 text-sm text-foreground">{r.comment}</p>}
              {r.reply && <p className="mt-3 rounded-lg bg-muted p-3 text-sm text-muted-foreground"><span className="font-semibold text-primary">Coach reply: </span>{r.reply}</p>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default TrainerDetail;
