# FindMyTrainer backend

Spring Boot 3.3 / Java 21 REST API backed by **H2** (file database by default). It powers every screen of the
React frontend: sign-up/sign-in, coach search and booking, venues, My Calendar, profiles, the Coach Hub and the
Admin console.

- Code: `backend/`
- Interactive API docs (Swagger UI): `http://localhost:8081/swagger-ui.html`
- H2 console: `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:file:./data/findmycoach`, user `sa`, no password)

## Running

```bash
# terminal 1 – API on :8081 (seeds demo data on first start into backend/data/)
cd backend && mvn spring-boot:run          # or: npm run dev:backend

# terminal 2 – frontend on :8080, proxies /api to :8081
npm run dev
```

Tests: `cd backend && mvn test` (51 tests, in-memory H2) and `npm test` for the frontend.

Reset the database: stop the server and delete `backend/data/`.

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@findmytrainer.dev` | `Admin@12345` |
| Trainee (has past, upcoming and venue bookings) | `sam@findmytrainer.dev` | `Trainee@123` |
| Coach (Marcus Reed listing) | `marcus@findmytrainer.dev` | `Trainer@123` |
| Other coaches | `sofia@`, `arjun@`, `leah@findmytrainer.dev` | `Trainer@123` |
| Coach whose listing awaits approval | `hannah@findmytrainer.dev` | `Trainer@123` |

The social buttons (Google/Apple/Facebook/Microsoft) are **demo sign-ins**: no provider is contacted; each maps to
`alex.<provider>@example.com`. Such accounts can set a password later from the profile page.

## Requirements by role

### Trainee (customer)
- Register / sign in with email + password; demo social sign-in.
- Browse, search (name, city, sport, specialty), filter and sort coaches; see real availability and reviews.
- Book a 60-minute session; add notes for the coach. Cannot double-book themselves at the same time.
- My Calendar: upcoming and past coaching sessions plus venue reservations; cancel, reschedule (max 3 times),
  review attended sessions.
- Reserve venues by the hour (1–4 hours), within opening hours and subject to court/pitch capacity.
- Save favourite coaches, receive in-app notifications, edit profile, change/set password, close account.

### Coach (trainer)
- Create a listing (sport, city, rate, bio, specialties, qualifications, photo); new listings are **PENDING** until an
  admin approves them. Rejected listings return to PENDING when edited.
- Weekly schedule (which of the fixed hourly slots they work each weekday) and time off (blocks whole days; can
  cancel already-booked sessions on those days, notifying trainees).
- See bookings with trainee contact details and notes; mark sessions **COMPLETED** or **NO_SHOW** once started;
  cancel upcoming sessions with a reason.
- Reply publicly to reviews. Dashboard with upcoming sessions, earnings (month / total / booked), rating, no-shows.

### Admin
- Dashboard: users by role, coaches by status, bookings by status, revenue (delivered / booked / venues),
  next-14-days load, top coaches, bookings by sport.
- Users: search, change role, enable/disable (takes effect immediately – tokens of disabled users stop working).
  Guards: cannot disable/demote yourself; the last admin cannot be removed.
- Coaches: approve / reject / suspend (with reason sent to the coach), feature on the home page, edit, create a
  listing for an existing coach account.
- Bookings: search all coaching and venue bookings, cancel or mark outcomes.
- Venues: create, edit, deactivate/reactivate. Sports catalogue: add, hide/offer.
- Reviews: moderate (hide/unhide – hidden reviews leave the coach's rating), delete.
- Audit log of every admin action.

### Project manager / engineering notes
- North-star metric (“training sessions booked with a trainer each week”) is visible on the admin dashboard
  (`bookingsLast7Days`, next-14-days chart) and the public `/stats` endpoint.
- All state lives in the API; the frontend only stores the token pair in `localStorage`.

## Business rules

| Rule | Value / behaviour |
|---|---|
| Session slots | `08:00 09:00 10:00 12:00 14:00 15:00 17:00 18:00 19:00`, 60 min |
| Booking window | today .. today + 59 days (`APP_BOOKING_WINDOW_DAYS`) |
| Double booking | Coach row is locked (`SELECT … FOR UPDATE`) while booking; concurrent requests for the same slot get exactly one `201` and the rest `409` (tested) |
| Trainee clash | A trainee cannot hold two confirmed sessions at the same date+slot (`409`) |
| Own listing | A coach cannot book themselves (`422`) |
| Cancellation | Trainee or coach until the session starts; admins any time. Trainee cancellations < 24h before start are flagged `lateCancellation` |
| Reschedule | Trainee only, before start, at most 3 times |
| Outcomes | Coach/admin can mark COMPLETED/NO_SHOW only after start. A nightly job (02:15) auto-completes past CONFIRMED sessions |
| Reviews | One per booking, only after the session started and not for cancelled/no-show sessions. Rating = (imported baseline + platform reviews) average |
| Price | Snapshot of the coach's hourly rate at booking time (GBP) |
| Venue capacity | Bookings per hour ≤ venue capacity (number of courts/pitches) |
| Time zone | `Europe/London` (`APP_TIME_ZONE`) for “today”, slot start times and cancellation rules |

## Security

- Passwords hashed with BCrypt. Login errors don't reveal whether an email exists.
- Access token: HS256 JWT, 1 hour (`APP_JWT_ACCESS_TOKEN_TTL`). Refresh token: random 256-bit opaque value,
  stored as SHA-256, 30 days, **rotated on every use**; re-use of a rotated token revokes all of that user's
  refresh tokens. Logout revokes the presented refresh token; password change revokes all.
- The user is re-loaded on each request, so role changes and disabling apply immediately.
- Role areas: `/api/v1/admin/**` → ADMIN, `/api/v1/coach/**` → TRAINER; ownership is checked in services
  (other users' resources answer `404`, not `403`, to avoid leaking existence).
- CORS origins: `APP_CORS_ALLOWED_ORIGINS`. **Set `APP_JWT_SECRET` (≥ 32 chars) and disable the H2 console
  (`H2_CONSOLE_ENABLED=false`) anywhere outside local development.**

## API overview (`/api/v1`)

Errors always look like `{"status":409,"code":"CONFLICT","error":"Slot already booked","details":{…},"path":"…","timestamp":"…"}`.
Paged endpoints return `{content, page, size, totalElements, totalPages}`.

| Area | Endpoints |
|---|---|
| Auth (public) | `POST /auth/register`, `POST /auth/login`, `POST /auth/social`, `POST /auth/refresh`, `POST /auth/logout` |
| Me | `GET/PATCH/DELETE /me`, `POST /me/password`, `GET /me/favorites`, `GET /me/favorites/ids`, `PUT/DELETE /me/favorites/{trainerId}` |
| Catalogue (public) | `GET /sports`, `GET /stats`, `GET /trainers?sport&city&q&minRate&maxRate&minRating&featured&sort&page&size`, `GET /trainers/cities`, `GET /trainers/{id}`, `GET /trainers/{id}/availability?from&to`, `GET /trainers/{id}/reviews`, `GET /trainers/{id}/reviews/summary`, `GET /venues?sport&city&type&q&maxPrice&sort`, `GET /venues/{id}`, `GET /venues/{id}/availability?from&to` |
| Coach bookings | `POST /bookings`, `GET /bookings?scope=active|upcoming|past|cancelled|all`, `GET /bookings/{id}`, `POST /bookings/{id}/cancel`, `DELETE /bookings/{id}`, `POST /bookings/{id}/reschedule` |
| Venue bookings | `POST /venue-bookings`, `GET /venue-bookings?includeCancelled`, `POST /venue-bookings/{id}/cancel`, `DELETE /venue-bookings/{id}` |
| Reviews | `POST /reviews`, `PATCH /reviews/{id}`, `DELETE /reviews/{id}` |
| Notifications | `GET /notifications?unreadOnly`, `GET /notifications/unread-count`, `POST /notifications/{id}/read`, `POST /notifications/read-all`, `DELETE /notifications/{id}` |
| Coach hub (TRAINER) | `GET/PUT /coach/listing`, `GET/PUT /coach/schedule`, `GET/POST /coach/time-off`, `DELETE /coach/time-off/{id}`, `GET /coach/bookings`, `PATCH /coach/bookings/{id}`, `GET /coach/reviews`, `POST /coach/reviews/{id}/reply`, `GET /coach/stats` |
| Admin (ADMIN) | `GET /admin/dashboard`, `GET /admin/users`, `GET/PATCH /admin/users/{id}`, `GET/POST /admin/trainers`, `PUT /admin/trainers/{id}`, `PATCH /admin/trainers/{id}/status`, `PATCH /admin/trainers/{id}/featured`, `GET /admin/bookings`, `PATCH /admin/bookings/{id}`, `GET/POST /admin/venues`, `PUT/DELETE /admin/venues/{id}`, `GET /admin/venue-bookings`, `POST /admin/venue-bookings/{id}/cancel`, `GET /admin/reviews`, `PATCH/DELETE /admin/reviews/{id}`, `GET/PUT /admin/sports`, `GET /admin/audit-logs` |

Full request/response schemas are in Swagger UI.

## Code layout

```
backend/src/main/java/com/findmycoach/
  auth/          register, login, demo social, refresh-token rotation
  user/          User entity, /me endpoints
  trainer/       coach listings, weekly slots, time off, availability, public search
  coach/         Coach Hub (self-service for TRAINER role)
  booking/       coach session bookings + nightly auto-complete
  venue/         venues, capacity-aware venue bookings
  review/        reviews, replies, moderation
  favorite/      saved coaches
  notification/  in-app notifications
  admin/         admin console API
  audit/         audit log
  sport/         sports catalogue
  stats/         public landing-page numbers
  seed/          demo data (runs on an empty DB when APP_SEED_DEMO_DATA=true)
  config/, security/, common/
```

Schema is created by Hibernate (`ddl-auto=update`), which is fine for H2 during this phase. Before moving to a
production database, introduce Flyway/Liquibase migrations.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8081` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/findmycoach;MODE=LEGACY;AUTO_SERVER=TRUE` | Use `jdbc:h2:mem:x;DB_CLOSE_DELAY=-1` for a throwaway DB |
| `APP_JWT_SECRET` | dev value | HS256 key, ≥ 32 chars |
| `APP_JWT_ACCESS_TOKEN_TTL` / `APP_JWT_REFRESH_TOKEN_TTL` | `PT1H` / `P30D` | ISO-8601 durations |
| `APP_CORS_ALLOWED_ORIGINS` | localhost:8080/5173 | Comma separated |
| `APP_TIME_ZONE` | `Europe/London` | Business time zone |
| `APP_BOOKING_WINDOW_DAYS` | `60` | How far ahead bookings are allowed |
| `APP_SEED_DEMO_DATA` | `true` | Seed demo accounts/data on an empty DB |
| `H2_CONSOLE_ENABLED` | `true` | Disable outside local dev |
| `VITE_API_URL` (frontend) | `/api/v1` | Set for a static deploy that talks to a remote API |

## Not in scope yet (suggested next steps)

- Real payments (bookings record the price; nothing is charged) and payouts to coaches.
- Email/SMS delivery of notifications and password reset by email (no mail provider configured).
- Real OAuth providers instead of demo social sign-in.
- Image uploads (images are URLs or bundled asset keys such as `trainer-1`).
- Linking a coach session to a venue booking, group sessions, and per-coach session lengths other than 60 minutes.
- Rate limiting on auth endpoints and database migrations for a production database.
