import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { Calendar, LogOut, MapPin, Mail, Pencil, Check, X } from "lucide-react";
import Navbar from "@/components/Navbar";
import { useAuth } from "@/context/AuthContext";
import { toast } from "sonner";

const Profile = () => {
  const { user, logout, updateProfile, bookings } = useAuth();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(user?.name ?? "");
  const [city, setCity] = useState(user?.city ?? "");

  if (!user) return <Navigate to="/login" replace />;

  const upcoming = bookings.filter((b) => new Date(b.date) >= new Date(new Date().toDateString())).length;

  const save = () => {
    updateProfile({ name, city });
    setEditing(false);
    toast.success("Profile updated");
  };

  const initials = user.name.split(" ").map((s) => s[0]).slice(0, 2).join("").toUpperCase();

  return (
    <div className="min-h-screen bg-background">
      <div className="bg-gradient-hero pt-24 pb-32">
        <Navbar />
        <div className="container">
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-accent">My Account</p>
          <h1 className="mt-2 font-display text-5xl text-primary-foreground md:text-6xl">Profile</h1>
        </div>
      </div>

      <div className="container -mt-20 pb-20">
        <div className="grid gap-6 md:grid-cols-3">
          {/* Profile card */}
          <div className="md:col-span-2 rounded-3xl border border-border bg-card p-8 shadow-card">
            <div className="flex items-start gap-5">
              <div className="grid h-20 w-20 shrink-0 place-items-center rounded-2xl bg-gradient-accent font-display text-3xl text-primary shadow-glow">
                {initials}
              </div>
              <div className="flex-1">
                {!editing ? (
                  <>
                    <h2 className="font-display text-3xl text-primary">{user.name}</h2>
                    <div className="mt-2 flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
                      <span className="flex items-center gap-1.5"><Mail className="h-4 w-4" />{user.email}</span>
                      <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{user.city}</span>
                    </div>
                  </>
                ) : (
                  <div className="space-y-3">
                    <input value={name} onChange={(e) => setName(e.target.value)}
                      className="w-full rounded-lg border-2 border-border bg-background px-3 py-2 text-lg font-semibold text-primary outline-none focus:border-accent" />
                    <input value={city} onChange={(e) => setCity(e.target.value)}
                      className="w-full rounded-lg border-2 border-border bg-background px-3 py-2 text-sm text-primary outline-none focus:border-accent" />
                  </div>
                )}
              </div>
              {!editing ? (
                <button onClick={() => setEditing(true)}
                  className="rounded-full border-2 border-border p-2.5 text-primary transition-colors hover:border-accent">
                  <Pencil className="h-4 w-4" />
                </button>
              ) : (
                <div className="flex gap-2">
                  <button onClick={save} className="rounded-full bg-accent p-2.5 text-accent-foreground"><Check className="h-4 w-4" /></button>
                  <button onClick={() => { setEditing(false); setName(user.name); setCity(user.city); }}
                    className="rounded-full border-2 border-border p-2.5 text-primary"><X className="h-4 w-4" /></button>
                </div>
              )}
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3 border-t border-border pt-6">
              <Stat label="Total Bookings" value={bookings.length} />
              <Stat label="Upcoming" value={upcoming} />
            </div>
          </div>

          {/* Quick actions */}
          <div className="space-y-4">
            <Link to="/my-calendar"
              className="block rounded-3xl border border-border bg-card p-6 shadow-card transition-transform hover:-translate-y-1">
              <div className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-accent shadow-glow">
                <Calendar className="h-5 w-5 text-primary" />
              </div>
              <h3 className="mt-3 font-display text-2xl text-primary">My Calendar</h3>
              <p className="mt-1 text-sm text-muted-foreground">View and manage your scheduled training sessions.</p>
            </Link>

            <button onClick={() => { logout(); toast.success("Signed out"); }}
              className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-border bg-card px-6 py-4 text-sm font-bold uppercase tracking-wider text-primary transition-colors hover:border-destructive hover:text-destructive">
              <LogOut className="h-4 w-4" /> Sign Out
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const Stat = ({ label, value }: { label: string; value: number }) => (
  <div className="rounded-xl bg-muted/50 p-4">
    <p className="font-display text-3xl text-primary">{value}</p>
    <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{label}</p>
  </div>
);

export default Profile;
