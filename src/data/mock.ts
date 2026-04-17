import t1 from "@/assets/trainer-1.jpg";
import t2 from "@/assets/trainer-2.jpg";
import t3 from "@/assets/trainer-3.jpg";
import t4 from "@/assets/trainer-4.jpg";
import vTennis from "@/assets/venue-tennis.jpg";
import vFootball from "@/assets/venue-football.jpg";
import vBasket from "@/assets/venue-basketball.jpg";
import vCricket from "@/assets/venue-cricket.jpg";

export type Sport = "Tennis" | "Football" | "Cricket" | "Basketball" | "Badminton" | "Swimming";

export const SPORTS: { name: Sport; emoji: string }[] = [
  { name: "Tennis", emoji: "🎾" },
  { name: "Football", emoji: "⚽" },
  { name: "Cricket", emoji: "🏏" },
  { name: "Basketball", emoji: "🏀" },
  { name: "Badminton", emoji: "🏸" },
  { name: "Swimming", emoji: "🏊" },
];

export type Trainer = {
  id: string;
  name: string;
  sport: Sport;
  city: string;
  rating: number;
  reviews: number;
  hourlyRate: number;
  experience: number;
  bio: string;
  image: string;
  specialties: string[];
};

export const TRAINERS: Trainer[] = [
  {
    id: "marcus-reed",
    name: "Marcus Reed",
    sport: "Tennis",
    city: "London",
    rating: 4.9,
    reviews: 184,
    hourlyRate: 65,
    experience: 12,
    bio: "Former ATP qualifier turned coach. I help juniors and adults sharpen technique, footwork and match strategy.",
    image: t1,
    specialties: ["Serve & Volley", "Junior Development", "Match Play"],
  },
  {
    id: "sofia-alvarez",
    name: "Sofia Alvarez",
    sport: "Football",
    city: "Manchester",
    rating: 4.8,
    reviews: 142,
    hourlyRate: 55,
    experience: 9,
    bio: "UEFA B licensed coach. Focused on positional play, finishing and game intelligence for ages 8 to adult.",
    image: t2,
    specialties: ["Finishing", "1v1 Defending", "Position Play"],
  },
  {
    id: "arjun-mehta",
    name: "Arjun Mehta",
    sport: "Cricket",
    city: "Birmingham",
    rating: 4.9,
    reviews: 98,
    hourlyRate: 50,
    experience: 14,
    bio: "Ex-county batsman. Specializing in batting fundamentals, spin play, and high-pressure scenario training.",
    image: t3,
    specialties: ["Batting", "Spin Play", "Fielding Drills"],
  },
  {
    id: "leah-okafor",
    name: "Leah Okafor",
    sport: "Basketball",
    city: "London",
    rating: 4.7,
    reviews: 121,
    hourlyRate: 60,
    experience: 8,
    bio: "Pro shooting coach. Build a deadly jumper, tighten your handle, and develop pro-level court IQ.",
    image: t4,
    specialties: ["Shooting", "Ball Handling", "Conditioning"],
  },
];

export type Venue = {
  id: string;
  name: string;
  type: "Court" | "Pitch" | "Stadium" | "Club";
  sport: Sport;
  city: string;
  pricePerHour: number;
  rating: number;
  image: string;
};

export const VENUES: Venue[] = [
  { id: "v1", name: "Riverside Tennis Club", type: "Court", sport: "Tennis", city: "London", pricePerHour: 30, rating: 4.7, image: vTennis },
  { id: "v2", name: "Northgate Football Arena", type: "Pitch", sport: "Football", city: "Manchester", pricePerHour: 80, rating: 4.8, image: vFootball },
  { id: "v3", name: "Hardwood Hoops Center", type: "Court", sport: "Basketball", city: "London", pricePerHour: 45, rating: 4.6, image: vBasket },
  { id: "v4", name: "Royal Cricket Ground", type: "Stadium", sport: "Cricket", city: "Birmingham", pricePerHour: 120, rating: 4.9, image: vCricket },
];
