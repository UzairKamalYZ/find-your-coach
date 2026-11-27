import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";

type Call = { url: string; method: string; body?: Record<string, unknown>; auth?: string | null };

const baseUser = { id: "u1", email: "trainer@example.com", name: "Jordan Lee", city: "Brussels", phone: "+32 123", role: "trainer", provider: "Email", enabled: true, hasPassword: true, createdAt: "2026-01-01T00:00:00Z" };

let calls: Call[] = [];
let me = { ...baseUser };

const json = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

beforeEach(() => {
  localStorage.clear();
  calls = [];
  me = { ...baseUser };
  vi.stubGlobal("fetch", vi.fn(async (input: string, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    const auth = (init?.headers as Record<string, string> | undefined)?.Authorization ?? null;
    calls.push({ url, method, body, auth });
    const tokens = { accessToken: "access-1", refreshToken: "refresh-1", tokenType: "Bearer", expiresIn: 3600 };
    if (url.endsWith("/auth/register")) {
      me = { ...me, email: body.email, name: body.name, city: body.city, role: body.role, phone: body.phone };
      return json(201, { ...tokens, user: me });
    }
    if (url.endsWith("/auth/login")) {
      if (body.password !== "right-password") return json(401, { error: "Incorrect email or password" });
      return json(200, { ...tokens, user: me });
    }
    if (url.endsWith("/auth/social")) return json(200, { ...tokens, user: { ...me, email: `alex.${body.provider.toLowerCase()}@example.com`, name: "Alex Morgan (demo)", provider: body.provider, role: body.role ?? "trainee" } });
    if (url.endsWith("/auth/logout")) return json(204);
    if (url.endsWith("/me") && method === "PATCH") { me = { ...me, ...body }; return json(200, me); }
    if (url.endsWith("/me")) return auth === "Bearer access-1" ? json(200, me) : json(401, { error: "Authentication required" });
    if (url.endsWith("/bookings") && method === "GET") return json(200, []);
    if (url.endsWith("/venue-bookings")) return json(200, []);
    if (url.endsWith("/bookings") && method === "POST") return json(201, { id: "b1", ...body, startsAt: `${body.date}T${body.slot}:00`, status: "CONFIRMED" });
    return json(404, { error: "not mocked" });
  }));
});

afterEach(() => vi.unstubAllGlobals());

const render = async () => {
  const hook = renderHook(() => useAuth(), { wrapper: AuthProvider });
  await waitFor(() => expect(hook.result.current.ready).toBe(true));
  return hook;
};

describe("API-backed accounts", () => {
  it("signs up with role and contact details and never stores the password", async () => {
    const { result } = await render();
    await act(async () => result.current.signup("trainer@example.com", "secret-pass", "Jordan Lee", "Brussels", "trainer", "+32 123"));
    expect(result.current.user).toMatchObject({ role: "trainer", name: "Jordan Lee", email: "trainer@example.com", city: "Brussels", phone: "+32 123" });
    expect(calls.find((c) => c.url.endsWith("/auth/register"))?.body).toMatchObject({ role: "trainer", password: "secret-pass" });
    expect(JSON.stringify(localStorage)).not.toContain("secret-pass");
  });

  it("surfaces login errors from the server", async () => {
    const { result } = await render();
    await expect(act(async () => result.current.login("trainer@example.com", "wrong"))).rejects.toThrow("Incorrect email or password");
    expect(result.current.user).toBeNull();
  });

  it("restores the session from stored tokens on reload", async () => {
    const first = await render();
    await act(async () => first.result.current.login("trainer@example.com", "right-password"));
    first.unmount();
    const second = await render();
    expect(second.result.current.user?.email).toBe("trainer@example.com");
  });

  for (const provider of ["Google", "Apple", "Facebook", "Microsoft"] as const) {
    it(`uses the ${provider} demo identity and the selected role`, async () => {
      const { result } = await render();
      await act(async () => result.current.socialLogin(provider, "trainer"));
      expect(result.current.user).toMatchObject({ name: "Alex Morgan (demo)", email: `alex.${provider.toLowerCase()}@example.com`, provider, role: "trainer" });
    });
  }

  it("updates the profile through the API and signs out", async () => {
    const { result } = await render();
    await act(async () => result.current.login("trainer@example.com", "right-password"));
    await act(async () => result.current.updateProfile({ sports: "Tennis", goals: "Improve serve" }));
    expect(result.current.user).toMatchObject({ sports: "Tennis", goals: "Improve serve" });
    await act(async () => result.current.logout());
    expect(result.current.user).toBeNull();
    expect(calls.some((c) => c.url.endsWith("/auth/logout") && c.body?.refreshToken === "refresh-1")).toBe(true);
    expect(localStorage.getItem("fmt_tokens")).toBeNull();
  });

  it("adds bookings returned by the server", async () => {
    const { result } = await render();
    await act(async () => result.current.login("trainer@example.com", "right-password"));
    await act(async () => { await result.current.addBooking({ trainerId: "marcus-reed", date: "2030-01-02", slot: "09:00" }); });
    expect(result.current.bookings).toHaveLength(1);
    expect(calls.find((c) => c.method === "POST" && c.url.endsWith("/bookings"))?.auth).toBe("Bearer access-1");
  });
});
