import type { ApiUser, Provider } from "./types";

/** Roles a person can pick when signing up. Admin accounts are created by the platform, not via signup. */
export type AccountRole = "trainer" | "trainee";
export type SocialProvider = Exclude<Provider, "Email">;

/** The signed-in account, as returned by GET /api/v1/me. */
export type User = ApiUser;

/** Demo social sign-ins use a fixed sample identity per provider (the backend mirrors this). */
export const demoSocialEmail = (provider: SocialProvider) => `alex.${provider.toLowerCase()}@example.com`;
