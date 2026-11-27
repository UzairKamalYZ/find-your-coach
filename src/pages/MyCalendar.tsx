import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { format, isSameDay, parseISO, startOfToday } from "date-fns";
import { Calendar as CalendarIcon, Clock, MapPin, Trash2, Star, CalendarClock, Building2 } from "lucide-react";
import Navbar from "@/components/Navbar";
import RequireAuth from "@/components/RequireAuth";
import ReviewDialog from "@/components/ReviewDialog";
import RescheduleDialog from "@/components/RescheduleDialog";
import { useAuth } from "@/context/AuthContext";
import { api, errorMessage } from "@/lib/api";
import { resolveImage } from "@/lib/images";
import { money } from "@/lib/format";
import type { Booking, VenueBooking } from "@/lib/types";
import { toast } from "sonner";

type Item =
  | { kind: "coach"; id: string; date: string; time: string; b: Booking }
  | { kind: "venue"; id: string; date: string; time: string; v: VenueBooking };

const MyCalendarPage = () => {
  const { bookings, venueBookings, cancelBooking, refreshBookings } = useAuth();
  const [reviewing, setReviewing] = useState<Booking | null>(null);
  const [moving, setMoving] = useState<Booking | null>(null);
  const today = startOfToday();

  const items = useMemo<Item[]>(() => [
    ...bookings.map((b) => ({ kind: "coach" as const, id: b.id, date: b.date, time: b.slot, b })),
    ...venueBookings.map((v) => ({ kind: "venue" as const, id: v.id, date: v.date, time: v.startTime.slice(0, 5), v })),
  ].sort((a, b) => a.date.localeCompare(b.date) || a.time.localeCompare(b.time)), [bookings, venueBookings]);

  const isPast = (i: Item) => parseISO(i.date) < today || (i.kind === "coach" && i.b.status !== "CONFIRMED") || (i.kind === "venue" && i.v.status !== "CONFIRMED");
  const upcoming = items.filter((i) => !isPast(i));
  const past = items.filter(isPast).reverse();

  const cancel = async (i: Item) => {
    if (!window.confirm("Cancel this booking?")) return;
    try {
      if (i.kind === "coach") {
        await cancelBooking(i.id);
        if (i.b.startsAt && new Date(i.b.startsAt).getTime() - Date.now() < 24 * 3600 * 1000) toast.message("Late cancellation", { description: "Cancelled less than 24h before the session." });
      } else {
        await api(`/venue-bookings/${i.id}/cancel`, { method: "POST", body: {} });
        await refreshBookings();
      }
      toast.success("Booking cancelled");
    } catch (e) { toast.error(errorMessage(e)); }
  };

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <section className="bg-hero pb-12 pt-32 text-primary-foreground md:pb-16 md:pt-40">
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Schedule</p>
          <h1 className="mt-2 font-display text-5xl text-primary-foreground md:text-6xl">My Calendar</h1>
          <p className="mt-3 text-primary-foreground/70">{upcoming.length} upcoming · {past.length} past sessions</p>
        </div>
      </section>

      <div className="container py-12 md:py-16">
        <Section title="Upcoming Sessions" empty="No upcoming sessions. Book one with a trainer!" items={upcoming}
          onCancel={cancel} onReschedule={setMoving} />
        {past.length > 0 && (
          <div className="mt-10">
            <Section title="Past Sessions" empty="" items={past} faded onReview={setReviewing} />
          </div>
        )}
      </div>
      <ReviewDialog booking={reviewing} onClose={() => setReviewing(null)} onDone={refreshBookings} />
      <RescheduleDialog booking={moving} onClose={() => setMoving(null)} onDone={refreshBookings} />
    </div>
  );
};

const Section = ({
  title, items, empty, onCancel, onReschedule, onReview, faded,
}: {
  title: string;
  items: Item[];
  empty: string;
  onCancel?: (i: Item) => void;
  onReschedule?: (b: Booking) => void;
  onReview?: (b: Booking) => void;
  faded?: boolean;
}) => {
  const groups = items.reduce<Record<string, Item[]>>((acc, i) => {
    (acc[i.date] ||= []).push(i);
    return acc;
  }, {});

  return (
    <div>
      <h2 className="mb-4 font-display text-3xl text-primary">{title}</h2>
      {items.length === 0 ? (
        <div className="rounded-3xl border-2 border-dashed border-border bg-card p-12 text-center">
          <CalendarIcon className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">{empty}</p>
          <Link to="/trainers" className="mt-5 inline-block rounded-full bg-gradient-accent px-6 py-2.5 text-sm font-bold uppercase tracking-wider text-primary shadow-glow">
            Browse Trainers
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(groups).map(([dateKey, list]) => {
            const d = parseISO(dateKey);
            const isToday = isSameDay(d, new Date());
            return (
              <div key={dateKey}>
                <div className="mb-3 flex items-baseline gap-3">
                  <span className="font-display text-4xl text-primary">{format(d, "d")}</span>
                  <div>
                    <p className="text-sm font-bold uppercase tracking-wider text-primary">{format(d, "EEEE")}</p>
                    <p className="text-xs text-muted-foreground">{format(d, "MMMM yyyy")} {isToday && <span className="ml-1 rounded-full bg-accent px-2 py-0.5 text-[10px] font-bold uppercase text-accent-foreground">Today</span>}</p>
                  </div>
                </div>
                <div className="space-y-3">
                  {list.map((i) => i.kind === "coach" ? (
                    <div key={i.id} className={`flex flex-wrap items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-card ${faded && !i.b.canReview ? "opacity-60" : ""}`}>
                      <img src={resolveImage(i.b.trainerImage)} alt={i.b.trainerName} className="h-14 w-14 rounded-xl object-cover" />
                      <div className="min-w-0 flex-1">
                        <Link to={`/trainers/${i.b.trainerId}`} className="font-display text-xl text-primary hover:text-accent">{i.b.trainerName}</Link>
                        <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1"><Clock className="h-3 w-3" />{i.b.slot}</span>
                          <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{i.b.sport}</span>
                          <span className="font-bold text-primary">{money(i.b.price)}</span>
                          {i.b.status !== "CONFIRMED" && <span className="rounded-full bg-muted px-2 py-0.5 font-semibold uppercase">{i.b.status.replace("_", " ").toLowerCase()}</span>}
                          {i.b.reviewed && <span className="flex items-center gap-1"><Star className="h-3 w-3 fill-accent text-accent" />Reviewed</span>}
                        </div>
                      </div>
                      <div className="flex gap-2">
                        {onReview && i.b.canReview && (
                          <button onClick={() => onReview(i.b)} className="rounded-full bg-gradient-accent px-4 py-2 text-xs font-bold uppercase tracking-wider text-primary">Review</button>
                        )}
                        {onReschedule && i.b.canReschedule && (
                          <button onClick={() => onReschedule(i.b)} aria-label="Reschedule"
                            className="rounded-full border-2 border-border p-2.5 text-muted-foreground transition-colors hover:border-accent hover:text-accent">
                            <CalendarClock className="h-4 w-4" />
                          </button>
                        )}
                        {onCancel && i.b.canCancel && (
                          <button onClick={() => onCancel(i)} aria-label="Cancel booking"
                            className="rounded-full border-2 border-border p-2.5 text-muted-foreground transition-colors hover:border-destructive hover:text-destructive">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div key={i.id} className={`flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-card ${faded ? "opacity-60" : ""}`}>
                      <img src={resolveImage(i.v.venueImage)} alt={i.v.venueName} className="h-14 w-14 rounded-xl object-cover" />
                      <div className="min-w-0 flex-1">
                        <p className="flex items-center gap-2 font-display text-xl text-primary"><Building2 className="h-4 w-4 text-accent" />{i.v.venueName}</p>
                        <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1"><Clock className="h-3 w-3" />{i.v.startTime.slice(0, 5)}–{i.v.endTime.slice(0, 5)}</span>
                          <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{i.v.city}</span>
                          <span className="font-bold text-primary">{money(i.v.totalPrice)}</span>
                        </div>
                      </div>
                      {onCancel && i.v.canCancel && (
                        <button onClick={() => onCancel(i)} aria-label="Cancel venue booking"
                          className="rounded-full border-2 border-border p-2.5 text-muted-foreground transition-colors hover:border-destructive hover:text-destructive">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

const MyCalendar = () => <RequireAuth><MyCalendarPage /></RequireAuth>;
export default MyCalendar;
