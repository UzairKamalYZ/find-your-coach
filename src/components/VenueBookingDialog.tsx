import { useEffect, useMemo, useState } from "react";
import { addDays, format, parseISO, startOfToday } from "date-fns";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useAuth } from "@/context/AuthContext";
import { useVenueAvailability } from "@/hooks/use-api";
import { api, errorMessage } from "@/lib/api";
import { money } from "@/lib/format";
import type { Venue, VenueBooking } from "@/lib/types";

const iso = (d: Date) => format(d, "yyyy-MM-dd");

export default function VenueBookingDialog({ venue, onClose }: { venue: Venue | null; onClose: () => void }) {
  const today = startOfToday();
  const days = useMemo(() => Array.from({ length: 14 }, (_, i) => iso(addDays(today, i))), [today]);
  const [date, setDate] = useState(days[0]);
  const [start, setStart] = useState<string | null>(null);
  const [hours, setHours] = useState(1);
  const [busy, setBusy] = useState(false);
  const { user, refreshBookings } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { data, isLoading } = useVenueAvailability(venue?.id, date, date);
  const slots = data?.[0]?.hours ?? [];

  useEffect(() => { setStart(null); setHours(1); }, [venue?.id, date]);

  const fits = (s: string, h: number) => {
    const i = slots.findIndex((x) => x.time === s);
    if (i < 0 || i + h > slots.length) return false;
    return slots.slice(i, i + h).every((x) => x.available);
  };

  const submit = async () => {
    if (!venue || !start) return;
    if (!user) { onClose(); navigate("/login", { state: { from: "/venues" } }); return; }
    setBusy(true);
    try {
      const b = await api<VenueBooking>("/venue-bookings", { method: "POST", body: { venueId: venue.id, date, startTime: start, hours } });
      toast.success("Venue reserved!", { description: `${venue.name} · ${format(parseISO(date), "EEE d MMM")} ${b.startTime.slice(0, 5)}–${b.endTime.slice(0, 5)}` });
      await refreshBookings();
      qc.invalidateQueries({ queryKey: ["venue-availability", venue.id] });
      onClose();
    } catch (e) {
      toast.error("Could not reserve", { description: errorMessage(e) });
      qc.invalidateQueries({ queryKey: ["venue-availability", venue.id] });
    } finally { setBusy(false); }
  };

  return (
    <Dialog open={!!venue} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        {venue && <>
          <DialogHeader>
            <DialogTitle className="font-display text-3xl">{venue.name}</DialogTitle>
            <DialogDescription>{venue.type} · {venue.sport} · {venue.city}{venue.address ? ` · ${venue.address}` : ""}</DialogDescription>
          </DialogHeader>
          {venue.description && <p className="text-sm text-muted-foreground">{venue.description}</p>}
          {venue.amenities.length > 0 && <div className="flex flex-wrap gap-2">{venue.amenities.map((a) => <span key={a} className="rounded-full bg-muted px-3 py-1 text-xs font-semibold">{a}</span>)}</div>}
          <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
            {days.map((d) => {
              const dt = parseISO(d);
              return (
                <button key={d} onClick={() => setDate(d)}
                  className={`flex min-w-[56px] flex-col items-center rounded-lg px-2 py-2 text-center ${d === date ? "bg-accent text-accent-foreground" : "bg-muted"}`}>
                  <span className="text-[10px] font-bold uppercase">{format(dt, "EEE")}</span>
                  <span className="font-display text-xl leading-none">{format(dt, "d")}</span>
                </button>
              );
            })}
          </div>
          <div className="grid grid-cols-4 gap-2">
            {isLoading && <p className="col-span-4 text-sm text-muted-foreground">Loading availability…</p>}
            {slots.map((s) => (
              <button key={s.time} disabled={!s.available} onClick={() => setStart(s.time)}
                title={s.available ? `${s.remaining} of ${venue.capacity} free` : "Unavailable"}
                className={`rounded-md border-2 py-2 text-sm font-semibold ${!s.available ? "cursor-not-allowed border-border bg-muted/40 text-muted-foreground/50 line-through" : start === s.time ? "border-accent bg-accent text-accent-foreground" : "border-border hover:border-accent"}`}>
                {s.time}
              </button>
            ))}
          </div>
          <div className="flex items-center justify-between gap-3">
            <label className="flex items-center gap-2 text-sm font-semibold">Duration
              <select value={hours} onChange={(e) => setHours(Number(e.target.value))} className="h-9 rounded-md border border-input bg-background px-2">
                {[1, 2, 3, 4].map((h) => <option key={h} value={h} disabled={!!start && !fits(start, h)}>{h} hour{h > 1 ? "s" : ""}</option>)}
              </select>
            </label>
            <p className="font-display text-2xl">{money(venue.pricePerHour * hours)}</p>
          </div>
          <button onClick={submit} disabled={!start || busy || (start ? !fits(start, hours) : true)}
            className="h-11 w-full rounded-full bg-gradient-accent text-sm font-bold uppercase tracking-wider text-primary disabled:opacity-40">
            {busy ? "Reserving…" : user ? "Reserve" : "Sign in to reserve"}
          </button>
        </>}
      </DialogContent>
    </Dialog>
  );
}
