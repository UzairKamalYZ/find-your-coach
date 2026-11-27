import { Link, NavLink, useNavigate } from "react-router-dom";
import { useState } from "react";
import { Dumbbell, LogIn, Menu } from "lucide-react";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { useAuth } from "@/context/AuthContext";
import NotificationBell from "@/components/NotificationBell";

const Navbar = ({ solid = false }: { solid?: boolean }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const mobileCls = ({ isActive }: { isActive: boolean }) =>
    `block rounded-lg px-4 py-3 text-base font-semibold uppercase tracking-wider transition-colors ${isActive ? "bg-accent/15 text-accent" : "text-primary-foreground/85 hover:bg-primary-foreground/10"}`;

  const linkCls = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-semibold uppercase tracking-wider transition-colors hover:text-accent ${
      isActive ? "text-accent" : "text-primary-foreground/80"
    }`;

  const initials = user?.name.split(" ").map((s) => s[0]).slice(0, 2).join("").toUpperCase();

  return (
    <header className={`absolute top-0 left-0 right-0 z-50 ${solid ? "bg-primary" : ""}`}>
      <div className="container flex items-center justify-between gap-3 py-4 md:py-5">
        <Link to="/" className="flex items-center gap-2 text-primary-foreground">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-gradient-accent shadow-glow">
            <Dumbbell className="h-5 w-5 text-primary" />
          </span>
          <span className="font-display text-xl tracking-wide sm:text-2xl">
            Find<span className="text-accent">My</span>Trainer
          </span>
        </Link>
        <nav className="hidden items-center gap-8 md:flex">
          <NavLink to="/" end className={linkCls}>Home</NavLink>
          <NavLink to="/trainers" className={linkCls}>Trainers</NavLink>
          <NavLink to="/venues" className={linkCls}>Venues</NavLink>
          {user && <NavLink to="/my-calendar" className={linkCls}>My Calendar</NavLink>}
          {user?.role === "trainer" && <NavLink to="/coach" className={linkCls}>Coach Hub</NavLink>}
          {user?.role === "admin" && <NavLink to="/admin" className={linkCls}>Admin</NavLink>}
        </nav>

        <div className="flex items-center gap-2">
        {user ? (
          <div className="flex items-center gap-2">
          <NotificationBell />
          <button
            onClick={() => navigate("/profile")}
            className="flex items-center gap-2 rounded-full border-2 border-accent/40 bg-primary/40 px-3 py-1.5 backdrop-blur transition-colors hover:border-accent"
          >
            <span className="grid h-8 w-8 place-items-center rounded-full bg-gradient-accent text-xs font-bold text-primary">
              {initials}
            </span>
            <span className="hidden pr-2 text-sm font-semibold text-primary-foreground sm:block">{user.name.split(" ")[0]}</span>
          </button>
          </div>
        ) : (
          <Link
            to="/login"
            className="flex items-center gap-2 rounded-full bg-gradient-accent px-4 py-2 sm:px-5 sm:py-2.5 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105"
          >
            <LogIn className="h-4 w-4" /> Sign In
          </Link>
        )}
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <button aria-label="Open menu" className="grid h-10 w-10 place-items-center rounded-full border border-primary-foreground/25 text-primary-foreground md:hidden">
                <Menu className="h-5 w-5" />
              </button>
            </SheetTrigger>
            <SheetContent side="right" className="w-72 border-none bg-primary text-primary-foreground">
              <SheetTitle className="mb-6 font-display text-2xl tracking-wide text-primary-foreground">Menu</SheetTitle>
              <nav className="flex flex-col gap-1" onClick={() => setOpen(false)}>
                <NavLink to="/" end className={mobileCls}>Home</NavLink>
                <NavLink to="/trainers" className={mobileCls}>Trainers</NavLink>
                <NavLink to="/venues" className={mobileCls}>Venues</NavLink>
                {user && <NavLink to="/my-calendar" className={mobileCls}>My Calendar</NavLink>}
                {user && <NavLink to="/profile" className={mobileCls}>Profile</NavLink>}
                {user?.role === "trainer" && <NavLink to="/coach" className={mobileCls}>Coach Hub</NavLink>}
                {user?.role === "admin" && <NavLink to="/admin" className={mobileCls}>Admin</NavLink>}
                {!user && <NavLink to="/login" className={mobileCls}>Sign In</NavLink>}
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
