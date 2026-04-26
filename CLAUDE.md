# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev          # Start Vite dev server
npm run build        # Production build (TypeScript check + Vite)
npm run lint         # ESLint
npm run test         # Run tests once (Vitest)
npm run test:watch   # Run tests in watch mode
```

To run a single test file: `npx vitest run src/path/to/file.test.ts`

## Architecture

This is a **pure frontend SPA** — no backend, no API calls. All state persists to `localStorage`.

**Stack:** React 18 + TypeScript, Vite, Tailwind CSS, shadcn/ui (Radix UI primitives), React Router v6, TanStack Query, React Hook Form + Zod.

**Key architectural decisions:**

- **Mock auth & data:** `src/context/AuthContext.tsx` implements auth entirely in memory/localStorage — `login()` accepts any credentials. All trainer/venue data lives in `src/data/mock.ts` as static arrays.
- **Global state:** `AuthContext` is the sole context, providing `user`, `bookings`, and CRUD operations. Components consume it via `useAuth()`. TanStack Query is wired up in `App.tsx` but not currently used for data fetching (data comes from `mock.ts`).
- **Routing:** Flat route structure defined in `App.tsx`. Protected routes are enforced per-page by checking `useAuth().user`.
- **Path alias:** `@/` maps to `src/` (configured in both `vite.config.ts` and `tsconfig.app.json`).
- **Tests:** Vitest with jsdom + `@testing-library/react`. Setup file at `src/test/setup.ts`. Tests live alongside source in `src/**/*.test.{ts,tsx}`.

**Pages:** `Index` (landing), `Trainers` (list/filter), `TrainerDetail` (booking flow with `BookingCalendar`), `Venues`, `Login`, `Profile`, `MyCalendar` (user's bookings).