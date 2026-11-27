import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { format, parseISO } from "date-fns";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import Navbar from "@/components/Navbar";
import RequireAuth from "@/components/RequireAuth";
import AccountField from "@/components/AccountField";
import Stars from "@/components/Stars";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useAuth } from "@/context/AuthContext";
import { useSports } from "@/hooks/use-api";
import { api, errorMessage } from "@/lib/api";
import { money } from "@/lib/format";
import type { ApiUser, AuditLog, Booking, BookingStatus, DashboardStats, Page, Review, Role, Sport, Trainer, TrainerStatus, Venue, VenueBooking } from "@/lib/types";

const useInvalidate = () => {
  const qc = useQueryClient();
  return (...keys: string[]) => keys.forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
};

const run = async (fn: () => Promise<unknown>, ok: string, after?: () => void) => {
  try { await fn(); toast.success(ok); after?.(); } catch (e) { toast.error(errorMessage(e)); }
};

const Th = ({ children }: { children?: React.ReactNode }) => <th className="whitespace-nowrap border-b border-border px-3 py-2 text-left text-xs font-bold uppercase text-muted-foreground">{children}</th>;
const Td = ({ children, className = "" }: { children?: React.ReactNode; className?: string }) => <td className={`border-b border-border px-3 py-2 align-top text-sm ${className}`}>{children}</td>;
const Table = ({ head, children }: { head: string[]; children: React.ReactNode }) => (
  <div className="overflow-x-auto rounded-xl border border-border bg-card">
    <table className="w-full min-w-[640px]"><thead><tr>{head.map((h) => <Th key={h}>{h}</Th>)}</tr></thead><tbody>{children}</tbody></table>
  </div>
);
const Pager = ({ page, data, setPage }: { page: number; data?: Page<unknown>; setPage: (p: number) => void }) =>
  data && data.totalPages > 1 ? (
    <div className="mt-3 flex items-center gap-3 text-sm">
      <Button size="sm" variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
      <span>Page {page + 1} of {data.totalPages}</span>
      <Button size="sm" variant="outline" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</Button>
    </div>
  ) : null;
const Search = ({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) => (
  <input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} className="h-10 w-full max-w-xs rounded-md border border-input bg-background px-3 text-sm" />
);
const Select = <T extends string>({ value, onChange, options, label }: { value: T | ""; onChange: (v: T | "") => void; options: readonly T[]; label: string }) => (
  <select aria-label={label} value={value} onChange={(e) => onChange(e.target.value as T | "")} className="h-10 rounded-md border border-input bg-background px-2 text-sm">
    <option value="">All {label}</option>{options.map((o) => <option key={o} value={o}>{o}</option>)}
  </select>
);

const AdminDashboardPage = () => (
  <div className="min-h-screen bg-background">
    <Navbar solid />
    <main className="container pb-16 pt-28">
      <header className="mb-8 border-b border-border pb-6">
        <p className="text-xs font-bold uppercase text-accent">Admin console</p>
        <h1 className="mt-1 font-display text-5xl">Platform management</h1>
      </header>
      <Tabs defaultValue="overview">
        <TabsList className="mb-6 flex h-auto flex-wrap justify-start">
          {["overview", "coaches", "users", "bookings", "venues", "reviews", "sports", "audit"].map((t) => <TabsTrigger key={t} value={t} className="capitalize">{t}</TabsTrigger>)}
        </TabsList>
        <TabsContent value="overview"><Overview /></TabsContent>
        <TabsContent value="coaches"><Coaches /></TabsContent>
        <TabsContent value="users"><Users /></TabsContent>
        <TabsContent value="bookings"><Bookings /></TabsContent>
        <TabsContent value="venues"><Venues /></TabsContent>
        <TabsContent value="reviews"><Reviews /></TabsContent>
        <TabsContent value="sports"><Sports /></TabsContent>
        <TabsContent value="audit"><Audit /></TabsContent>
      </Tabs>
    </main>
  </div>
);

const Card = ({ label, value, sub }: { label: string; value: React.ReactNode; sub?: string }) => (
  <div className="rounded-xl border border-border bg-card p-4">
    <p className="text-xs font-semibold uppercase text-muted-foreground">{label}</p>
    <p className="mt-2 font-display text-3xl">{value}</p>
    {sub && <p className="text-xs text-muted-foreground">{sub}</p>}
  </div>
);

const Overview = () => {
  const { data: s } = useQuery({ queryKey: ["admin-dashboard"], queryFn: () => api<DashboardStats>("/admin/dashboard") });
  if (!s) return <p className="text-muted-foreground">Loading…</p>;
  const max = Math.max(1, ...s.bookingsNext14Days.map((d) => d.count));
  return (
    <div className="space-y-8">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card label="Users" value={s.totalUsers} sub={`${s.usersByRole.trainee} trainees · ${s.usersByRole.trainer} coaches · ${s.disabledUsers} disabled`} />
        <Card label="Live coaches" value={s.trainersByStatus.ACTIVE} sub={`${s.trainersByStatus.PENDING} awaiting approval`} />
        <Card label="Upcoming sessions" value={s.bookingsByStatus.CONFIRMED} sub={`${s.bookingsLast7Days} booked in the last 7 days`} />
        <Card label="Active venues" value={s.activeVenues} sub={`${s.venueBookings} venue bookings`} />
        <Card label="Coaching revenue (delivered)" value={money(s.completedRevenue)} />
        <Card label="Coaching revenue (booked)" value={money(s.upcomingRevenue)} />
        <Card label="Venue revenue" value={money(s.venueRevenue)} />
        <Card label="Cancelled / no-show" value={`${s.bookingsByStatus.CANCELLED} / ${s.bookingsByStatus.NO_SHOW}`} />
      </div>
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        <section>
          <h2 className="mb-3 font-display text-2xl">Sessions in the next 14 days</h2>
          <div className="flex h-40 items-end gap-1 rounded-xl border border-border bg-card p-4">
            {s.bookingsNext14Days.map((d) => (
              <div key={d.date} className="flex flex-1 flex-col items-center gap-1" title={`${d.date}: ${d.count}`}>
                <div className="w-full rounded-t bg-accent" style={{ height: `${(d.count / max) * 100}px` }} />
                <span className="text-[10px] text-muted-foreground">{format(parseISO(d.date), "d")}</span>
              </div>
            ))}
          </div>
        </section>
        <section>
          <h2 className="mb-3 font-display text-2xl">Top coaches</h2>
          <Table head={["Coach", "Sessions", "Revenue"]}>
            {s.topTrainers.map((t) => <tr key={t.trainerId}><Td><Link className="font-semibold hover:text-accent" to={`/trainers/${t.trainerId}`}>{t.name}</Link></Td><Td>{t.sessions}</Td><Td>{money(t.revenue)}</Td></tr>)}
          </Table>
          <p className="mt-3 text-xs text-muted-foreground">By sport: {Object.entries(s.bookingsBySport).map(([k, v]) => `${k} ${v}`).join(" · ") || "–"}</p>
        </section>
      </div>
    </div>
  );
};

const TRAINER_STATUSES = ["PENDING", "ACTIVE", "SUSPENDED", "REJECTED"] as const;
const Coaches = () => {
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<TrainerStatus | "">("");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Trainer | null>(null);
  const inv = useInvalidate();
  const { data } = useQuery({ queryKey: ["admin-trainers", q, status, page], queryFn: () => api<Page<Trainer>>("/admin/trainers", { query: { q, status, page } }) });
  const setStat = (t: Trainer, s: TrainerStatus) => {
    const reason = s === "ACTIVE" ? undefined : window.prompt(`Reason for ${s.toLowerCase()} (sent to the coach):`) ?? undefined;
    run(() => api(`/admin/trainers/${t.id}/status`, { method: "PATCH", body: { status: s, reason } }), `${t.name} is now ${s.toLowerCase()}`, () => inv("admin-trainers", "trainers", "admin-dashboard"));
  };
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-3"><Search value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Search coaches…" /><Select label="statuses" value={status} onChange={(v) => { setStatus(v); setPage(0); }} options={TRAINER_STATUSES} /></div>
      <Table head={["Coach", "Sport / city", "Rate", "Rating", "Status", "Actions"]}>
        {(data?.content ?? []).map((t) => (
          <tr key={t.id}>
            <Td><p className="font-semibold">{t.name}</p><p className="text-xs text-muted-foreground">{t.id}</p></Td>
            <Td>{t.sport}<br /><span className="text-xs text-muted-foreground">{t.city}</span></Td>
            <Td>{money(t.hourlyRate)}</Td>
            <Td>{t.rating || "–"} ({t.reviews})</Td>
            <Td><span className="font-semibold">{t.status}</span>{t.featured && <span className="ml-1 text-xs text-accent">★ featured</span>}{t.statusReason && <p className="text-xs text-muted-foreground">{t.statusReason}</p>}</Td>
            <Td className="space-x-1 whitespace-nowrap">
              {t.status !== "ACTIVE" && <Button size="sm" onClick={() => setStat(t, "ACTIVE")}>Approve</Button>}
              {t.status === "PENDING" && <Button size="sm" variant="outline" onClick={() => setStat(t, "REJECTED")}>Reject</Button>}
              {t.status === "ACTIVE" && <Button size="sm" variant="outline" onClick={() => setStat(t, "SUSPENDED")}>Suspend</Button>}
              <Button size="sm" variant="outline" onClick={() => run(() => api(`/admin/trainers/${t.id}/featured`, { method: "PATCH", body: { featured: !t.featured } }), t.featured ? "Unfeatured" : "Featured", () => inv("admin-trainers", "trainers"))}>{t.featured ? "Unfeature" : "Feature"}</Button>
              <Button size="sm" variant="ghost" onClick={() => setEditing(t)}>Edit</Button>
            </Td>
          </tr>
        ))}
      </Table>
      <Pager page={page} data={data} setPage={setPage} />
      <TrainerEditDialog trainer={editing} onClose={() => setEditing(null)} onSaved={() => inv("admin-trainers", "trainers", "trainer")} />
    </div>
  );
};

const TrainerEditDialog = ({ trainer, onClose, onSaved }: { trainer: Trainer | null; onClose: () => void; onSaved: () => void }) => {
  const { sports } = useSports();
  const [f, setF] = useState<Record<string, string>>({});
  const val = (k: string, d: unknown) => f[k] ?? String(d ?? "");
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!trainer) return;
    const body = {
      name: val("name", trainer.name), sport: val("sport", trainer.sport), city: val("city", trainer.city),
      hourlyRate: Number(val("hourlyRate", trainer.hourlyRate)), experience: Number(val("experience", trainer.experience)),
      bio: val("bio", trainer.bio), image: val("image", trainer.image), qualifications: val("qualifications", trainer.qualifications),
      specialties: val("specialties", trainer.specialties.join(", ")).split(",").map((s) => s.trim()).filter(Boolean),
    };
    run(() => api(`/admin/trainers/${trainer.id}`, { method: "PUT", body }), "Coach updated", () => { onSaved(); setF({}); onClose(); });
  };
  return (
    <Dialog open={!!trainer} onOpenChange={(o) => { if (!o) { setF({}); onClose(); } }}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
        <DialogHeader><DialogTitle>Edit {trainer?.name}</DialogTitle></DialogHeader>
        {trainer && (
          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
            <AccountField label="Name" value={val("name", trainer.name)} onChange={(v) => setF({ ...f, name: v })} required />
            <label className="block"><span className="mb-1.5 block text-sm font-semibold">Sport</span>
              <select value={val("sport", trainer.sport)} onChange={(e) => setF({ ...f, sport: e.target.value })} className="h-11 w-full rounded-md border border-input bg-background px-3 text-sm">{sports.map((s) => <option key={s.name}>{s.name}</option>)}</select>
            </label>
            <AccountField label="City" value={val("city", trainer.city)} onChange={(v) => setF({ ...f, city: v })} required />
            <AccountField label="Hourly rate" type="number" value={val("hourlyRate", trainer.hourlyRate)} onChange={(v) => setF({ ...f, hourlyRate: v })} required />
            <AccountField label="Experience (years)" type="number" value={val("experience", trainer.experience)} onChange={(v) => setF({ ...f, experience: v })} required />
            <AccountField label="Image" value={val("image", trainer.image)} onChange={(v) => setF({ ...f, image: v })} />
            <div className="sm:col-span-2"><AccountField label="Specialties" value={val("specialties", trainer.specialties.join(", "))} onChange={(v) => setF({ ...f, specialties: v })} /></div>
            <div className="sm:col-span-2"><AccountField label="Qualifications" value={val("qualifications", trainer.qualifications)} onChange={(v) => setF({ ...f, qualifications: v })} /></div>
            <label className="block sm:col-span-2"><span className="mb-1.5 block text-sm font-semibold">Bio</span>
              <textarea rows={3} value={val("bio", trainer.bio)} onChange={(e) => setF({ ...f, bio: e.target.value })} className="w-full rounded-md border border-input bg-background p-3 text-sm" /></label>
            <Button type="submit" className="sm:col-span-2">Save</Button>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
};

const ROLES = ["trainee", "trainer", "admin"] as const;
const Users = () => {
  const { user: me } = useAuth();
  const [q, setQ] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [page, setPage] = useState(0);
  const inv = useInvalidate();
  const { data } = useQuery({ queryKey: ["admin-users", q, role, page], queryFn: () => api<Page<ApiUser>>("/admin/users", { query: { q, role, page } }) });
  const update = (u: ApiUser, body: { role?: Role; enabled?: boolean }, msg: string) =>
    run(() => api(`/admin/users/${u.id}`, { method: "PATCH", body }), msg, () => inv("admin-users", "admin-dashboard"));
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-3"><Search value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Name, email or city…" /><Select label="roles" value={role} onChange={(v) => { setRole(v); setPage(0); }} options={ROLES} /></div>
      <Table head={["User", "Role", "City", "Joined", "Last sign-in", "Status", ""]}>
        {(data?.content ?? []).map((u) => (
          <tr key={u.id} className={u.enabled ? "" : "opacity-60"}>
            <Td><p className="font-semibold">{u.name}</p><p className="text-xs text-muted-foreground">{u.email} · {u.provider}</p></Td>
            <Td>
              <select aria-label={`Role of ${u.name}`} value={u.role} disabled={u.id === me?.id} onChange={(e) => update(u, { role: e.target.value as Role }, "Role updated")} className="rounded border border-input bg-background px-1 py-0.5 text-sm">
                {ROLES.map((r) => <option key={r}>{r}</option>)}
              </select>
            </Td>
            <Td>{u.city || "–"}</Td>
            <Td>{format(new Date(u.createdAt), "d MMM yyyy")}</Td>
            <Td>{u.lastLoginAt ? format(new Date(u.lastLoginAt), "d MMM HH:mm") : "–"}</Td>
            <Td>{u.enabled ? "Active" : "Disabled"}</Td>
            <Td>{u.id !== me?.id && <Button size="sm" variant="outline" onClick={() => update(u, { enabled: !u.enabled }, u.enabled ? "User disabled" : "User enabled")}>{u.enabled ? "Disable" : "Enable"}</Button>}</Td>
          </tr>
        ))}
      </Table>
      <Pager page={page} data={data} setPage={setPage} />
    </div>
  );
};

const BOOKING_STATUSES = ["CONFIRMED", "COMPLETED", "NO_SHOW", "CANCELLED"] as const;
const Bookings = () => {
  const [status, setStatus] = useState<BookingStatus | "">("");
  const [page, setPage] = useState(0);
  const [vpage, setVpage] = useState(0);
  const inv = useInvalidate();
  const { data } = useQuery({ queryKey: ["admin-bookings", status, page], queryFn: () => api<Page<Booking>>("/admin/bookings", { query: { status, page } }) });
  const { data: vb } = useQuery({ queryKey: ["admin-venue-bookings", vpage], queryFn: () => api<Page<VenueBooking>>("/admin/venue-bookings", { query: { page: vpage } }) });
  const setStat = (b: Booking, s: BookingStatus) => {
    const reason = s === "CANCELLED" ? window.prompt("Reason (sent to trainee and coach):") ?? undefined : undefined;
    run(() => api(`/admin/bookings/${b.id}`, { method: "PATCH", body: { status: s, reason } }), "Booking updated", () => inv("admin-bookings", "admin-dashboard"));
  };
  return (
    <div className="space-y-8">
      <section className="space-y-4">
        <div className="flex items-center justify-between gap-3"><h2 className="font-display text-2xl">Coaching sessions</h2><Select label="statuses" value={status} onChange={(v) => { setStatus(v); setPage(0); }} options={BOOKING_STATUSES} /></div>
        <Table head={["When", "Coach", "Trainee", "Price", "Status", ""]}>
          {(data?.content ?? []).map((b) => (
            <tr key={b.id}>
              <Td>{format(parseISO(b.date), "EEE d MMM yyyy")}<br /><span className="text-xs text-muted-foreground">{b.slot}</span></Td>
              <Td>{b.trainerName}<br /><span className="text-xs text-muted-foreground">{b.sport}</span></Td>
              <Td>{b.traineeName}<br /><span className="text-xs text-muted-foreground">{b.traineeEmail}</span></Td>
              <Td>{money(b.price)}</Td>
              <Td>{b.status}{b.cancelledBy && <p className="text-xs text-muted-foreground">by {b.cancelledBy}{b.cancellationReason ? `: ${b.cancellationReason}` : ""}</p>}</Td>
              <Td className="space-x-1 whitespace-nowrap">{b.status === "CONFIRMED" && <>
                <Button size="sm" variant="outline" onClick={() => setStat(b, "CANCELLED")}>Cancel</Button>
                {new Date(b.startsAt) <= new Date() && <Button size="sm" variant="outline" onClick={() => setStat(b, "COMPLETED")}>Complete</Button>}
              </>}</Td>
            </tr>
          ))}
        </Table>
        <Pager page={page} data={data} setPage={setPage} />
      </section>
      <section className="space-y-4">
        <h2 className="font-display text-2xl">Venue bookings</h2>
        <Table head={["When", "Venue", "Booked by", "Total", "Status", ""]}>
          {(vb?.content ?? []).map((b) => (
            <tr key={b.id}>
              <Td>{format(parseISO(b.date), "EEE d MMM yyyy")}<br /><span className="text-xs text-muted-foreground">{b.startTime.slice(0, 5)}–{b.endTime.slice(0, 5)}</span></Td>
              <Td>{b.venueName}</Td>
              <Td>{b.userName}<br /><span className="text-xs text-muted-foreground">{b.userEmail}</span></Td>
              <Td>{money(b.totalPrice)}</Td>
              <Td>{b.status}</Td>
              <Td>{b.status === "CONFIRMED" && <Button size="sm" variant="outline" onClick={() => run(() => api(`/admin/venue-bookings/${b.id}/cancel`, { method: "POST", body: { reason: window.prompt("Reason:") ?? undefined } }), "Venue booking cancelled", () => inv("admin-venue-bookings"))}>Cancel</Button>}</Td>
            </tr>
          ))}
        </Table>
        <Pager page={vpage} data={vb} setPage={setVpage} />
      </section>
    </div>
  );
};

const VENUE_TYPES = ["Court", "Pitch", "Stadium", "Club", "Pool", "Hall"] as const;
const emptyVenue = { name: "", type: "Court", sport: "", city: "", address: "", description: "", pricePerHour: "", capacity: "1", openTime: "07:00", closeTime: "22:00", image: "", amenities: "", rating: "" };
const Venues = () => {
  const inv = useInvalidate();
  const { sports } = useSports();
  const { data } = useQuery({ queryKey: ["admin-venues"], queryFn: () => api<Page<Venue>>("/admin/venues", { query: { size: 100 } }) });
  const [editing, setEditing] = useState<Venue | "new" | null>(null);
  const [f, setF] = useState<Record<string, string>>(emptyVenue);
  const open = (v: Venue | "new") => {
    setEditing(v);
    setF(v === "new" ? emptyVenue : {
      name: v.name, type: v.type, sport: v.sport, city: v.city, address: v.address ?? "", description: v.description ?? "",
      pricePerHour: String(v.pricePerHour), capacity: String(v.capacity), openTime: v.openTime.slice(0, 5), closeTime: v.closeTime.slice(0, 5),
      image: v.image ?? "", amenities: v.amenities.join(", "), rating: String(v.rating),
    });
  };
  const save = (e: FormEvent) => {
    e.preventDefault();
    const body = { ...f, pricePerHour: Number(f.pricePerHour), capacity: Number(f.capacity), rating: f.rating ? Number(f.rating) : undefined,
      amenities: f.amenities.split(",").map((s) => s.trim()).filter(Boolean), active: editing === "new" ? true : (editing as Venue).active };
    const req = editing === "new" ? api("/admin/venues", { method: "POST", body }) : api(`/admin/venues/${(editing as Venue).id}`, { method: "PUT", body });
    run(() => req, "Venue saved", () => { inv("admin-venues", "venues"); setEditing(null); });
  };
  const set = (k: string) => (v: string) => setF((p) => ({ ...p, [k]: v }));
  return (
    <div className="space-y-4">
      <Button onClick={() => open("new")}>Add venue</Button>
      <Table head={["Venue", "Type / sport", "City", "Price", "Capacity", "Hours", "Status", ""]}>
        {(data?.content ?? []).map((v) => (
          <tr key={v.id} className={v.active ? "" : "opacity-60"}>
            <Td className="font-semibold">{v.name}</Td><Td>{v.type} · {v.sport}</Td><Td>{v.city}</Td><Td>{money(v.pricePerHour)}/hr</Td>
            <Td>{v.capacity}</Td><Td>{v.openTime.slice(0, 5)}–{v.closeTime.slice(0, 5)}</Td><Td>{v.active ? "Active" : "Inactive"}</Td>
            <Td className="space-x-1 whitespace-nowrap">
              <Button size="sm" variant="outline" onClick={() => open(v)}>Edit</Button>
              {v.active
                ? <Button size="sm" variant="outline" onClick={() => window.confirm(`Deactivate ${v.name}?`) && run(() => api(`/admin/venues/${v.id}`, { method: "DELETE" }), "Venue deactivated", () => inv("admin-venues", "venues"))}>Deactivate</Button>
                : <Button size="sm" variant="outline" onClick={() => { open(v); setF((p) => ({ ...p })); }}>Reactivate…</Button>}
            </Td>
          </tr>
        ))}
      </Table>
      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
          <DialogHeader><DialogTitle>{editing === "new" ? "Add venue" : "Edit venue"}</DialogTitle></DialogHeader>
          <form onSubmit={save} className="grid gap-4 sm:grid-cols-2">
            <AccountField label="Name" value={f.name} onChange={set("name")} required />
            <label className="block"><span className="mb-1.5 block text-sm font-semibold">Type</span>
              <select value={f.type} onChange={(e) => set("type")(e.target.value)} className="h-11 w-full rounded-md border border-input bg-background px-3 text-sm">{VENUE_TYPES.map((t) => <option key={t}>{t}</option>)}</select></label>
            <label className="block"><span className="mb-1.5 block text-sm font-semibold">Sport</span>
              <select required value={f.sport} onChange={(e) => set("sport")(e.target.value)} className="h-11 w-full rounded-md border border-input bg-background px-3 text-sm"><option value="">Select</option>{sports.map((s) => <option key={s.name}>{s.name}</option>)}</select></label>
            <AccountField label="City" value={f.city} onChange={set("city")} required />
            <div className="sm:col-span-2"><AccountField label="Address" value={f.address} onChange={set("address")} /></div>
            <AccountField label="Price per hour (£)" type="number" min="0" step="0.5" value={f.pricePerHour} onChange={set("pricePerHour")} required />
            <AccountField label="Capacity (courts/pitches)" type="number" min="1" max="50" value={f.capacity} onChange={set("capacity")} required />
            <AccountField label="Opens" type="time" step="3600" value={f.openTime} onChange={set("openTime")} required />
            <AccountField label="Closes" type="time" step="3600" value={f.closeTime} onChange={set("closeTime")} required />
            <AccountField label="Rating (0–5)" type="number" min="0" max="5" step="0.1" value={f.rating} onChange={set("rating")} />
            <AccountField label="Image (URL or venue-tennis…)" value={f.image} onChange={set("image")} />
            <div className="sm:col-span-2"><AccountField label="Amenities (comma separated)" value={f.amenities} onChange={set("amenities")} /></div>
            <label className="block sm:col-span-2"><span className="mb-1.5 block text-sm font-semibold">Description</span>
              <textarea rows={3} value={f.description} onChange={(e) => set("description")(e.target.value)} className="w-full rounded-md border border-input bg-background p-3 text-sm" /></label>
            {editing !== "new" && editing && !editing.active && (
              <Button type="button" variant="outline" className="sm:col-span-2" onClick={() => run(() => api(`/admin/venues/${editing.id}`, { method: "PUT", body: { ...f, pricePerHour: Number(f.pricePerHour), capacity: Number(f.capacity), amenities: f.amenities.split(",").map((s) => s.trim()).filter(Boolean), active: true } }), "Venue reactivated", () => { inv("admin-venues", "venues"); setEditing(null); })}>Save and reactivate</Button>
            )}
            <Button type="submit" className="sm:col-span-2">Save</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

const Reviews = () => {
  const [page, setPage] = useState(0);
  const [lowOnly, setLowOnly] = useState(false);
  const inv = useInvalidate();
  const { data } = useQuery({ queryKey: ["admin-reviews", page, lowOnly], queryFn: () => api<Page<Review>>("/admin/reviews", { query: { page, maxRating: lowOnly ? 2 : undefined } }) });
  return (
    <div className="space-y-4">
      <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={lowOnly} onChange={(e) => { setLowOnly(e.target.checked); setPage(0); }} /> Only 1–2 star reviews</label>
      <Table head={["Review", "Coach", "Author", "Date", "Visibility", ""]}>
        {(data?.content ?? []).map((r) => (
          <tr key={r.id} className={r.hidden ? "opacity-60" : ""}>
            <Td><Stars value={r.rating} size={3} /><p className="mt-1 max-w-md">{r.comment || <i className="text-muted-foreground">No comment</i>}</p></Td>
            <Td>{r.trainerName}</Td><Td>{r.authorName}</Td><Td>{format(new Date(r.createdAt), "d MMM yyyy")}</Td>
            <Td>{r.hidden ? "Hidden" : "Public"}</Td>
            <Td className="space-x-1 whitespace-nowrap">
              <Button size="sm" variant="outline" onClick={() => run(() => api(`/admin/reviews/${r.id}`, { method: "PATCH", body: { hidden: !r.hidden } }), r.hidden ? "Review restored" : "Review hidden", () => inv("admin-reviews", "reviews"))}>{r.hidden ? "Unhide" : "Hide"}</Button>
              <Button size="sm" variant="ghost" onClick={() => window.confirm("Delete this review permanently?") && run(() => api(`/admin/reviews/${r.id}`, { method: "DELETE" }), "Review deleted", () => inv("admin-reviews", "reviews"))}>Delete</Button>
            </Td>
          </tr>
        ))}
      </Table>
      <Pager page={page} data={data} setPage={setPage} />
    </div>
  );
};

const Sports = () => {
  const inv = useInvalidate();
  const { data } = useQuery({ queryKey: ["admin-sports"], queryFn: () => api<Sport[]>("/admin/sports") });
  const [name, setName] = useState("");
  const [emoji, setEmoji] = useState("");
  const upsert = (body: Partial<Sport> & { name: string }) => run(() => api("/admin/sports", { method: "PUT", body }), "Sport saved", () => inv("admin-sports", "sports"));
  return (
    <div className="max-w-xl space-y-4">
      <form onSubmit={(e) => { e.preventDefault(); upsert({ name, emoji, sortOrder: (data?.length ?? 0) }); setName(""); setEmoji(""); }} className="flex items-end gap-3">
        <AccountField label="New sport" value={name} onChange={setName} required maxLength={50} />
        <div className="w-24"><AccountField label="Emoji" value={emoji} onChange={setEmoji} maxLength={16} /></div>
        <Button type="submit">Add</Button>
      </form>
      <Table head={["Sport", "Status", ""]}>
        {(data ?? []).map((s) => (
          <tr key={s.name}><Td>{s.emoji} {s.name}</Td><Td>{s.active ? "Offered" : "Hidden"}</Td>
            <Td><Button size="sm" variant="outline" onClick={() => upsert({ ...s, active: !s.active })}>{s.active ? "Hide" : "Offer"}</Button></Td></tr>
        ))}
      </Table>
    </div>
  );
};

const Audit = () => {
  const [page, setPage] = useState(0);
  const { data } = useQuery({ queryKey: ["admin-audit", page], queryFn: () => api<Page<AuditLog>>("/admin/audit-logs", { query: { page } }) });
  return (
    <div>
      <Table head={["When", "Actor", "Action", "Target", "Details"]}>
        {(data?.content ?? []).map((a) => (
          <tr key={a.id}><Td className="whitespace-nowrap">{format(new Date(a.createdAt), "d MMM HH:mm")}</Td><Td>{a.actorEmail ?? "system"}</Td>
            <Td className="font-mono text-xs">{a.action}</Td><Td className="text-xs">{a.targetType} {a.targetId}</Td><Td className="text-xs">{a.details}</Td></tr>
        ))}
      </Table>
      <Pager page={page} data={data} setPage={setPage} />
    </div>
  );
};

const AdminDashboard = () => <RequireAuth roles={["admin"]}><AdminDashboardPage /></RequireAuth>;
export default AdminDashboard;
