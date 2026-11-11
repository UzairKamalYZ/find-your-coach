import { FormEvent, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { Calendar, LogOut, MapPin, Mail, Pencil, Phone, ShieldCheck, Dumbbell, UserRound } from "lucide-react";
import Navbar from "@/components/Navbar";
import AccountField from "@/components/AccountField";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";
import { User } from "@/lib/demo-account";
import { toast } from "sonner";

const Profile = () => {
  const { user, logout, updateProfile, bookings } = useAuth();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<Partial<User>>({});
  if (!user) return <Navigate to="/login" replace />;
  const trainer = user.role === "trainer";
  const upcoming = bookings.filter((booking) => new Date(booking.date) >= new Date(new Date().toDateString())).length;
  const initials = user.name.split(" ").filter(Boolean).map((word) => word[0]).slice(0, 2).join("").toUpperCase();
  const field = (key: keyof User) => String((editing ? draft[key] : user[key]) ?? "");
  const change = (key: keyof User) => (value: string) => setDraft((previous) => ({ ...previous, [key]: value }));
  const startEditing = () => { setDraft({ ...user }); setEditing(true); };
  const save = (event: FormEvent) => {
    event.preventDefault();
    if (!draft.name?.trim()) return;
    updateProfile({ ...draft, name: draft.name.trim(), city: draft.city?.trim() ?? "" });
    setEditing(false);
    toast.success("Profile updated");
  };

  return (
    <div className="min-h-screen bg-background">
      <Navbar solid />
      <main className="container pb-16 pt-28">
        <header className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-border pb-6">
          <div><p className="text-xs font-bold uppercase text-accent">My account</p><h1 className="mt-1 font-display text-5xl text-foreground">Your profile</h1></div>
          <Button asChild variant="outline"><Link to="/my-calendar"><Calendar /> My calendar</Link></Button>
        </header>
        <div className="grid gap-10 lg:grid-cols-[280px_1fr]">
          <aside className="min-w-0">
            <div className="flex items-center gap-4 lg:flex-col lg:items-start">
              <div className="grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded-lg bg-accent font-display text-3xl text-accent-foreground">
                {user.avatar ? <img src={user.avatar} alt={user.name} className="h-full w-full object-cover" /> : initials}
              </div>
              <div className="min-w-0"><h2 className="break-words font-display text-3xl">{user.name}</h2><p className="mt-1 flex items-center gap-2 text-sm font-semibold capitalize text-accent">{trainer ? <Dumbbell className="h-4 w-4" /> : <UserRound className="h-4 w-4" />}{user.role}</p></div>
            </div>
            <div className="mt-6 space-y-3 border-b border-border pb-6 text-sm text-muted-foreground">
              <p className="flex items-start gap-2"><Mail className="mt-0.5 h-4 w-4 shrink-0" /><span className="break-all">{user.email}</span></p>
              <p className="flex items-center gap-2"><Phone className="h-4 w-4 shrink-0" />{user.phone || "Phone not added"}</p>
              <p className="flex items-center gap-2"><MapPin className="h-4 w-4 shrink-0" />{user.city || "City not added"}</p>
              <p className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 shrink-0" />{user.provider || "Email"} · Demo account</p>
            </div>
            <dl className="mt-6 grid grid-cols-2 gap-4"><div><dt className="text-xs text-muted-foreground">Total bookings</dt><dd className="font-display text-3xl">{bookings.length}</dd></div><div><dt className="text-xs text-muted-foreground">Upcoming</dt><dd className="font-display text-3xl">{upcoming}</dd></div></dl>
            <Button onClick={() => { logout(); toast.success("Signed out"); }} variant="outline" className="mt-6 w-full hover:bg-destructive hover:text-destructive-foreground"><LogOut /> Sign out</Button>
          </aside>
          <form onSubmit={save} className="min-w-0">
            <div className="mb-6 flex items-center justify-between gap-3"><h2 className="font-display text-3xl">Personal details</h2>
              {!editing && <Button type="button" variant="outline" onClick={startEditing}><Pencil /> Edit profile</Button>}
            </div>
            <div className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
              {editing ? <>
                <AccountField label="Full name" value={field("name")} onChange={change("name")} required maxLength={100} autoComplete="name" />
                <Detail label="Email" value={user.email} />
                <AccountField label="Phone" type="tel" value={field("phone")} onChange={change("phone")} autoComplete="tel" maxLength={40} />
                <AccountField label="City" value={field("city")} onChange={change("city")} autoComplete="address-level2" maxLength={100} />
              </> : <><Detail label="Full name" value={user.name} /><Detail label="Email" value={user.email} /><Detail label="Phone" value={user.phone} /><Detail label="City" value={user.city} /></>}
            </div>
            <section className="mt-8 border-t border-border pt-6">
              <h2 className="mb-5 font-display text-3xl">{trainer ? "Coaching profile" : "Training preferences"}</h2>
              <div className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
                {editing ? <>
                  <AccountField label={trainer ? "Sports you coach" : "Sports you enjoy"} value={field("sports")} onChange={change("sports")} placeholder="e.g. Tennis, Football" maxLength={200} />
                  {trainer ? <AccountField label="Years of coaching experience" type="number" min="0" max="80" value={field("experience")} onChange={change("experience")} /> :
                    <label className="block"><span className="mb-1.5 block text-sm font-semibold">Training level</span><select value={field("level")} onChange={(event) => change("level")(event.target.value)} className="h-11 w-full rounded-md border border-input bg-background px-3 text-sm"><option value="">Select level</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select></label>}
                  <AccountField label={trainer ? "Qualifications" : "Training goals"} value={field(trainer ? "qualifications" : "goals")} onChange={change(trainer ? "qualifications" : "goals")} placeholder={trainer ? "Your certifications" : "What would you like to achieve?"} maxLength={300} />
                </> : <>
                  <Detail label={trainer ? "Sports you coach" : "Sports you enjoy"} value={user.sports} />
                  <Detail label={trainer ? "Years of coaching experience" : "Training level"} value={trainer ? user.experience : user.level} />
                  <Detail label={trainer ? "Qualifications" : "Training goals"} value={trainer ? user.qualifications : user.goals} />
                </>}
              </div>
              <div className="mt-5">{editing ? <label className="block"><span className="mb-1.5 block text-sm font-semibold">About you</span><textarea value={field("bio")} onChange={(event) => change("bio")(event.target.value)} rows={4} maxLength={1000} placeholder="A little about you" className="w-full resize-y rounded-md border border-input bg-background p-3 text-sm outline-none focus:border-ring" /></label> : <Detail label="About you" value={user.bio} />}</div>
            </section>
            {editing && <div className="mt-6 flex justify-end gap-3 border-t border-border pt-6"><Button type="button" variant="outline" onClick={() => setEditing(false)}>Cancel</Button><Button type="submit" className="bg-accent text-accent-foreground hover:bg-accent/90">Save changes</Button></div>}
          </form>
        </div>
      </main>
    </div>
  );
};
const Detail = ({ label, value }: { label: string; value?: string }) => <div className="min-w-0"><p className="mb-1.5 text-sm font-semibold">{label}</p><p className={`whitespace-pre-wrap break-words text-sm ${value ? "text-foreground" : "text-muted-foreground"}`}>{value || "Not added"}</p></div>;
export default Profile;
