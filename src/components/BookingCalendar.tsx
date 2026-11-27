import { useMemo, useState } from "react";
import { addDays, format, parseISO, startOfToday } from "date-fns";
import { Clock } from "lucide-react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import type { Trainer } from "@/lib/types";
import { useAuth } from "@/context/AuthContext";
import { useTrainerAvailability } from "@/hooks/use-api";
import { errorMessage } from "@/lib/api";
import { money } from "@/lib/format";

const SLOTS = ["08:00", "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "19:00"];
const iso = (d: Date) => format(d, "yyyy-MM-dd");

const BookingCalendar = ({ trainer }: { trainer: Trainer }) => {
  const today = startOfToday();
  const from = iso(today);
  const to = iso(addDays(today, 13));
  const { data: days, isLoading, isError, refetch } = useTrainerAvailability(trainer.id, from, to);
  const [selected, setSelected] = useState<string>(from);
  const [slot, setSlot] = useState<string | null>(null);
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const { user, addBooking } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const dayList = useMemo(() => days ?? Array.from({ length: 14 }, (_, i) => ({ date: iso(addDays(today, i)), dayOff: false, dayOffReason: undefined as string | undefined, slots: [] as { time: string; status: string }[] })), [days, today]);
  const current = dayList.find((d) => d.date === selected) ?? dayList[0];
  const statusOf = (t: string) => current?.slots.find((s) => s.time === t)?.status ?? "UNAVAILABLE";
  const freeCount = (d: (typeof dayList)[number]) => d.slots.filter((s) => s.status === "AVAILABLE").length;
  const isOwnListing = user?.trainerId === trainer.id;

  const handleBook = async () => {
    if (!slot) return;
    if (!user) {
      toast.error("Please sign in to book", { description: "Redirecting to login..." });
      setTimeout(() => navigate("/login", { state: { from: `/trainers/${trainer.id}` } }), 600);
      return;
    }
    setBusy(true);
    try {
      await addBooking({ trainerId: trainer.id, date: selected, slot, notes: notes.trim() || undefined });
      toast.success("Booking confirmed!", {
        description: `${trainer.name} · ${format(parseISO(selected), "EEE d MMM")} at ${slot}`,
        action: { label: "View", onClick: () => navigate("/my-calendar") },
      });
      setSlot(null);
      setNotes("");
    } catch (e) {
      toast.error("Could not book this slot", { description: errorMessage(e) });
    } finally {
      setBusy(false);
      qc.invalidateQueries({ queryKey: ["availability", trainer.id] });
      qc.invalidateQueries({ queryKey: ["notifications"] });
    }
  };

  return (
    <div className="rounded-3xl border border-border bg-card p-6 shadow-card">
      <h3 className="font-display text-3xl text-primary">Book a Session</h3>
      <p className="mt-1 text-sm text-muted-foreground">Pick a date and time slot · {trainer.sessionMinutes} minutes</p>

      <div className="mt-6 -mx-2 flex gap-2 overflow-x-auto px-2 pb-2">
        {dayList.map((d) => {
          const date = parseISO(d.date);
          const active = d.date === selected;
          const none = !!days && freeCount(d) === 0;
          return (
            <button
              key={d.date}
              onClick={() => { setSelected(d.date); setSlot(null); }}
              className={`flex min-w-[68px] flex-col items-center rounded-xl px-3 py-3 text-center transition-all ${
                active ? "bg-gradient-accent text-primary shadow-glow" : none ? "bg-muted/40 text-muted-foreground" : "bg-muted text-primary hover:bg-muted/70"
              }`}
            >
              <span className="text-[10px] font-bold uppercase tracking-wider opacity-70">{format(date, "EEE")}</span>
              <span className="font-display text-2xl leading-none">{format(date, "d")}</span>
              <span className="text-[10px] uppercase opacity-70">{format(date, "MMM")}</span>
            </button>
          );
        })}
      </div>

      <div className="mt-6">
        <p className="mb-3 text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Available slots · {format(parseISO(selected), "EEEE d MMMM")}
        </p>
        {isError ? (
          <div className="rounded-lg border border-dashed border-border p-4 text-sm text-muted-foreground">
            Couldn't load availability. <button className="font-semibold text-accent underline" onClick={() => refetch()}>Retry</button>
          </div>
        ) : current?.dayOff ? (
          <p className="rounded-lg bg-muted p-4 text-sm text-muted-foreground">{trainer.name.split(" ")[0]} is away this day{current.dayOffReason ? ` (${current.dayOffReason})` : ""}.</p>
        ) : (
          <div className="grid grid-cols-3 gap-2">
            {SLOTS.map((s) => {
              const st = isLoading ? "LOADING" : statusOf(s);
              const off = st !== "AVAILABLE";
              const active = slot === s;
              return (
                <button
                  key={s}
                  disabled={off}
                  title={st === "BOOKED" ? "Already booked" : st === "PAST" ? "Time has passed" : st === "UNAVAILABLE" ? "Coach not available" : undefined}
                  onClick={() => setSlot(s)}
                  className={`flex items-center justify-center gap-1.5 rounded-lg border-2 py-2.5 text-sm font-semibold transition-all ${
                    off
                      ? "cursor-not-allowed border-border bg-muted/40 text-muted-foreground/50 line-through"
                      : active
                      ? "border-accent bg-accent text-accent-foreground"
                      : "border-border bg-background text-primary hover:border-accent"
                  } ${st === "LOADING" ? "animate-pulse no-underline" : ""}`}
                >
                  <Clock className="h-3.5 w-3.5" />
                  {s}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {slot && (
        <label className="mt-5 block">
          <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-muted-foreground">Notes for the coach (optional)</span>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} maxLength={500}
            placeholder="Goals, injuries, level…" className="w-full resize-none rounded-md border border-input bg-background p-3 text-sm outline-none focus:border-ring" />
        </label>
      )}

      <div className="mt-6 flex items-center justify-between border-t border-border pt-5">
        <div>
          <p className="text-xs uppercase tracking-wider text-muted-foreground">Total</p>
          <p className="font-display text-3xl text-primary">{money(trainer.hourlyRate)}</p>
        </div>
        <button
          onClick={handleBook}
          disabled={!slot || busy || isOwnListing}
          title={isOwnListing ? "This is your own listing" : undefined}
          className="rounded-full bg-gradient-accent px-7 py-3 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
        >
          {busy ? "Booking…" : "Confirm Booking"}
        </button>
      </div>
    </div>
  );
};

export default BookingCalendar;
