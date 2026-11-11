import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, Check, Dumbbell, GraduationCap, Apple, Facebook, Mail } from "lucide-react";
import Navbar from "@/components/Navbar";
import AccountField from "@/components/AccountField";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";
import { AccountRole, SocialProvider } from "@/lib/demo-account";
import { toast } from "sonner";

const providers: SocialProvider[] = ["Google", "Apple", "Facebook", "Microsoft"];
const Login = () => {
  const { user, login, signup, socialLogin } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [role, setRole] = useState<AccountRole | null>(null);
  const [step, setStep] = useState<"role" | "details">("role");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [city, setCity] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  if (user) return <Navigate to="/profile" replace />;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (mode === "signup" && !role) return;
    setLoading(true);
    try {
      if (mode === "login") await login(email, password);
      else if (role) await signup(email, password, name, city, role, phone);
      toast.success(mode === "login" ? "Welcome back!" : "Demo account created!");
      navigate("/profile");
    } catch { toast.error("Unable to continue. Please try again."); }
    finally { setLoading(false); }
  };

  const onSocial = async (provider: SocialProvider) => {
    if (mode === "signup" && !role) return;
    setLoading(true);
    try {
      await socialLogin(provider, role ?? undefined);
      toast.success(`${provider} demonstration signed in — sample name and email used`);
      navigate("/profile");
    } catch { toast.error("Unable to continue. Please try again."); }
    finally { setLoading(false); }
  };

  const roleStep = mode === "signup" && step === "role";
  return (
    <div className="min-h-screen bg-background">
      <Navbar solid />
      <main className="container mx-auto max-w-xl pb-16 pt-28">
        <Link to="/" className="mb-8 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="h-4 w-4" /> FindMyTrainer</Link>
        <div className="mb-7 flex items-center gap-3">
          <span className="grid h-12 w-12 place-items-center rounded-lg bg-accent text-accent-foreground"><Dumbbell className="h-6 w-6" /></span>
          <div>
            <p className="text-xs font-semibold uppercase text-muted-foreground">Your next session starts here</p>
            <h1 className="font-display text-4xl leading-tight text-foreground">{mode === "login" ? "Welcome back" : roleStep ? "Join FindMyTrainer" : `Your ${role} account`}</h1>
          </div>
        </div>
        <div className="mb-7 grid grid-cols-2 border-b border-border" role="tablist" aria-label="Account access">
          {(["login", "signup"] as const).map((value) => <Button key={value} variant="ghost" role="tab" aria-selected={mode === value}
            className={`h-12 rounded-none border-b-2 ${mode === value ? "border-accent text-foreground" : "border-transparent text-muted-foreground"}`}
            onClick={() => { setMode(value); setStep("role"); setRole(null); }}>{value === "login" ? "Sign in" : "Sign up"}</Button>)}
        </div>
        {mode === "signup" && <div className="mb-6 flex items-center gap-3 text-xs font-semibold text-muted-foreground"><span className={roleStep ? "text-foreground" : "text-muted-foreground"}>1 · Account type</span><ArrowRight className="h-3 w-3" /><span className={!roleStep ? "text-foreground" : "text-muted-foreground"}>2 · Your details</span></div>}
        {roleStep ? (
          <section>
            <h2 className="mb-4 font-display text-2xl">How are you joining?</h2>
            <div className="grid grid-cols-2 gap-3" role="radiogroup" aria-label="Account type">
              {(["trainee", "trainer"] as const).map((value) => {
                const Icon = value === "trainer" ? Dumbbell : GraduationCap;
                return <Button key={value} variant="outline" role="radio" aria-checked={role === value} onClick={() => setRole(value)}
                  className={`relative h-36 flex-col items-start whitespace-normal p-5 text-left hover:bg-muted ${role === value ? "border-accent bg-accent/10 ring-1 ring-accent" : "bg-card"}`}>
                  <Icon className="mb-2 !h-7 !w-7 text-accent" />
                  <span className="text-lg font-bold capitalize">{value}</span>
                  <span className="text-xs font-normal text-muted-foreground">{value === "trainer" ? "Coach and lead sessions" : "Find a coach and train"}</span>
                  {role === value && <Check className="absolute right-3 top-3 text-accent" />}
                </Button>;
              })}
            </div>
            <Button disabled={!role} onClick={() => setStep("details")} className="mt-6 h-12 w-full bg-accent text-accent-foreground hover:bg-accent/90">Continue <ArrowRight /></Button>
          </section>
        ) : (
          <section>
            {mode === "signup" && <Button variant="link" className="mb-4 h-auto px-0 text-muted-foreground" onClick={() => setStep("role")}><ArrowLeft /> Change account type</Button>}
            <div className="grid grid-cols-2 gap-3">
              {providers.map((provider) => <Button key={provider} variant="outline" disabled={loading} className="h-11" onClick={() => onSocial(provider)}>
                {provider === "Apple" ? <Apple /> : provider === "Facebook" ? <Facebook /> : <span aria-hidden="true" className="text-base font-bold">{provider === "Google" ? "G" : "⊞"}</span>}{provider} <span className="text-xs text-muted-foreground">demo</span>
              </Button>)}
            </div>
            <div className="my-6 flex items-center gap-3"><div className="h-px flex-1 bg-border" /><span className="flex items-center gap-1 text-xs text-muted-foreground"><Mail className="h-3 w-3" /> Or with email</span><div className="h-px flex-1 bg-border" /></div>
            <form onSubmit={onSubmit} className="space-y-4">
              {mode === "signup" && <AccountField label="Full name" value={name} onChange={setName} autoComplete="name" placeholder="Your full name" required maxLength={100} />}
              <AccountField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" placeholder="you@example.com" required />
              {mode === "signup" && <div className="grid gap-4 sm:grid-cols-2">
                <AccountField label="Phone (optional)" type="tel" value={phone} onChange={setPhone} autoComplete="tel" placeholder="+44" maxLength={40} />
                <AccountField label="City" value={city} onChange={setCity} autoComplete="address-level2" placeholder="Your city" required maxLength={100} />
              </div>}
              <AccountField label="Password" type="password" value={password} onChange={setPassword} autoComplete={mode === "login" ? "current-password" : "new-password"} placeholder="Password" required minLength={mode === "signup" ? 8 : 1} />
              <Button type="submit" disabled={loading} className="h-12 w-full bg-accent text-accent-foreground hover:bg-accent/90">{loading ? "Please wait…" : mode === "login" ? "Sign in" : "Create demo account"}<ArrowRight /></Button>
            </form>
          </section>
        )}
        <p className="mt-6 text-center text-xs text-muted-foreground">Demo accounts only · No real social connection · Passwords are not saved or verified</p>
      </main>
    </div>
  );
};
export default Login;
