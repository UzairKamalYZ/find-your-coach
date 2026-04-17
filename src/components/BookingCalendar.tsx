import { useMemo, useState } from "react";
import { addDays, format, isSameDay, startOfToday } from "date-fns";
import { Clock } from "lucide-react";
import { toast } from "sonner";
import type { Trainer } from "@/data/mock";

const SLOTS = ["08:00", "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "19:00"];

const BookingCalendar = ({ trainer }: { trainer: Trainer }) => {
  const today = startOfToday();
  const days = useMemo(() => Array.from({ length: 14 }, (_, i) => addDays(today, i)), [today]);
  const [selectedDay, setSelectedDay] = useState<Date>(days[0]);
  const [slot, setSlot] = useState<string | null>(null);

  // pseudo-random unavailability
  const unavailable = (d: Date, time: string) => {
    const hash = (d.getDate() + parseInt(time)) % 4;
    return hash === 0;
  };

  const handleBook = () => {
    if (!slot) return;
    toast.success("Booking confirmed!", {
      description: `${trainer.name} · ${format(selectedDay, "EEE d MMM")} at ${slot}`,
    });
    setSlot(null);
  };

  return (
    <div className="rounded-3xl border border-border bg-card p-6 shadow-card">
      <h3 className="font-display text-3xl text-primary">Book a Session</h3>
      <p className="mt-1 text-sm text-muted-foreground">Pick a date and time slot</p>

      <div className="mt-6 -mx-2 flex gap-2 overflow-x-auto px-2 pb-2">
        {days.map((d) => {
          const active = isSameDay(d, selectedDay);
          return (
            <button
              key={d.toISOString()}
              onClick={() => { setSelectedDay(d); setSlot(null); }}
              className={`flex min-w-[68px] flex-col items-center rounded-xl px-3 py-3 text-center transition-all ${
                active
                  ? "bg-gradient-accent text-primary shadow-glow"
                  : "bg-muted text-primary hover:bg-muted/70"
              }`}
            >
              <span className="text-[10px] font-bold uppercase tracking-wider opacity-70">
                {format(d, "EEE")}
              </span>
              <span className="font-display text-2xl leading-none">{format(d, "d")}</span>
              <span className="text-[10px] uppercase opacity-70">{format(d, "MMM")}</span>
            </button>
          );
        })}
      </div>

      <div className="mt-6">
        <p className="mb-3 text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Available slots · {format(selectedDay, "EEEE d MMMM")}
        </p>
        <div className="grid grid-cols-3 gap-2">
          {SLOTS.map((s) => {
            const off = unavailable(selectedDay, s);
            const active = slot === s;
            return (
              <button
                key={s}
                disabled={off}
                onClick={() => setSlot(s)}
                className={`flex items-center justify-center gap-1.5 rounded-lg border-2 py-2.5 text-sm font-semibold transition-all ${
                  off
                    ? "cursor-not-allowed border-border bg-muted/40 text-muted-foreground/50 line-through"
                    : active
                    ? "border-accent bg-accent text-accent-foreground"
                    : "border-border bg-background text-primary hover:border-accent"
                }`}
              >
                <Clock className="h-3.5 w-3.5" />
                {s}
              </button>
            );
          })}
        </div>
      </div>

      <div className="mt-6 flex items-center justify-between border-t border-border pt-5">
        <div>
          <p className="text-xs uppercase tracking-wider text-muted-foreground">Total</p>
          <p className="font-display text-3xl text-primary">£{trainer.hourlyRate}</p>
        </div>
        <button
          onClick={handleBook}
          disabled={!slot}
          className="rounded-full bg-gradient-accent px-7 py-3 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
        >
          Confirm Booking
        </button>
      </div>
    </div>
  );
};

export default BookingCalendar;
