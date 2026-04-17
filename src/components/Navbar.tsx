import { Link, NavLink } from "react-router-dom";
import { Dumbbell } from "lucide-react";

const Navbar = () => {
  const linkCls = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-semibold uppercase tracking-wider transition-colors hover:text-accent ${
      isActive ? "text-accent" : "text-primary-foreground/80"
    }`;

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
        </nav>
        <Link
          to="/trainers"
          className="rounded-full bg-gradient-accent px-5 py-2.5 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-105"
        >
          Find a Coach
        </Link>
      </div>
    </header>
  );
};

export default Navbar;
