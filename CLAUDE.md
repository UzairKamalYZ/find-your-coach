# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev            # Vite dev server on :8080 (proxies /api → :8081)
npm run dev:backend    # Spring Boot API on :8081 (cd backend && mvn spring-boot:run)
npm run build          # Production build
npm run lint           # ESLint
npm run test           # Frontend tests once (Vitest)
npm run test:backend   # Backend tests (cd backend && mvn test)
```

Single frontend test: `npx vitest run src/path/to/file.test.ts`. Single backend test: `cd backend && mvn test -Dtest=BookingApiTest`.

## Architecture

**Frontend:** React 18 + TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router v6, TanStack Query.
**Backend:** Spring Boot 3.3 / Java 21 in `backend/`, H2 database (file `backend/data/` by default), JWT auth. Full design, business rules and endpoint list: `docs/backend.md`.

- **API client:** `src/lib/api.ts` (base URL `VITE_API_URL` or `/api/v1`, bearer token, automatic refresh-token rotation). Response types in `src/lib/types.ts` mirror the Java DTOs.
- **Auth/global state:** `src/context/AuthContext.tsx` exposes `user`, `ready`, `bookings`, `venueBookings` and async actions (`login`, `signup`, `socialLogin`, `logout`, `updateProfile`, `addBooking`, `cancelBooking`, `refreshBookings`). Read data (coaches, venues, availability, reviews) uses TanStack Query hooks in `src/hooks/use-api.ts`.
- **Roles:** `trainee`, `trainer`, `admin`. Protected pages wrap content in `RequireAuth` (optionally with `roles`). Trainers get `/coach` (Coach Hub), admins `/admin`.
- **Images:** the API stores bundled asset keys like `trainer-1`/`venue-tennis` or URLs; resolve with `resolveImage()` from `src/lib/images.ts`.
- **Path alias:** `@/` maps to `src/`.
- **Tests:** Vitest + jsdom + Testing Library (`src/**/*.test.tsx`, fetch is mocked). Backend: `@SpringBootTest` + MockMvc integration tests on in-memory H2 with seeded demo data, each test rolled back.

**Pages:** `Index`, `Trainers`, `TrainerDetail` (booking via `BookingCalendar`, reviews), `Venues` (reservation dialog), `Login`, `Profile`, `MyCalendar` (cancel/reschedule/review), `CoachDashboard`, `AdminDashboard`.
