import { Link } from "react-router-dom";
import { Star, MapPin } from "lucide-react";
import type { Trainer } from "@/data/mock";

const TrainerCard = ({ trainer, index = 0 }: { trainer: Trainer; index?: number }) => {
  return (
    <Link
      to={`/trainers/${trainer.id}`}
      className="group relative block animate-float-up overflow-hidden rounded-2xl bg-card shadow-card transition-all duration-500 hover:-translate-y-1 hover:shadow-elevate"
      style={{ animationDelay: `${index * 80}ms` }}
    >
      <div className="relative aspect-[4/5] overflow-hidden">
        <img
          src={trainer.image}
          alt={`${trainer.name}, ${trainer.sport} coach`}
          loading="lazy"
          width={640}
          height={800}
          className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-primary via-primary/30 to-transparent" />
        <span className="absolute left-4 top-4 rounded-full bg-gradient-accent px-3 py-1 text-xs font-bold uppercase tracking-wider text-primary">
          {trainer.sport}
        </span>
        <div className="absolute bottom-0 left-0 right-0 p-5 text-primary-foreground">
          <h3 className="font-display text-3xl leading-none">{trainer.name}</h3>
          <div className="mt-2 flex items-center justify-between text-sm">
            <span className="flex items-center gap-1.5 text-primary-foreground/80">
              <MapPin className="h-4 w-4" /> {trainer.city}
            </span>
            <span className="flex items-center gap-1 font-semibold">
              <Star className="h-4 w-4 fill-accent text-accent" />
              {trainer.rating} <span className="text-primary-foreground/60">({trainer.reviews})</span>
            </span>
          </div>
        </div>
      </div>
      <div className="flex items-center justify-between bg-card px-5 py-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-muted-foreground">From</p>
          <p className="font-display text-2xl text-primary">£{trainer.hourlyRate}<span className="text-base text-muted-foreground">/hr</span></p>
        </div>
        <span className="rounded-full bg-primary px-4 py-2 text-xs font-bold uppercase tracking-wider text-primary-foreground transition-colors group-hover:bg-accent group-hover:text-accent-foreground">
          Book →
        </span>
      </div>
    </Link>
  );
};

export default TrainerCard;
