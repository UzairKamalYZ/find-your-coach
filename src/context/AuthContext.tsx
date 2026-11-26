import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from "react";
import { api, setOnAuthLost, tokenStore } from "@/lib/api";
import type { AuthResponse, Booking, VenueBooking } from "@/lib/types";
import { AccountRole, SocialProvider, User } from "@/lib/demo-account";
export type { User } from "@/lib/demo-account";
export type { Booking } from "@/lib/types";

export type ProfilePatch = Partial<Pick<User, "name" | "city" | "phone" | "avatar" | "bio" | "sports" | "goals" | "level" | "experience" | "qualifications">>;

type AuthContextType = {
  user: User | null;
  /** False until the stored session has been checked on first load. */
  ready: boolean;
  bookings: Booking[];
  venueBookings: VenueBooking[];
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, name: string, city: string, role: AccountRole, phone: string) => Promise<void>;
  socialLogin: (provider: SocialProvider, role?: AccountRole) => Promise<void>;
  logout: () => Promise<void>;
  updateProfile: (patch: ProfilePatch) => Promise<void>;
  addBooking: (b: { trainerId: string; date: string; slot: string; notes?: string }) => Promise<Booking>;
  cancelBooking: (id: string, reason?: string) => Promise<void>;
  refreshBookings: () => Promise<void>;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | null>(null);

const LEGACY_KEYS = ["fmt_user", "fmt_bookings", "fmt_demo_accounts"];

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [venueBookings, setVenueBookings] = useState<VenueBooking[]>([]);

  const clear = useCallback(() => {
    tokenStore.set(null);
    setUser(null);
    setBookings([]);
    setVenueBookings([]);
  }, []);

  const refreshBookings = useCallback(async () => {
    if (!tokenStore.get()) return;
    const [b, v] = await Promise.all([api<Booking[]>("/bookings"), api<VenueBooking[]>("/venue-bookings")]);
    setBookings(b ?? []);
    setVenueBookings(v ?? []);
  }, []);

  const refreshUser = useCallback(async () => {
    if (!tokenStore.get()) return;
    setUser(await api<User>("/me"));
  }, []);

  useEffect(() => {
    LEGACY_KEYS.forEach((k) => { try { localStorage.removeItem(k); } catch { /* ignore */ } });
    setOnAuthLost(clear);
    (async () => {
      if (tokenStore.get()) {
        try {
          setUser(await api<User>("/me"));
          await refreshBookings();
        } catch {
          clear();
        }
      }
      setReady(true);
    })();
    return () => setOnAuthLost(null);
  }, [clear, refreshBookings]);

  const accept = async (res: AuthResponse) => {
    tokenStore.set({ accessToken: res.accessToken, refreshToken: res.refreshToken });
    setUser(res.user);
    try { await refreshBookings(); } catch { /* bookings load lazily later */ }
  };

  const login = async (email: string, password: string) =>
    accept(await api<AuthResponse>("/auth/login", { method: "POST", auth: false, body: { email: email.trim(), password } }));

  const signup = async (email: string, password: string, name: string, city: string, role: AccountRole, phone: string) =>
    accept(await api<AuthResponse>("/auth/register", {
      method: "POST", auth: false,
      body: { email: email.trim(), password, name: name.trim(), city: city.trim(), role, phone: phone.trim() || undefined },
    }));

  const socialLogin = async (provider: SocialProvider, role?: AccountRole) =>
    accept(await api<AuthResponse>("/auth/social", { method: "POST", auth: false, body: { provider, role } }));

  const logout = async () => {
    const refreshToken = tokenStore.get()?.refreshToken;
    clear();
    if (refreshToken) {
      try { await api("/auth/logout", { method: "POST", auth: false, body: { refreshToken } }); } catch { /* already signed out locally */ }
    }
  };

  const updateProfile = async (patch: ProfilePatch) => {
    setUser(await api<User>("/me", { method: "PATCH", body: patch }));
  };

  const addBooking = async (b: { trainerId: string; date: string; slot: string; notes?: string }) => {
    const created = await api<Booking>("/bookings", { method: "POST", body: b });
    setBookings((prev) => [...prev, created].sort((x, y) => x.startsAt.localeCompare(y.startsAt)));
    return created;
  };

  const cancelBooking = async (id: string, reason?: string) => {
    await api<Booking>(`/bookings/${id}/cancel`, { method: "POST", body: { reason } });
    setBookings((prev) => prev.filter((b) => b.id !== id));
  };

  return (
    <AuthContext.Provider value={{ user, ready, bookings, venueBookings, login, signup, socialLogin, logout, updateProfile, addBooking, cancelBooking, refreshBookings, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
};
