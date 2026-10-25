export type AccountRole = "trainer" | "trainee";
export type SocialProvider = "Google" | "Apple" | "Facebook" | "Microsoft";

export type User = {
  email: string;
  name: string;
  city: string;
  avatar?: string;
  role: AccountRole;
  phone?: string;
  provider?: SocialProvider | "Email";
  bio?: string;
  sports?: string;
  goals?: string;
  level?: string;
  experience?: string;
  qualifications?: string;
};

export const normalizeUser = (user: Partial<User> & Pick<User, "email" | "name" | "city">): User => ({
  ...user,
  role: user.role === "trainer" ? "trainer" : "trainee",
});

export const createSocialDemoUser = (provider: SocialProvider, role: AccountRole): User => ({
  email: `alex.${provider.toLowerCase()}@example.com`,
  name: "Alex Morgan (demo)",
  city: "",
  phone: "",
  role,
  provider,
});