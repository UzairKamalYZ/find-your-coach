import { Link, NavLink, useNavigate } from "react-router-dom";
import { Dumbbell, User, LogIn } from "lucide-react";
import { useAuth } from "@/context/AuthContext";

const Navbar = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const linkCls = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-semibold uppercase tracking-wider transition-colors hover:text-accent ${
      isActive ? "text-accent" : "text-primary-foreground/80"
    }`;

  const initials = user?.name.split(" ").map((s) => s[0]).slice(0, 2).join("").toUpperCase();

  return (
    <header className="absolute top-0 left-0 right-0 z-50">
      <div className="container flex items-center justify-between py-5">
        <Link to="/" className="flex items-center gap-2 text-primary-foreground">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-gradient-accent shadow-glow">
            <Dumbbell className="h-5 w-5 text-primary" />
          </span>
          <span className="font-display text-2xl tracking-wide">
            Find<span className="text-accent">My</span>Trainer
          </span>
        </Link>
        <nav className="hidden items-center gap-8 md:flex">
          <NavLink to="/" end className={linkCls}>Home</NavLink>
          <NavLink to="/trainers" className={linkCls}>Trainers</NavLink>
          <NavLink to="/venues" className={linkCls}>Venues</NavLink>
          {user && <NavLink to="/my-calendar" className={linkCls}>My Calendar</NavLink>}
        </nav>

        {user ? (
          <button
            onClick={() => navigate("/profile")}
            className="flex items-center gap-2 rounded-full border-2 border-accent/40 bg-primary/40 px-3 py-1.5 backdrop-blur transition-colors hover:border-accent"
          >
            <span className="grid h-8 w-8 place-items-center rounded-full bg-gradient-accent text-xs font-bold text-primary">
              {initials}
            </span>
            <span className="hidden pr-2 text-sm font-semibold text-primary-foreground sm:block">{user.name.split(" ")[0]}</span>
          </button>
        ) : (
          <Link
            to="/login"
            className="flex items-center gap-2 rounded-full bg-gradient-accent px-5 py-2.5 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105"
          >
            <LogIn className="h-4 w-4" /> Sign In
          </Link>
        )}
      </div>
    </header>
  );
};

export default Navbar;
