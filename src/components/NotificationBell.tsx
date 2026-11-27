import { Link } from "react-router-dom";
import { Bell } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { api } from "@/lib/api";
import type { Notification, Page } from "@/lib/types";

export default function NotificationBell() {
  const qc = useQueryClient();
  const { data: count } = useQuery({
    queryKey: ["notifications", "count"],
    refetchInterval: 60_000,
    queryFn: () => api<{ count: number }>("/notifications/unread-count"),
  });
  const { data, refetch } = useQuery({
    queryKey: ["notifications", "list"],
    enabled: false,
    queryFn: () => api<Page<Notification>>("/notifications", { query: { size: 15 } }),
  });
  const refresh = () => qc.invalidateQueries({ queryKey: ["notifications"] });
  const markAll = async () => { await api("/notifications/read-all", { method: "POST" }); refresh(); refetch(); };
  const open = async (n: Notification) => { if (!n.read) { await api(`/notifications/${n.id}/read`, { method: "POST" }); refresh(); } };
  const unread = count?.count ?? 0;

  return (
    <Popover onOpenChange={(o) => o && refetch()}>
      <PopoverTrigger asChild>
        <button aria-label={`Notifications${unread ? ` (${unread} unread)` : ""}`}
          className="relative grid h-10 w-10 place-items-center rounded-full border-2 border-accent/40 bg-primary/40 text-primary-foreground backdrop-blur transition-colors hover:border-accent">
          <Bell className="h-4 w-4" />
          {unread > 0 && <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-accent px-1 text-[10px] font-bold text-accent-foreground">{unread > 9 ? "9+" : unread}</span>}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(22rem,calc(100vw-2rem))] p-0">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <p className="font-semibold">Notifications</p>
          {unread > 0 && <button onClick={markAll} className="text-xs font-semibold text-accent hover:underline">Mark all read</button>}
        </div>
        <ul className="max-h-96 overflow-y-auto">
          {(data?.content ?? []).length === 0 && <li className="px-4 py-8 text-center text-sm text-muted-foreground">You're all caught up.</li>}
          {(data?.content ?? []).map((n) => (
            <li key={n.id} className={`border-b border-border last:border-0 ${n.read ? "" : "bg-accent/5"}`}>
              <Link to={n.link || "#"} onClick={() => open(n)} className="block px-4 py-3 hover:bg-muted">
                <p className="flex items-center gap-2 text-sm font-semibold">{!n.read && <span className="h-2 w-2 shrink-0 rounded-full bg-accent" />}{n.title}</p>
                {n.message && <p className="mt-0.5 text-xs text-muted-foreground">{n.message}</p>}
                <p className="mt-1 text-[11px] text-muted-foreground">{formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}</p>
              </Link>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
