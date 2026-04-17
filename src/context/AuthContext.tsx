import { createContext, useContext, useEffect, useState, ReactNode } from "react";

export type User = {
  email: string;
  name: string;
  city: string;
  avatar?: string;
};

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
  signup: (email: string, password: string, name: string, city: string) => Promise<void>;
  logout: () => void;
  updateProfile: (patch: Partial<User>) => void;
  addBooking: (b: Omit<Booking, "id" | "createdAt">) => void;
  cancelBooking: (id: string) => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

const USER_KEY = "fmt_user";
const BOOKINGS_KEY = "fmt_bookings";

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);

  useEffect(() => {
    const u = localStorage.getItem(USER_KEY);
    if (u) setUser(JSON.parse(u));
    const b = localStorage.getItem(BOOKINGS_KEY);
    if (b) setBookings(JSON.parse(b));
  }, []);

  const persistUser = (u: User | null) => {
    setUser(u);
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u));
    else localStorage.removeItem(USER_KEY);
  };

  const persistBookings = (b: Booking[]) => {
    setBookings(b);
    localStorage.setItem(BOOKINGS_KEY, JSON.stringify(b));
  };

  const login = async (email: string, _password: string) => {
    // Mock: accept any credentials, derive name from email
    const name = email.split("@")[0].replace(/[._]/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
    persistUser({ email, name, city: "London" });
  };

  const signup = async (email: string, _password: string, name: string, city: string) => {
    persistUser({ email, name, city });
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
    <AuthContext.Provider value={{ user, bookings, login, signup, logout, updateProfile, addBooking, cancelBooking }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
};
