import type { AuthResponse } from "./types";

/** Base URL of the backend API. In dev, Vite proxies /api to the Spring Boot server (see vite.config.ts). */
export const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, "") || "/api/v1";

const TOKENS_KEY = "fmt_tokens";

type Tokens = { accessToken: string; refreshToken: string };

export class ApiError extends Error {
  status: number;
  code?: string;
  details?: Record<string, string>;
  constructor(status: number, message: string, code?: string, details?: Record<string, string>) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export const tokenStore = {
  get(): Tokens | null {
    try {
      const raw = localStorage.getItem(TOKENS_KEY);
      return raw ? (JSON.parse(raw) as Tokens) : null;
    } catch {
      return null;
    }
  },
  set(t: Tokens | null) {
    try {
      if (t) localStorage.setItem(TOKENS_KEY, JSON.stringify(t));
      else localStorage.removeItem(TOKENS_KEY);
    } catch {
      /* storage unavailable */
    }
  },
};

let refreshing: Promise<boolean> | null = null;
let onAuthLost: (() => void) | null = null;
export const setOnAuthLost = (fn: (() => void) | null) => { onAuthLost = fn; };

async function refreshTokens(): Promise<boolean> {
  const current = tokenStore.get();
  if (!current?.refreshToken) return false;
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await fetch(`${API_BASE}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: current.refreshToken }),
        });
        if (!res.ok) return false;
        const data = (await res.json()) as AuthResponse;
        tokenStore.set({ accessToken: data.accessToken, refreshToken: data.refreshToken });
        return true;
      } catch {
        return false;
      } finally {
        setTimeout(() => { refreshing = null; }, 0);
      }
    })();
  }
  return refreshing;
}

type Options = { method?: string; body?: unknown; auth?: boolean; query?: Record<string, string | number | boolean | undefined | null> };

export async function api<T = unknown>(path: string, opts: Options = {}): Promise<T> {
  const { method = "GET", body, auth = true, query } = opts;
  const qs = query
    ? Object.entries(query)
        .filter(([, v]) => v !== undefined && v !== null && v !== "")
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join("&")
    : "";
  const url = `${API_BASE}${path}${qs ? `?${qs}` : ""}`;

  const send = async () => {
    const headers: Record<string, string> = { Accept: "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const token = auth ? tokenStore.get()?.accessToken : undefined;
    if (token) headers.Authorization = `Bearer ${token}`;
    return fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  };

  let res: Response;
  try {
    res = await send();
  } catch {
    throw new ApiError(0, "Cannot reach the FindMyTrainer server. Is the backend running?");
  }
  if (res.status === 401 && auth && tokenStore.get()) {
    if (await refreshTokens()) res = await send();
    if (res.status === 401) {
      tokenStore.set(null);
      onAuthLost?.();
    }
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? safeJson(text) : undefined;
  if (!res.ok) {
    const d = (data ?? {}) as { error?: string; code?: string; details?: Record<string, string> };
    const detail = d.details ? Object.entries(d.details).map(([k, v]) => `${k} ${v}`).join(", ") : "";
    throw new ApiError(res.status, detail ? `${d.error ?? "Request failed"}: ${detail}` : d.error ?? `Request failed (${res.status})`, d.code, d.details);
  }
  return data as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export const errorMessage = (e: unknown) => (e instanceof Error ? e.message : "Something went wrong");
