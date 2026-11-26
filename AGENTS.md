# Architecture rules
- All application state (accounts, coaches, venues, bookings, reviews, notifications) lives in the Spring Boot API in `backend/`; the frontend only keeps the token pair in `localStorage`.
- Social sign-in buttons are demonstration-only: the backend maps each provider to a fixed sample identity and never contacts a real provider. Keep them labelled as demo.
- Frontend API access goes through `src/lib/api.ts` (fetch wrapper with token refresh) and the shapes in `src/lib/types.ts`; keep those types in sync with the backend DTOs.
- Keep reusable contact form fields separate from account pages so labels and input behavior stay consistent.
