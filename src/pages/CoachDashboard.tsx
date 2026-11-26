import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { format, parseISO } from "date-fns";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CalendarDays, CheckCircle2, Clock, PoundSterling, Star, Users, XCircle, UserX } from "lucide-react";
import Navbar from "@/components/Navbar";
import RequireAuth from "@/components/RequireAuth";
import AccountField from "@/components/AccountField";
import Stars from "@/components/Stars";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useSports } from "@/hooks/use-api";
import { api, ApiError, errorMessage } from "@/lib/api";
import { money } from "@/lib/format";
import { IMAGE_KEYS } from "@/lib/images";
import type { Booking, BookingStatus, CoachStats, Page, Review, TimeOff, Trainer, WeekDay, WeeklySchedule } from "@/lib/types";

const DAYS: WeekDay[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const STATUS_STYLE: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800", PENDING: "bg-amber-100 text-amber-800",
  SUSPENDED: "bg-red-100 text-red-800", REJECTED: "bg-red-100 text-red-800",
};

const useListing = () =>
  useQuery({
    queryKey: ["coach", "listing"],
    retry: false,
    queryFn: async () => {
      try { return await api<Trainer>("/coach/listing"); }
      catch (e) { if (e instanceof ApiError && e.status === 404) return null; throw e; }
    },
  });

const CoachDashboardPage = () => {
  const { data: listing, isLoading } = useListing();
  if (isLoading) return <Shell><p className="text-muted-foreground">Loading…</p></Shell>;
  if (!listing) {
    return (
      <Shell>
        <div className="max-w-3xl">
          <h2 className="font-display text-4xl">Create your coaching listing</h2>
          <p className="mt-2 text-muted-foreground">Tell trainees what you coach and what you charge. Our team reviews new listings before they go live — usually within a day.</p>
          <div className="mt-8"><ListingForm /></div>
        </div>
      </Shell>
    );
  }
  return (
    <Shell listing={listing}>
      <Tabs defaultValue="overview">
        <TabsList className="mb-6 flex h-auto flex-wrap justify-start">
          {["overview", "bookings", "schedule", "time-off", "reviews", "listing"].map((t) => <TabsTrigger key={t} value={t} className="capitalize">{t.replace("-", " ")}</TabsTrigger>)}
        </TabsList>
        <TabsContent value="overview"><Overview /></TabsContent>
        <TabsContent value="bookings"><Bookings /></TabsContent>
        <TabsContent value="schedule"><Schedule /></TabsContent>
        <TabsContent value="time-off"><TimeOffPanel /></TabsContent>
        <TabsContent value="reviews"><Reviews /></TabsContent>
        <TabsContent value="listing"><div className="max-w-3xl"><ListingForm listing={listing} /></div></TabsContent>
      </Tabs>
    </Shell>
  );
};

const Shell = ({ children, listing }: { children: React.ReactNode; listing?: Trainer }) => (
  <div className="min-h-screen bg-background">
    <Navbar solid />
    <main className="container pb-16 pt-28">
      <header className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-border pb-6">
        <div>
          <p className="text-xs font-bold uppercase text-accent">Coach hub</p>
          <h1 className="mt-1 font-display text-5xl text-foreground">{listing ? listing.name : "Welcome, coach"}</h1>
        </div>
        {listing && (
          <div className="flex items-center gap-3">
            <span className={`rounded-full px-3 py-1 text-xs font-bold ${STATUS_STYLE[listing.status]}`}>{listing.status}</span>
            {listing.status === "ACTIVE" && <Button asChild variant="outline"><Link to={`/trainers/${listing.id}`}>View public page</Link></Button>}
          </div>
        )}
      </header>
      {listing?.status === "PENDING" && <p className="mb-6 rounded-lg bg-amber-50 p-4 text-sm text-amber-900">Your listing is awaiting admin approval. You can set up your schedule in the meantime.</p>}
      {listing?.statusReason && listing.status !== "ACTIVE" && <p className="mb-6 rounded-lg bg-red-50 p-4 text-sm text-red-900">Note from our team: {listing.statusReason}</p>}
      {children}
    </main>
  </div>
);

const Stat = ({ icon: Icon, label, value }: { icon: typeof Star; label: string; value: React.ReactNode }) => (
  <div className="rounded-xl border border-border bg-card p-4">
    <p className="flex items-center gap-2 text-xs font-semibold uppercase text-muted-foreground"><Icon className="h-4 w-4 text-accent" />{label}</p>
    <p className="mt-2 font-display text-3xl">{value}</p>
  </div>
);

const Overview = () => {
  const { data: s } = useQuery({ queryKey: ["coach", "stats"], queryFn: () => api<CoachStats>("/coach/stats") });
  if (!s) return <p className="text-muted-foreground">Loading…</p>;
  return (
    <div className="space-y-8">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat icon={CalendarDays} label="Upcoming" value={s.upcomingSessions} />
        <Stat icon={CheckCircle2} label="Completed" value={s.completedSessions} />
        <Stat icon={PoundSterling} label="Earned this month" value={money(s.earningsThisMonth)} />
        <Stat icon={PoundSterling} label="Booked (not yet delivered)" value={money(s.pendingEarnings)} />
        <Stat icon={Users} label="Unique trainees" value={s.uniqueTrainees} />
        <Stat icon={Star} label="Rating" value={`${s.rating || "–"} (${s.reviews})`} />
        <Stat icon={PoundSterling} label="Total earned" value={money(s.totalEarnings)} />
        <Stat icon={UserX} label="No-shows / cancelled" value={`${s.noShows} / ${s.cancelledSessions}`} />
      </div>
      <section>
        <h2 className="mb-4 font-display text-3xl">Next sessions</h2>
        {s.nextSessions.length === 0 ? <p className="text-sm text-muted-foreground">No upcoming sessions.</p> :
          <ul className="space-y-2">{s.nextSessions.map((b) => <BookingRow key={b.id} b={b} />)}</ul>}
      </section>
    </div>
  );
};

const BookingRow = ({ b, actions }: { b: Booking; actions?: React.ReactNode }) => (
  <li className="flex flex-wrap items-center gap-4 rounded-xl border border-border bg-card p-4">
    <div className="w-28 shrink-0">
      <p className="font-semibold">{format(parseISO(b.date), "EEE d MMM")}</p>
      <p className="flex items-center gap-1 text-xs text-muted-foreground"><Clock className="h-3 w-3" />{b.slot}</p>
    </div>
    <div className="min-w-0 flex-1">
      <p className="font-semibold">{b.traineeName}</p>
      <p className="text-xs text-muted-foreground">{[b.traineeEmail, b.traineePhone].filter(Boolean).join(" · ")}</p>
      {b.notes && <p className="mt-1 text-xs italic text-muted-foreground">“{b.notes}”</p>}
      {b.cancellationReason && <p className="mt-1 text-xs text-destructive">Cancelled by {b.cancelledBy}: {b.cancellationReason}</p>}
    </div>
    <span className="text-sm font-semibold">{money(b.price)}</span>
    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold">{b.status.replace("_", " ")}{b.lateCancellation ? " · late" : ""}</span>
    {actions}
  </li>
);

const Bookings = () => {
  const [status, setStatus] = useState<BookingStatus | "">("CONFIRMED");
  const qc = useQueryClient();
  const { data } = useQuery({
    queryKey: ["coach", "bookings", status],
    queryFn: () => api<Page<Booking>>("/coach/bookings", { query: { status: status || undefined, size: 100 } }),
  });
  const act = async (b: Booking, next: BookingStatus) => {
    let reason: string | undefined;
    if (next === "CANCELLED") {
      const r = window.prompt("Reason for cancelling (sent to the trainee):");
      if (r === null) return;
      reason = r;
    }
    try {
      await api(`/coach/bookings/${b.id}`, { method: "PATCH", body: { status: next, reason } });
      toast.success(`Session marked ${next.replace("_", " ").toLowerCase()}`);
      qc.invalidateQueries({ queryKey: ["coach"] });
    } catch (e) { toast.error(errorMessage(e)); }
  };
  const started = (b: Booking) => new Date(b.startsAt) <= new Date();
  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-2">
        {(["CONFIRMED", "COMPLETED", "NO_SHOW", "CANCELLED", ""] as const).map((s) => (
          <button key={s || "all"} onClick={() => setStatus(s)}
            className={`rounded-full border px-3 py-1.5 text-xs font-bold uppercase ${status === s ? "border-accent bg-accent text-accent-foreground" : "border-border"}`}>
            {s ? s.replace("_", " ") : "All"}
          </button>
        ))}
      </div>
      {(data?.content ?? []).length === 0 ? <p className="text-sm text-muted-foreground">No bookings.</p> : (
        <ul className="space-y-2">
          {data!.content.map((b) => (
            <BookingRow key={b.id} b={b} actions={b.status === "CONFIRMED" && (
              <div className="flex gap-2">
                {started(b) ? <>
                  <Button size="sm" variant="outline" onClick={() => act(b, "COMPLETED")}><CheckCircle2 /> Completed</Button>
                  <Button size="sm" variant="outline" onClick={() => act(b, "NO_SHOW")}><UserX /> No-show</Button>
                </> : <Button size="sm" variant="outline" onClick={() => act(b, "CANCELLED")}><XCircle /> Cancel</Button>}
              </div>
            )} />
          ))}
        </ul>
      )}
    </div>
  );
};

const Schedule = () => {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["coach", "schedule"], queryFn: () => api<WeeklySchedule>("/coach/schedule") });
  const [days, setDays] = useState<Record<WeekDay, string[]> | null>(null);
  useEffect(() => { if (data) setDays(data.days); }, [data]);
  if (!data || !days) return <p className="text-muted-foreground">Loading…</p>;
  const toggle = (d: WeekDay, s: string) =>
    setDays({ ...days, [d]: days[d].includes(s) ? days[d].filter((x) => x !== s) : [...days[d], s].sort() });
  const save = async () => {
    try {
      await api("/coach/schedule", { method: "PUT", body: { days } });
      toast.success("Weekly schedule saved");
      qc.invalidateQueries({ queryKey: ["coach", "schedule"] });
      qc.invalidateQueries({ queryKey: ["availability"] });
    } catch (e) { toast.error(errorMessage(e)); }
  };
  return (
    <div>
      <p className="mb-4 text-sm text-muted-foreground">Tick the hours you normally coach. Trainees can book any ticked slot that isn't taken.</p>
      <div className="overflow-x-auto">
        <table className="text-sm">
          <thead><tr><th className="p-2" />{data.allSlots.map((s) => <th key={s} className="p-2 font-semibold">{s}</th>)}</tr></thead>
          <tbody>
            {DAYS.map((d) => (
              <tr key={d}>
                <th className="p-2 text-left font-semibold capitalize">{d.slice(0, 3).toLowerCase()}</th>
                {data.allSlots.map((s) => (
                  <td key={s} className="p-1 text-center">
                    <input type="checkbox" aria-label={`${d} ${s}`} checked={days[d].includes(s)} onChange={() => toggle(d, s)} className="h-5 w-5 accent-[hsl(var(--accent))]" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Button onClick={save} className="mt-6 bg-accent text-accent-foreground hover:bg-accent/90">Save schedule</Button>
    </div>
  );
};

const TimeOffPanel = () => {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["coach", "time-off"], queryFn: () => api<TimeOff[]>("/coach/time-off") });
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [reason, setReason] = useState("");
  const add = async (e: FormEvent, cancelExistingBookings = false) => {
    e.preventDefault();
    try {
      const r = await api<{ added: TimeOff[]; cancelledBookings: number }>("/coach/time-off", { method: "POST", body: { from, to: to || undefined, reason: reason || undefined, cancelExistingBookings } });
      toast.success(`${r.added.length} day(s) blocked${r.cancelledBookings ? `, ${r.cancelledBookings} session(s) cancelled` : ""}`);
      setFrom(""); setTo(""); setReason("");
      qc.invalidateQueries({ queryKey: ["coach"] });
    } catch (err) {
      if (err instanceof ApiError && err.status === 409 && window.confirm(`${err.message}\n\nCancel those sessions and notify the trainees?`)) return add(e, true);
      if (!(err instanceof ApiError && err.status === 409)) toast.error(errorMessage(err));
    }
  };
  const remove = async (id: string) => {
    try { await api(`/coach/time-off/${id}`, { method: "DELETE" }); qc.invalidateQueries({ queryKey: ["coach", "time-off"] }); }
    catch (e) { toast.error(errorMessage(e)); }
  };
  return (
    <div className="grid gap-8 lg:grid-cols-2">
      <form onSubmit={add} className="space-y-4">
        <h2 className="font-display text-3xl">Block days</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <AccountField label="From" type="date" value={from} onChange={setFrom} required />
          <AccountField label="To (optional)" type="date" value={to} onChange={setTo} />
        </div>
        <AccountField label="Reason (shown to trainees)" value={reason} onChange={setReason} maxLength={200} placeholder="e.g. Tournament" />
        <Button type="submit" className="bg-accent text-accent-foreground hover:bg-accent/90">Add time off</Button>
      </form>
      <div>
        <h2 className="mb-4 font-display text-3xl">Upcoming time off</h2>
        {(data ?? []).length === 0 ? <p className="text-sm text-muted-foreground">None scheduled.</p> : (
          <ul className="space-y-2">{data!.map((t) => (
            <li key={t.id} className="flex items-center justify-between rounded-lg border border-border p-3 text-sm">
              <span><b>{format(parseISO(t.date), "EEE d MMM yyyy")}</b>{t.reason ? ` · ${t.reason}` : ""}</span>
              <button onClick={() => remove(t.id)} className="text-xs font-semibold text-destructive hover:underline">Remove</button>
            </li>
          ))}</ul>
        )}
      </div>
    </div>
  );
};

const Reviews = () => {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["coach", "reviews"], queryFn: () => api<Page<Review>>("/coach/reviews", { query: { size: 50 } }) });
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const reply = async (r: Review) => {
    try {
      await api(`/coach/reviews/${r.id}/reply`, { method: "POST", body: { reply: drafts[r.id] } });
      toast.success("Reply posted");
      qc.invalidateQueries({ queryKey: ["coach", "reviews"] });
    } catch (e) { toast.error(errorMessage(e)); }
  };
  if ((data?.content ?? []).length === 0) return <p className="text-sm text-muted-foreground">No written reviews yet.</p>;
  return (
    <ul className="space-y-4">
      {data!.content.map((r) => (
        <li key={r.id} className="rounded-xl border border-border bg-card p-4">
          <div className="flex items-center justify-between"><p className="font-semibold">{r.authorName}</p><Stars value={r.rating} size={3} /></div>
          {r.comment && <p className="mt-2 text-sm">{r.comment}</p>}
          {r.reply ? <p className="mt-3 rounded-lg bg-muted p-3 text-sm text-muted-foreground">Your reply: {r.reply}</p> : (
            <div className="mt-3 flex gap-2">
              <input value={drafts[r.id] ?? ""} onChange={(e) => setDrafts({ ...drafts, [r.id]: e.target.value })} placeholder="Write a public reply…" maxLength={1000}
                className="h-10 flex-1 rounded-md border border-input bg-background px-3 text-sm" />
              <Button size="sm" disabled={!drafts[r.id]?.trim()} onClick={() => reply(r)}>Reply</Button>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
};

export const ListingForm = ({ listing, onSave }: { listing?: Trainer; onSave?: (body: Record<string, unknown>) => Promise<unknown> }) => {
  const qc = useQueryClient();
  const { sports } = useSports();
  const [f, setF] = useState({
    name: listing?.name ?? "", sport: listing?.sport ?? "", city: listing?.city ?? "",
    hourlyRate: String(listing?.hourlyRate ?? ""), experience: String(listing?.experience ?? ""),
    bio: listing?.bio ?? "", image: listing?.image ?? "", specialties: (listing?.specialties ?? []).join(", "),
    qualifications: listing?.qualifications ?? "",
  });
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (v: string) => setF((p) => ({ ...p, [k]: v }));
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const body = {
      ...f, hourlyRate: Number(f.hourlyRate), experience: Number(f.experience), image: f.image,
      specialties: f.specialties.split(",").map((s) => s.trim()).filter(Boolean),
    };
    setBusy(true);
    try {
      if (onSave) await onSave(body);
      else await api("/coach/listing", { method: "PUT", body });
      toast.success(listing ? "Listing updated" : "Listing submitted for review");
      qc.invalidateQueries({ queryKey: ["coach"] });
      qc.invalidateQueries({ queryKey: ["trainers"] });
    } catch (err) { toast.error(errorMessage(err)); } finally { setBusy(false); }
  };
  return (
    <form onSubmit={submit} className="grid gap-5 sm:grid-cols-2">
      <AccountField label="Display name" value={f.name} onChange={set("name")} required maxLength={100} />
      <label className="block"><span className="mb-1.5 block text-sm font-semibold">Sport</span>
        <select required value={f.sport} onChange={(e) => set("sport")(e.target.value)} className="h-11 w-full rounded-md border border-input bg-background px-3 text-sm">
          <option value="">Select sport</option>{sports.map((s) => <option key={s.name}>{s.name}</option>)}
        </select>
      </label>
      <AccountField label="City" value={f.city} onChange={set("city")} required maxLength={100} />
      <AccountField label="Hourly rate (£)" type="number" min="5" max="1000" step="0.5" value={f.hourlyRate} onChange={set("hourlyRate")} required />
      <AccountField label="Years of experience" type="number" min="0" max="80" value={f.experience} onChange={set("experience")} required />
      <AccountField label="Qualifications" value={f.qualifications} onChange={set("qualifications")} maxLength={300} />
      <div className="sm:col-span-2"><AccountField label="Specialties (comma separated, up to 8)" value={f.specialties} onChange={set("specialties")} placeholder="Footwork, Match play" /></div>
      <div className="sm:col-span-2"><AccountField label="Photo URL" value={f.image} onChange={set("image")} maxLength={500} placeholder={`https://… or one of: ${IMAGE_KEYS.filter((k) => k.startsWith("trainer")).join(", ")}`} /></div>
      <label className="block sm:col-span-2"><span className="mb-1.5 block text-sm font-semibold">About you</span>
        <textarea value={f.bio} onChange={(e) => set("bio")(e.target.value)} rows={4} maxLength={1000} className="w-full rounded-md border border-input bg-background p-3 text-sm" />
      </label>
      <div className="sm:col-span-2"><Button type="submit" disabled={busy} className="bg-accent text-accent-foreground hover:bg-accent/90">{busy ? "Saving…" : listing ? "Save listing" : "Submit for review"}</Button></div>
    </form>
  );
};

const CoachDashboard = () => <RequireAuth roles={["trainer"]}><CoachDashboardPage /></RequireAuth>;
export default CoachDashboard;
