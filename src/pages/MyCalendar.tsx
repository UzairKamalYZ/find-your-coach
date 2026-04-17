import { useMemo } from "react";
import { Link, Navigate } from "react-router-dom";
import { format, isSameDay, parseISO, startOfToday } from "date-fns";
import { Calendar as CalendarIcon, Clock, MapPin, Trash2 } from "lucide-react";
import Navbar from "@/components/Navbar";
import { useAuth } from "@/context/AuthContext";
import { toast } from "sonner";

const MyCalendar = () => {
  const { user, bookings, cancelBooking } = useAuth();
  if (!user) return <Navigate to="/login" replace />;

  const today = startOfToday();
  const sorted = useMemo(
    () => [...bookings].sort((a, b) => +parseISO(a.date) - +parseISO(b.date) || a.slot.localeCompare(b.slot)),
    [bookings],
  );
  const upcoming = sorted.filter((b) => parseISO(b.date) >= today);
  const past = sorted.filter((b) => parseISO(b.date) < today).reverse();

  const handleCancel = (id: string) => {
    cancelBooking(id);
    toast.success("Booking cancelled");
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="bg-gradient-hero pt-24 pb-32">
        <Navbar />
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">Schedule</p>
          <h1 className="mt-2 font-display text-5xl text-primary-foreground md:text-6xl">My Calendar</h1>
          <p className="mt-3 text-primary-foreground/70">{upcoming.length} upcoming · {past.length} past sessions</p>
        </div>
      </div>

      <div className="container -mt-20 pb-20">
        <Section title="Upcoming Sessions" empty="No upcoming sessions. Book one with a trainer!" items={upcoming} onCancel={handleCancel} />
        {past.length > 0 && (
          <div className="mt-10">
            <Section title="Past Sessions" empty="" items={past} faded />
          </div>
        )}
      </div>
    </div>
  );
};

const Section = ({
  title, items, empty, onCancel, faded,
}: {
  title: string;
  items: ReturnType<typeof useAuth>["bookings"];
  empty: string;
  onCancel?: (id: string) => void;
  faded?: boolean;
}) => {
  // Group by date
  const groups = items.reduce<Record<string, typeof items>>((acc, b) => {
    const k = b.date.slice(0, 10);
    (acc[k] ||= []).push(b);
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
            const d = parseISO(list[0].date);
            const today = isSameDay(d, new Date());
            return (
              <div key={dateKey}>
                <div className="mb-3 flex items-baseline gap-3">
                  <span className="font-display text-4xl text-primary">{format(d, "d")}</span>
                  <div>
                    <p className="text-sm font-bold uppercase tracking-wider text-primary">{format(d, "EEEE")}</p>
                    <p className="text-xs text-muted-foreground">{format(d, "MMMM yyyy")} {today && <span className="ml-1 rounded-full bg-accent px-2 py-0.5 text-[10px] font-bold uppercase text-accent-foreground">Today</span>}</p>
                  </div>
                </div>
                <div className="space-y-3">
                  {list.map((b) => (
                    <div key={b.id}
                      className={`flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-card ${faded ? "opacity-60" : ""}`}>
                      <img src={b.trainerImage} alt={b.trainerName} className="h-14 w-14 rounded-xl object-cover" />
                      <div className="flex-1 min-w-0">
                        <Link to={`/trainers/${b.trainerId}`} className="font-display text-xl text-primary hover:text-accent">{b.trainerName}</Link>
                        <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1"><Clock className="h-3 w-3" />{b.slot}</span>
                          <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{b.sport}</span>
                          <span className="font-bold text-primary">£{b.price}</span>
                        </div>
                      </div>
                      {onCancel && (
                        <button onClick={() => onCancel(b.id)}
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

export default MyCalendar;
