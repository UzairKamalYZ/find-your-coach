import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";

beforeEach(() => localStorage.clear());

describe("demonstration accounts", () => {
  it("keeps Trainer selection and contact details on signup", async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await act(async () => result.current.signup("trainer@example.com", "not-stored", "Jordan Lee", "Brussels", "trainer", "+32 123"));
    expect(result.current.user).toMatchObject({ role: "trainer", name: "Jordan Lee", email: "trainer@example.com", city: "Brussels", phone: "+32 123" });
    expect(localStorage.getItem("fmt_demo_accounts")).not.toContain("not-stored");
  });

  it("keeps Trainee selection on signup", async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await act(async () => result.current.signup("trainee@example.com", "demo", "Sam", "Paris", "trainee", ""));
    expect(result.current.user?.role).toBe("trainee");
  });

  for (const provider of ["Google", "Apple", "Facebook", "Microsoft"] as const) {
    it(`prefills ${provider} demo identity and the selected role`, async () => {
      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
      await act(async () => result.current.socialLogin(provider, "trainer"));
      expect(result.current.user).toMatchObject({ name: "Alex Morgan (demo)", email: `alex.${provider.toLowerCase()}@example.com`, provider, role: "trainer", city: "", phone: "" });
    });
  }

  it("prefills saved profile details after returning email sign-in", async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await act(async () => result.current.signup("sam@example.com", "demo", "Sam", "Brussels", "trainee", "123"));
    act(() => result.current.updateProfile({ sports: "Tennis", goals: "Improve serve", phone: "456" }));
    act(() => result.current.logout());
    await act(async () => result.current.login("sam@example.com", "demo"));
    expect(result.current.user).toMatchObject({ sports: "Tennis", goals: "Improve serve", phone: "456", city: "Brussels" });
  });

  it("restores legacy demo profiles as trainees", async () => {
    localStorage.setItem("fmt_user", JSON.stringify({ name: "Alex", email: "alex@example.com", city: "London" }));
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.user?.role).toBe("trainee"));
  });

  it("retains a trainer's role on returning social sign-in", async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await act(async () => result.current.socialLogin("Apple", "trainer"));
    act(() => result.current.logout());
    await act(async () => result.current.socialLogin("Apple"));
    expect(result.current.user?.role).toBe("trainer");
  });
});