import { Component, ReactNode } from "react";

/** Last-resort guard so an unexpected render error shows a message instead of a blank page. */
export default class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error) {
    console.error("Unhandled UI error", error);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div className="grid min-h-screen place-items-center bg-background p-6 text-center">
        <div className="max-w-md">
          <h1 className="font-display text-4xl text-primary">Something went wrong</h1>
          <p className="mt-3 text-muted-foreground">Please reload the page. If this keeps happening, try again in a few minutes.</p>
          <button onClick={() => window.location.assign("/")} className="mt-6 rounded-full bg-gradient-accent px-6 py-2.5 text-sm font-bold uppercase tracking-wider text-primary">
            Back to home
          </button>
        </div>
      </div>
    );
  }
}
