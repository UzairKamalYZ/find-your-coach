import t1 from "@/assets/trainer-1.jpg";
import t2 from "@/assets/trainer-2.jpg";
import t3 from "@/assets/trainer-3.jpg";
import t4 from "@/assets/trainer-4.jpg";
import vTennis from "@/assets/venue-tennis.jpg";
import vFootball from "@/assets/venue-football.jpg";
import vBasket from "@/assets/venue-basketball.jpg";
import vCricket from "@/assets/venue-cricket.jpg";

/** The API stores bundled images by key (e.g. "trainer-1"); anything else is treated as a URL. */
const ASSETS: Record<string, string> = {
  "trainer-1": t1,
  "trainer-2": t2,
  "trainer-3": t3,
  "trainer-4": t4,
  "venue-tennis": vTennis,
  "venue-football": vFootball,
  "venue-basketball": vBasket,
  "venue-cricket": vCricket,
};

export const IMAGE_KEYS = Object.keys(ASSETS);

export const resolveImage = (key?: string | null) => {
  if (!key) return "/placeholder.svg";
  return ASSETS[key] ?? key;
};
