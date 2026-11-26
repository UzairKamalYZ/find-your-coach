import { useState } from "react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import Stars from "@/components/Stars";
import { api, errorMessage } from "@/lib/api";
import type { Booking } from "@/lib/types";

export default function ReviewDialog({ booking, onClose, onDone }: { booking: Booking | null; onClose: () => void; onDone: () => void }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const qc = useQueryClient();
  const submit = async () => {
    if (!booking) return;
    setBusy(true);
    try {
      await api("/reviews", { method: "POST", body: { bookingId: booking.id, rating, comment: comment.trim() || undefined } });
      toast.success("Thanks for your review!");
      qc.invalidateQueries({ queryKey: ["reviews", booking.trainerId] });
      qc.invalidateQueries({ queryKey: ["trainer", booking.trainerId] });
      setComment("");
      setRating(5);
      onDone();
      onClose();
    } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };
  return (
    <Dialog open={!!booking} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-display text-3xl">Rate your session</DialogTitle>
          <DialogDescription>{booking?.trainerName} · {booking?.sport}</DialogDescription>
        </DialogHeader>
        <Stars value={rating} size={6} onChange={setRating} />
        <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={4} maxLength={1000} placeholder="What went well? What could be better?"
          className="w-full resize-none rounded-md border border-input bg-background p-3 text-sm outline-none focus:border-ring" />
        <button onClick={submit} disabled={busy} className="h-11 rounded-full bg-gradient-accent text-sm font-bold uppercase tracking-wider text-primary disabled:opacity-40">
          {busy ? "Sending…" : "Submit review"}
        </button>
      </DialogContent>
    </Dialog>
  );
}
