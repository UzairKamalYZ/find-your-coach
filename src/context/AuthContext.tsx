import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { AccountRole, SocialProvider, User, createSocialDemoUser, normalizeUser } from "@/lib/demo-account";
export type { User } from "@/lib/demo-account";

export type Booking = {
  id: string;
  trainerId: string;
  trainerName: string;
  trainerImage: string;
  sport: string;
  date: string; // ISO
  slot: string; // "HH:mm"
  price: number;
  createdAt: string;
};

type AuthContextType = {
  user: User | null;
  bookings: Booking[];
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, name: string, city: string, role: AccountRole, phone: string) => Promise<void>;
  socialLogin: (provider: SocialProvider, role?: AccountRole) => Promise<void>;
  logout: () => void;
  updateProfile: (patch: Partial<User>) => void;
  addBooking: (b: Omit<Booking, "id" | "createdAt">) => void;
  cancelBooking: (id: string) => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

const USER_KEY = "fmt_user";
const BOOKINGS_KEY = "fmt_bookings";
const ACCOUNTS_KEY = "fmt_demo_accounts";

const readAccounts = (): Record<string, User> => {
  try { return JSON.parse(localStorage.getItem(ACCOUNTS_KEY) || "{}"); }
  catch { return {}; }
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);

  useEffect(() => {
    const u = localStorage.getItem(USER_KEY);
    if (u) {
      try { setUser(normalizeUser(JSON.parse(u))); } catch { localStorage.removeItem(USER_KEY); }
    }
    const b = localStorage.getItem(BOOKINGS_KEY);
    if (b) {
      try { setBookings(JSON.parse(b)); } catch { localStorage.removeItem(BOOKINGS_KEY); }
    }
  }, []);

  const persistUser = (u: User | null) => {
    setUser(u);
    if (u) {
      localStorage.setItem(USER_KEY, JSON.stringify(u));
      localStorage.setItem(ACCOUNTS_KEY, JSON.stringify({ ...readAccounts(), [u.email.toLowerCase()]: u }));
    }
    else localStorage.removeItem(USER_KEY);
  };

  const persistBookings = (b: Booking[]) => {
    setBookings(b);
    localStorage.setItem(BOOKINGS_KEY, JSON.stringify(b));
  };

  const login = async (email: string, _password: string) => {
    // Demonstration only: no password is stored or validated.
    const saved = readAccounts()[email.trim().toLowerCase()];
    if (saved) { persistUser(normalizeUser(saved)); return; }
    const name = email.split("@")[0].replace(/[._]/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
    persistUser({ email: email.trim(), name, city: "", role: "trainee", provider: "Email" });
  };

  const signup = async (email: string, _password: string, name: string, city: string, role: AccountRole, phone: string) => {
    persistUser({ email: email.trim(), name: name.trim(), city: city.trim(), role, phone: phone.trim(), provider: "Email" });
  };

  const socialLogin = async (provider: SocialProvider, role?: AccountRole) => {
    const demo = createSocialDemoUser(provider, role ?? "trainee");
    const saved = readAccounts()[demo.email];
    persistUser(saved ? { ...normalizeUser(saved), role: role ?? normalizeUser(saved).role, provider } : demo);
  };

  const logout = () => persistUser(null);

  const updateProfile = (patch: Partial<User>) => {
    if (!user) return;
    persistUser({ ...user, ...patch });
  };

  const addBooking = (b: Omit<Booking, "id" | "createdAt">) => {
    const next: Booking = { ...b, id: crypto.randomUUID(), createdAt: new Date().toISOString() };
    persistBookings([...bookings, next]);
  };

  const cancelBooking = (id: string) => {
    persistBookings(bookings.filter((b) => b.id !== id));
  };

  return (
    <AuthContext.Provider value={{ user, bookings, login, signup, socialLogin, logout, updateProfile, addBooking, cancelBooking }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
};
