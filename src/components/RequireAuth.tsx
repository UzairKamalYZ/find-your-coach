import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import type { Role } from "@/lib/types";

/** Waits for the stored session to load, then redirects anonymous users (or the wrong role) away. */
export default function RequireAuth({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) return <div className="grid min-h-screen place-items-center bg-background text-sm text-muted-foreground">Loading…</div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/profile" replace />;
  return <>{children}</>;
}
