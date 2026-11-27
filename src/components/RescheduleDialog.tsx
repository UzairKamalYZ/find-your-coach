import { useMemo, useState } from "react";
import { addDays, format, parseISO, startOfToday } from "date-fns";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useTrainerAvailability } from "@/hooks/use-api";
import { api, errorMessage } from "@/lib/api";
import type { Booking } from "@/lib/types";

export default function RescheduleDialog({ booking, onClose, onDone }: { booking: Booking | null; onClose: () => void; onDone: () => void }) {
  return (
    <Dialog open={!!booking} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-lg">
        {booking && <Body booking={booking} onClose={onClose} onDone={onDone} />}
      </DialogContent>
    </Dialog>
  );
}

function Body({ booking, onClose, onDone }: { booking: Booking; onClose: () => void; onDone: () => void }) {
  const today = startOfToday();
  const from = format(today, "yyyy-MM-dd");
  const to = format(addDays(today, 13), "yyyy-MM-dd");
  const { data } = useTrainerAvailability(booking.trainerId, from, to);
  const [date, setDate] = useState(from);
  const [slot, setSlot] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const qc = useQueryClient();
  const day = useMemo(() => data?.find((d) => d.date === date), [data, date]);

  const submit = async () => {
    if (!slot) return;
    setBusy(true);
    try {
      await api(`/bookings/${booking.id}/reschedule`, { method: "POST", body: { date, slot } });
      toast.success("Session moved", { description: `${format(parseISO(date), "EEE d MMM")} at ${slot}` });
      qc.invalidateQueries({ queryKey: ["availability", booking.trainerId] });
      onDone();
      onClose();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };

  return (
    <>
      <DialogHeader>
        <DialogTitle className="font-display text-3xl">Reschedule</DialogTitle>
        <DialogDescription>{booking.trainerName} · currently {format(parseISO(booking.date), "EEE d MMM")} at {booking.slot} · {3 - booking.rescheduleCount} change(s) left</DialogDescription>
      </DialogHeader>
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
        {(data ?? []).map((d) => (
          <button key={d.date} onClick={() => { setDate(d.date); setSlot(null); }}
            className={`flex min-w-[56px] flex-col items-center rounded-lg px-2 py-2 ${d.date === date ? "bg-accent text-accent-foreground" : "bg-muted"}`}>
            <span className="text-[10px] font-bold uppercase">{format(parseISO(d.date), "EEE")}</span>
            <span className="font-display text-xl leading-none">{format(parseISO(d.date), "d")}</span>
          </button>
        ))}
      </div>
      <div className="grid grid-cols-3 gap-2">
        {(day?.slots ?? []).map((s) => {
          const off = s.status !== "AVAILABLE";
          return (
            <button key={s.time} disabled={off} onClick={() => setSlot(s.time)}
              className={`rounded-md border-2 py-2 text-sm font-semibold ${off ? "cursor-not-allowed border-border bg-muted/40 text-muted-foreground/50 line-through" : slot === s.time ? "border-accent bg-accent text-accent-foreground" : "border-border hover:border-accent"}`}>
              {s.time}
            </button>
          );
        })}
      </div>
      <button onClick={submit} disabled={!slot || busy} className="h-11 rounded-full bg-gradient-accent text-sm font-bold uppercase tracking-wider text-primary disabled:opacity-40">
        {busy ? "Moving…" : "Confirm new time"}
      </button>
    </>
  );
}
