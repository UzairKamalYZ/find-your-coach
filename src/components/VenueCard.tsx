import { Star, MapPin } from "lucide-react";
import type { Venue } from "@/data/mock";

const VenueCard = ({ venue, index = 0 }: { venue: Venue; index?: number }) => (
  <article
    className="group relative animate-float-up overflow-hidden rounded-2xl shadow-card transition-all duration-500 hover:-translate-y-1 hover:shadow-elevate"
    style={{ animationDelay: `${index * 80}ms` }}
  >
    <div className="relative aspect-[4/3] overflow-hidden">
      <img
        src={venue.image}
        alt={venue.name}
        loading="lazy"
        width={800}
        height={600}
        className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-primary/90 via-primary/20 to-transparent" />
      <span className="absolute left-4 top-4 rounded-full bg-background/95 px-3 py-1 text-xs font-bold uppercase tracking-wider text-primary backdrop-blur">
        {venue.type} · {venue.sport}
      </span>
    </div>
    <div className="absolute bottom-0 left-0 right-0 p-5 text-primary-foreground">
      <h3 className="font-display text-2xl">{venue.name}</h3>
      <div className="mt-2 flex items-center justify-between text-sm">
        <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{venue.city}</span>
        <span className="flex items-center gap-1"><Star className="h-4 w-4 fill-accent text-accent" />{venue.rating}</span>
      </div>
      <div className="mt-3 flex items-end justify-between">
        <p className="font-display text-xl">£{venue.pricePerHour}<span className="text-sm font-sans text-primary-foreground/70">/hr</span></p>
        <span className="rounded-full bg-gradient-accent px-3 py-1.5 text-xs font-bold uppercase tracking-wider text-primary">Reserve</span>
      </div>
    </div>
  </article>
);

export default VenueCard;
