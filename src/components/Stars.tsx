import { Star } from "lucide-react";

const SIZES = { 3: "h-3 w-3", 4: "h-4 w-4", 6: "h-6 w-6" } as const;

export default function Stars({ value, size = 4, onChange }: { value: number; size?: keyof typeof SIZES; onChange?: (v: number) => void }) {
  const cls = SIZES[size];
  return (
    <span className="inline-flex items-center gap-0.5" role={onChange ? "radiogroup" : "img"} aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((i) => {
        const icon = <Star className={`${cls} ${i <= Math.round(value) ? "fill-accent text-accent" : "text-muted-foreground/40"}`} />;
        return onChange ? (
          <button key={i} type="button" role="radio" aria-checked={i === value} aria-label={`${i} star${i > 1 ? "s" : ""}`} onClick={() => onChange(i)}>{icon}</button>
        ) : <span key={i}>{icon}</span>;
      })}
    </span>
  );
}
