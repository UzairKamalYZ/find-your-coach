import { InputHTMLAttributes } from "react";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "onChange"> & {
  label: string;
  onChange: (value: string) => void;
};

export default function AccountField({ label, onChange, ...props }: Props) {
  return (
    <label className="block min-w-0">
      <span className="mb-1.5 block text-sm font-semibold text-foreground">{label}</span>
      <input {...props} onChange={(event) => onChange(event.target.value)}
        className="h-11 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm text-foreground outline-none transition-colors focus:border-ring focus:ring-1 focus:ring-ring disabled:text-muted-foreground" />
    </label>
  );
}