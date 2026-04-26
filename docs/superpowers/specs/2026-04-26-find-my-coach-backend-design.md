# Find-My-Coach Backend — Design Spec

**Date:** 2026-04-26  
**Status:** Approved  
**Scope:** Spring Boot REST API backend for the find-your-coach frontend SPA

---

## 1. Technology Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| Build | Maven |
| Auth | Google OAuth2 → custom JWT (RS256) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15+ |
| Testing | JUnit 5 + Mockito (unit) + Testcontainers + MockMvc (integration) |
| Coverage | ≥ 80% on service + controller packages (JaCoCo, enforced in `mvn verify`) |

---

## 2. Key Dependencies (`pom.xml`)

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-oauth2-client        <!-- Google OAuth2 Authorization Code flow -->
spring-boot-starter-oauth2-resource-server  <!-- validates our own JWTs -->
spring-boot-starter-validation           <!-- Jakarta Bean Validation -->
spring-boot-starter-actuator             <!-- /actuator/health for free -->
postgresql
lombok
<!-- test -->
spring-boot-starter-test
testcontainers (postgresql module)
```

---

## 3. Package Structure

Base package: `com.findmycoach`

```
FindMyCoachApplication.java
auth/
  AuthController.java
  AuthService.java
  TokenService.java
  RefreshToken.java              (entity)
  RefreshTokenRepository.java
  dto/
    AuthResponse.java
    RefreshRequest.java
    CompleteProfileRequest.java
user/
  User.java                      (entity)
  UserRepository.java
  UserController.java
  UserService.java
  dto/
    UserResponse.java
    UpdateProfileRequest.java
trainer/
  Trainer.java                   (entity)
  TrainerRepository.java
  TrainerController.java
  TrainerService.java
  dto/
    TrainerResponse.java
    DayAvailability.java
venue/
  Venue.java                     (entity)
  VenueRepository.java
  VenueController.java
  VenueService.java
  dto/
    VenueResponse.java
booking/
  Booking.java                   (entity)
  BookingRepository.java
  BookingController.java
  BookingService.java
  dto/
    CreateBookingRequest.java
    BookingResponse.java
common/
  security/
    SecurityConfig.java
    JwtConfig.java
    OAuth2UserService.java       (custom: handles user create/lookup after Google callback)
    OAuth2SuccessHandler.java    (issues our JWT after Google auth succeeds)
  exception/
    GlobalExceptionHandler.java
    ErrorResponse.java
    AppException.java            (base; subclassed by ConflictException, ForbiddenException, etc.)
  seed/
    DataSeeder.java              (ApplicationRunner; seeds trainers + venues on empty DB)
```

**Test mirror:** each feature package has `*ServiceTest.java` (unit) and `*ControllerTest.java` (integration) under `src/test/`.

---

## 4. Data Model

### `users`
```sql
id          UUID PRIMARY KEY DEFAULT gen_random_uuid()
google_id   VARCHAR(255) UNIQUE NOT NULL
email       VARCHAR(255) UNIQUE NOT NULL
name        VARCHAR(100) NOT NULL           -- from Google profile
city        VARCHAR(100)                    -- nullable until complete-profile step
avatar      VARCHAR(500)                    -- Google picture URL, nullable
created_at  TIMESTAMP NOT NULL DEFAULT now()
```

### `refresh_tokens`
```sql
id          UUID PRIMARY KEY DEFAULT gen_random_uuid()
user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE
token       VARCHAR(255) UNIQUE NOT NULL
expires_at  TIMESTAMP NOT NULL
revoked     BOOLEAN NOT NULL DEFAULT false
```

Rotation: each `POST /auth/refresh` revokes the current token and issues a new one. Revoked or expired tokens return `401`.

### `trainers`
```sql
id           VARCHAR(50) PRIMARY KEY         -- slug e.g. "marcus-reed"
name         VARCHAR(100) NOT NULL
sport        VARCHAR(50) NOT NULL
city         VARCHAR(100) NOT NULL
rating       DECIMAL(3,1) NOT NULL
reviews      INT NOT NULL DEFAULT 0
hourly_rate  INT NOT NULL                    -- stored in pence (6500 = £65)
experience   INT NOT NULL                    -- years
bio          TEXT
image_url    VARCHAR(500)
specialties  TEXT                            -- JSON array e.g. '["Serve & Volley"]'
```

### `venues`
```sql
id              VARCHAR(50) PRIMARY KEY
name            VARCHAR(100) NOT NULL
type            VARCHAR(20) NOT NULL         -- Court | Pitch | Stadium | Club
sport           VARCHAR(50) NOT NULL
city            VARCHAR(100) NOT NULL
price_per_hour  INT NOT NULL                 -- pence
rating          DECIMAL(3,1) NOT NULL
image_url       VARCHAR(500)
```

### `bookings`
```sql
id                UUID PRIMARY KEY DEFAULT gen_random_uuid()
user_id           UUID NOT NULL REFERENCES users(id)
trainer_id        VARCHAR(50) NOT NULL REFERENCES trainers(id)
trainer_name      VARCHAR(100) NOT NULL      -- snapshot at booking time
trainer_image_url VARCHAR(500)               -- snapshot
sport             VARCHAR(50) NOT NULL       -- snapshot
booking_date      DATE NOT NULL
slot_time         VARCHAR(5) NOT NULL        -- "HH:mm"
price             INT NOT NULL               -- pence snapshot of hourly_rate
status            VARCHAR(20) NOT NULL       -- CONFIRMED | CANCELLED
created_at        TIMESTAMP NOT NULL DEFAULT now()

-- Partial unique index (PostgreSQL): only one CONFIRMED booking per trainer/date/slot
-- CREATE UNIQUE INDEX uq_trainer_slot_confirmed
--   ON bookings (trainer_id, booking_date, slot_time)
--   WHERE status = 'CONFIRMED';
-- Hibernate cannot declare this via @Table(uniqueConstraints); create via schema.sql or migration.
```

**Price convention:** stored as integer pence throughout. API responses return `price / 100.0` as a `double`.  
**Timezone:** `booking_date` is `LocalDate` (date only, no timezone). Slot times (`"HH:mm"`) are naive/local in v1.  
**Seeding:** `DataSeeder` (`ApplicationRunner`) inserts the 4 trainers and 4 venues from `src/data/mock.ts` if `trainerRepository.count() == 0`.

---

## 5. Authentication & Authorization

### OAuth2 Flow

```
1. GET  /api/v1/auth/google
       → Spring Security redirects to Google consent screen

2. GET  /api/v1/auth/google/callback?code=...
       → Spring Security exchanges code for Google tokens
       → OAuth2UserService extracts email, name, google_id, picture
       → Looks up user by google_id:
           NEW:      create User (city=null), issue JWT { profileComplete: false }
           EXISTING: issue JWT { profileComplete: true } (if city set)
       → OAuth2SuccessHandler redirects to frontend:
           /auth/callback?token=<accessToken>&refresh=<refreshToken>

3. POST /api/v1/auth/complete-profile  { city }
       → requires JWT (profileComplete=false accepted)
       → sets city on user, issues new JWT { profileComplete: true }

4. All subsequent requests:
       → JWT in Authorization: Bearer header
       → Resource Server validates signature + expiry
       → profileComplete=false → 403 PROFILE_INCOMPLETE
         (except /auth/complete-profile and /auth/logout)
```

### JWT Claims
```json
{
  "sub": "<user-uuid>",
  "email": "user@gmail.com",
  "profileComplete": true,
  "iat": 1234567890,
  "exp": 1234567890
}
```

Access token TTL: **15 minutes**. Signed with RSA-256 (private key on server, public key for validation).  
Refresh token TTL: **30 days**. Stored in `refresh_tokens` table. Rotated on every refresh.

### Endpoint Access Matrix

| Endpoint | JWT required | profileComplete required |
|---|---|---|
| `GET /auth/google` | No | — |
| `GET /auth/google/callback` | No | — |
| `POST /auth/refresh` | No | — |
| `POST /auth/logout` | Yes | No |
| `POST /auth/complete-profile` | Yes | No |
| `GET /me`, `PATCH /me` | Yes | Yes |
| `GET /trainers/**`, `GET /venues` | No | — |
| `POST /bookings`, `GET /bookings`, `DELETE /bookings/{id}` | Yes | Yes |
| `GET /actuator/health` | No | — |

---

## 6. API Contract

**Base path:** `/api/v1`

### Auth

```
GET  /auth/google
     → 302 redirect to Google

GET  /auth/google/callback
     → handled by Spring Security
     → 302 redirect to frontend /auth/callback?token=...&refresh=...

POST /auth/refresh
     body:    { "refreshToken": "string" }
     returns: { "accessToken": "string", "refreshToken": "string" }
     errors:  401 if revoked/expired

POST /auth/logout
     JWT required
     body:    { "refreshToken": "string" }
     returns: 200 {}

POST /auth/complete-profile
     JWT required (profileComplete=false ok)
     body:    { "city": "London" }
     returns: { "accessToken": "string", "refreshToken": "string", "user": UserResponse }
     errors:  400 if city blank
```

### User

```
GET  /me
     returns: UserResponse

PATCH /me
     body:    { "name"?: "string", "city"?: "string", "avatar"?: "string" }
     returns: UserResponse
     errors:  400 on validation failure
```

### Trainers (public)

```
GET  /trainers
     query:   sport?, city?, q? (name/city search), page=0, size=20
     returns: PageResponse<TrainerResponse>

GET  /trainers/{id}
     returns: TrainerResponse
     errors:  404 if not found

GET  /trainers/{id}/availability
     query:   from=YYYY-MM-DD, to=YYYY-MM-DD
     returns: List<DayAvailability>
     errors:  404 if trainer not found | 400 if range > 14 days
```

### Venues (public)

```
GET  /venues
     query:   sport?, city?, page=0, size=20
     returns: PageResponse<VenueResponse>
```

### Bookings (JWT + profileComplete)

```
POST /bookings
     body:    { "trainerId": "string", "date": "YYYY-MM-DD", "slot": "HH:mm" }
     returns: BookingResponse (201)
     errors:  409 slot taken | 404 trainer not found | 400 validation

GET  /bookings
     query:   status=upcoming|past|all (default: all)
     returns: List<BookingResponse> sorted date ASC, slot ASC

DELETE /bookings/{id}
     returns: 200 {}
     errors:  403 not owner | 422 booking date in past | 404 not found
```

### Shared Response Shapes

```jsonc
// PageResponse<T>
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 4,
  "totalPages": 1
}

// DayAvailability
{ "date": "2026-04-26", "slots": ["08:00", "09:00", "14:00"] }

// BookingResponse
{
  "id": "uuid",
  "trainerId": "marcus-reed",
  "trainerName": "Marcus Reed",
  "trainerImageUrl": "...",
  "sport": "Tennis",
  "date": "2026-04-26",
  "slot": "08:00",
  "price": 65.0,
  "status": "CONFIRMED",
  "createdAt": "2026-04-26T10:00:00Z"
}

// ErrorResponse (all errors)
{ "error": "Slot already booked", "details": { "field": "message" } }
```

**Valid slot set (fixed):** `08:00 09:00 10:00 12:00 14:00 15:00 17:00 18:00 19:00`

---

## 7. Testing Strategy

### Unit Tests (`*ServiceTest.java`)

- Pure Java, no Spring context, no DB
- Mockito mocks repositories and dependencies
- Covers all business logic branches in service layer

**Required coverage:**

| Class | Cases |
|---|---|
| `BookingService` | Slot taken → ConflictException; past cancel → UnprocessableException; wrong owner → ForbiddenException; price snapshot is hourlyRate at booking time |
| `TokenService` | Revoked refresh → UnauthorizedException; expired refresh → UnauthorizedException; rotation issues new token and revokes old |
| `TrainerService` | Availability excludes CONFIRMED bookings; range > 14 days → IllegalArgumentException |
| `AuthService` | New Google user sets profileComplete=false; existing user with city set → profileComplete=true |

### Integration Tests (`*ControllerTest.java`)

- `@SpringBootTest` + `MockMvc` — full HTTP stack
- Testcontainers starts a real PostgreSQL container once per test class via `@DynamicPropertySource`
- Each test runs in a transaction that rolls back — no manual cleanup
- Google OAuth2 bypassed: a test helper creates a `User` directly in DB and calls `TokenService` to issue a real JWT

**Required coverage:**

| Scenario | Expected |
|---|---|
| `GET /trainers?sport=Tennis` | All results have sport=Tennis |
| `GET /trainers/unknown-id` | 404 |
| `GET /trainers/{id}/availability?from=T&to=T+30` | 400 |
| `GET /trainers/{id}/availability` after booking slot S | S absent from that day's slots |
| `POST /bookings` valid | 201, BookingResponse |
| `POST /bookings` same slot twice concurrently | One 201, one 409 |
| `POST /bookings` no JWT | 401 |
| `POST /bookings` profileComplete=false JWT | 403 |
| `DELETE /bookings/{id}` by owner | 200 |
| `DELETE /bookings/{id}` by other user | 403 |
| `DELETE /bookings/{id}` past booking | 422 |
| `GET /bookings?status=upcoming` | Only bookings with date ≥ today |
| `POST /auth/complete-profile` no JWT | 401 |
| `POST /auth/complete-profile` blank city | 400 with field error |
| `POST /auth/refresh` revoked token | 401 |
| `PATCH /me` valid patch | 200, updated UserResponse |

### Configuration

`src/test/resources/application-test.yml`:
- JPA pointed to Testcontainers DB via `@DynamicPropertySource`
- Fixed JWT RSA key pair (test-only, committed to repo)
- Actuator security disabled

### Build Enforcement

JaCoCo configured in `pom.xml`: `mvn verify` fails if line coverage on `com.findmycoach.*.service` and `com.findmycoach.*.controller` packages drops below 80%.

---

## 8. Environment Variables

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/findmycoach
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
APP_FRONTEND_URL=http://localhost:5173      # OAuth2 redirect target
APP_JWT_PRIVATE_KEY=                        # RSA private key (PEM)
APP_JWT_PUBLIC_KEY=                         # RSA public key (PEM)
APP_JWT_ACCESS_TOKEN_TTL=900               # seconds (15 min)
APP_JWT_REFRESH_TOKEN_TTL=2592000          # seconds (30 days)
```

---

## 9. Frontend Integration Contract

The existing `AuthContext` (`src/context/AuthContext.tsx`) must be replaceable by an API-backed version exposing the **identical interface** — no page component or `BookingCalendar` changes required.

| Frontend method | Backend call |
|---|---|
| `login()` | `GET /api/v1/auth/google` (redirect) |
| `logout()` | `POST /api/v1/auth/logout` |
| `updateProfile(patch)` | `PATCH /api/v1/me` |
| `addBooking(b)` | `POST /api/v1/bookings` |
| `cancelBooking(id)` | `DELETE /api/v1/bookings/{id}` |
| Load user on mount | `GET /api/v1/me` (if token in localStorage) |
| Load bookings on mount | `GET /api/v1/bookings` |

`BookingCalendar` replaces its deterministic hash with `GET /api/v1/trainers/{id}/availability`.

---

## 10. Explicit Non-Scope (Do Not Build)

- No payments or Stripe integration
- No real-time (WebSockets, SSE)
- No coach registration — trainers are seeded data only
- No venue bookings — venues endpoint is read-only
- No email sending
- No file uploads — image URLs are strings
- No admin panel
- No rating/review write endpoints
- No multi-region or CDN configuration