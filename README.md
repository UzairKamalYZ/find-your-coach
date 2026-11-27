# FindMyTrainer

Multi-sport coaching marketplace: trainees find and book coaches and venues, coaches manage their listing,
schedule and sessions, admins run the platform.

- **Frontend:** React 18 + TypeScript + Vite + Tailwind/shadcn (this folder)
- **Backend:** Spring Boot 3 + H2 (`backend/`) — see [`docs/backend.md`](docs/backend.md)

## Run locally

Requires Node 18+, Java 21 and Maven.

```bash
npm install
npm run dev:backend   # API on http://localhost:8081 (seeds demo data)
npm run dev           # app on http://localhost:8080, proxies /api to the backend
```

Sign in with `sam@findmytrainer.dev` / `Trainee@123` (trainee), `marcus@findmytrainer.dev` / `Trainer@123` (coach)
or `admin@findmytrainer.dev` / `Admin@12345` (admin).

## Tests

```bash
npm test              # frontend (Vitest)
npm run test:backend  # backend (JUnit + MockMvc on H2)
```

## Deploying the frontend separately

Set `VITE_API_URL` (see `.env.example`) to the backend's `/api/v1` URL at build time and add the frontend origin
to the backend's `APP_CORS_ALLOWED_ORIGINS`.
