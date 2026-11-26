// Shapes returned by the FindMyTrainer API (backend/). Keep in sync with the Java DTOs.

export type Role = "trainee" | "trainer" | "admin";
export type Provider = "Email" | "Google" | "Apple" | "Facebook" | "Microsoft";

export type ApiUser = {
  id: string;
  email: string;
  name: string;
  city: string;
  phone?: string;
  avatar?: string;
  role: Role;
  provider: Provider;
  bio?: string;
  sports?: string;
  goals?: string;
  level?: string;
  experience?: string;
  qualifications?: string;
  enabled: boolean;
  hasPassword: boolean;
  trainerId?: string;
  createdAt: string;
  lastLoginAt?: string;
};

export type AuthResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; user: ApiUser };

export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };

export type TrainerStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "REJECTED";

export type Trainer = {
  id: string;
  name: string;
  sport: string;
  city: string;
  rating: number;
  reviews: number;
  hourlyRate: number;
  experience: number;
  bio?: string;
  image?: string;
  specialties: string[];
  qualifications?: string;
  sessionMinutes: number;
  featured: boolean;
  status: TrainerStatus;
  statusReason?: string;
  createdAt: string;
};

export type SlotStatus = "AVAILABLE" | "BOOKED" | "UNAVAILABLE" | "PAST";
export type DayAvailability = { date: string; dayOff: boolean; dayOffReason?: string; slots: { time: string; status: SlotStatus }[] };

export type BookingStatus = "CONFIRMED" | "CANCELLED" | "COMPLETED" | "NO_SHOW";

export type Booking = {
  id: string;
  trainerId: string;
  trainerName: string;
  trainerImage?: string;
  sport: string;
  date: string; // YYYY-MM-DD
  slot: string; // HH:mm
  startsAt: string;
  price: number;
  status: BookingStatus;
  notes?: string;
  traineeId: string;
  traineeName: string;
  traineeEmail?: string;
  traineePhone?: string;
  cancelledBy?: Role;
  cancellationReason?: string;
  lateCancellation: boolean;
  rescheduleCount: number;
  canCancel: boolean;
  canReschedule: boolean;
  canReview: boolean;
  reviewed: boolean;
  createdAt: string;
};

export type VenueType = "Court" | "Pitch" | "Stadium" | "Club" | "Pool" | "Hall";

export type Venue = {
  id: string;
  name: string;
  type: VenueType;
  sport: string;
  city: string;
  address?: string;
  description?: string;
  pricePerHour: number;
  rating: number;
  image?: string;
  amenities: string[];
  capacity: number;
  openTime: string;
  closeTime: string;
  active: boolean;
};

export type VenueDayAvailability = { date: string; hours: { time: string; remaining: number; available: boolean }[] };

export type VenueBooking = {
  id: string;
  venueId: string;
  venueName: string;
  venueImage?: string;
  sport: string;
  city: string;
  date: string;
  startTime: string;
  endTime: string;
  hours: number;
  totalPrice: number;
  status: "CONFIRMED" | "CANCELLED" | "COMPLETED";
  notes?: string;
  userName?: string;
  userEmail?: string;
  cancellationReason?: string;
  canCancel: boolean;
  createdAt: string;
};

export type Review = {
  id: string;
  trainerId: string;
  trainerName: string;
  bookingId?: string;
  authorId: string;
  authorName: string;
  rating: number;
  comment?: string;
  reply?: string;
  repliedAt?: string;
  hidden: boolean;
  createdAt: string;
};

export type ReviewSummary = { rating: number; reviews: number; breakdown: Record<string, number> };

export type Sport = { name: string; emoji?: string; active: boolean; sortOrder: number };

export type Notification = { id: string; type: string; title: string; message?: string; link?: string; read: boolean; createdAt: string };

export type PublicStats = { coaches: number; venues: number; sessionsBooked: number };

export type WeekDay = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";
export type WeeklySchedule = { days: Record<WeekDay, string[]>; allSlots: string[] };
export type TimeOff = { id: string; date: string; reason?: string };

export type CoachStats = {
  listingStatus: TrainerStatus;
  upcomingSessions: number;
  completedSessions: number;
  noShows: number;
  cancelledSessions: number;
  uniqueTrainees: number;
  totalEarnings: number;
  earningsThisMonth: number;
  pendingEarnings: number;
  rating: number;
  reviews: number;
  favorites: number;
  nextSessions: Booking[];
};

export type DashboardStats = {
  usersByRole: Record<Role, number>;
  totalUsers: number;
  disabledUsers: number;
  trainersByStatus: Record<TrainerStatus, number>;
  activeVenues: number;
  bookingsByStatus: Record<BookingStatus, number>;
  bookingsLast7Days: number;
  venueBookings: number;
  completedRevenue: number;
  upcomingRevenue: number;
  venueRevenue: number;
  topTrainers: { trainerId: string; name: string; sessions: number; revenue: number }[];
  bookingsBySport: Record<string, number>;
  bookingsNext14Days: { date: string; count: number }[];
};

export type AuditLog = { id: string; actorEmail?: string; action: string; targetType?: string; targetId?: string; details?: string; createdAt: string };
