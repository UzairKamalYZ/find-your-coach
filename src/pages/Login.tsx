import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Dumbbell } from "lucide-react";
import Navbar from "@/components/Navbar";
import { useAuth } from "@/context/AuthContext";
import { toast } from "sonner";

const Login = () => {
  const { login, signup } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [city, setCity] = useState("London");
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (mode === "login") {
        await login(email, password);
        toast.success("Welcome back!");
      } else {
        await signup(email, password, name || email.split("@")[0], city);
        toast.success("Account created!");
      }
      navigate("/profile");
    } catch (err) {
      toast.error("Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-hero">
      <Navbar />
      <div className="container flex min-h-screen items-center justify-center pt-20 pb-10">
        <div className="w-full max-w-md rounded-3xl border border-border bg-card p-8 shadow-card">
          <div className="mb-6 flex items-center gap-3">
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-accent shadow-glow">
              <Dumbbell className="h-5 w-5 text-primary" />
            </span>
            <div>
              <h1 className="font-display text-3xl text-primary leading-none">
                {mode === "login" ? "Welcome Back" : "Join the Squad"}
              </h1>
              <p className="text-xs uppercase tracking-wider text-muted-foreground mt-1">
                {mode === "login" ? "Sign in to your account" : "Create your trainee profile"}
              </p>
            </div>
          </div>

          <form onSubmit={onSubmit} className="space-y-4">
            {mode === "signup" && (
              <>
                <Field label="Full Name" value={name} onChange={setName} placeholder="Alex Johnson" />
                <Field label="City" value={city} onChange={setCity} placeholder="London" />
              </>
            )}
            <Field label="Email" type="email" value={email} onChange={setEmail} placeholder="you@email.com" required />
            <Field label="Password" type="password" value={password} onChange={setPassword} placeholder="••••••••" required />

            <button
              type="submit"
              disabled={loading}
              className="mt-2 w-full rounded-full bg-gradient-accent py-3 text-sm font-bold uppercase tracking-wider text-primary shadow-glow transition-transform hover:scale-[1.02] disabled:opacity-50"
            >
              {loading ? "Please wait..." : mode === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>

          <div className="my-5 flex items-center gap-3">
            <div className="h-px flex-1 bg-border" />
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">or</span>
            <div className="h-px flex-1 bg-border" />
          </div>

          <button
            onClick={() => { login("demo@findmytrainer.com", "demo"); toast.success("Signed in as Demo"); navigate("/profile"); }}
            className="w-full rounded-full border-2 border-border bg-background py-3 text-sm font-bold uppercase tracking-wider text-primary transition-colors hover:border-accent"
          >
            Continue with Google (demo)
          </button>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            {mode === "login" ? "New here?" : "Already have an account?"}{" "}
            <button
              type="button"
              onClick={() => setMode(mode === "login" ? "signup" : "login")}
              className="font-bold text-accent hover:underline"
            >
              {mode === "login" ? "Sign up" : "Sign in"}
            </button>
          </p>
          <p className="mt-3 text-center text-xs text-muted-foreground">
            <Link to="/" className="hover:text-accent">← Back to home</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

const Field = ({
  label, value, onChange, type = "text", placeholder, required,
}: {
  label: string; value: string; onChange: (v: string) => void;
  type?: string; placeholder?: string; required?: boolean;
}) => (
  <label className="block">
    <span className="mb-1.5 block text-xs font-bold uppercase tracking-wider text-muted-foreground">{label}</span>
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      required={required}
      className="w-full rounded-xl border-2 border-border bg-background px-4 py-3 text-sm text-primary outline-none transition-colors focus:border-accent"
    />
  </label>
);

export default Login;
