# Backend deployment

The Spring Boot API runs natively on this machine (no Docker) and is published through the
Cloudflare tunnel as `https://coachbff.kamtechsolutions.eu`. `deploy.sh` runs it and keeps it
on the latest `main`, the same way my-stats and uk-agents deploy themselves. It needs `git`,
`java` (21), `mvn` and `curl`. Only the backend is deployed here; the frontend is built and
hosted by Cloudflare.

It builds the jar (`mvn package`, tests included), starts it with `java -jar` on port 9090,
then every minute it:

1. restarts the API if it has died;
2. asks GitHub whether `main` has moved. If it has, it fast-forwards the checkout. If anything
   under `backend/` changed, it builds and tests the new code, restarts the API on the new
   jar and waits for `/actuator/health` to report `UP`. Frontend-only commits are pulled
   without restarting the API.

If the build or the tests fail, the checkout is rolled back and the API keeps running the
last good version. If the new version isn't healthy within `DEPLOY_HEALTH_TIMEOUT` seconds,
the checkout is rolled back and the previous jar is started again. Either way, that commit
is skipped until `main` moves again.

`~/FindMyTrainer/find-your-coach` is also where the app is worked on, so the script only
updates it while it is on `main` with no uncommitted changes to tracked files; otherwise it
logs why and waits. It never touches local changes. Changes reach the API once they are
pushed to `main` on GitHub.

## Install (systemd user service)

```bash
cp backend/deployment/find-my-coach-api.service ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now find-my-coach-api
```

| To | Run |
|---|---|
| See whether it is running | `systemctl --user status find-my-coach-api` |
| Follow the deploy log | `journalctl --user -u find-my-coach-api -f` |
| Follow the API's own log | `tail -f backend/deployment/.logs/app.log` |
| Restart (e.g. after changing the unit's environment) | `systemctl --user restart find-my-coach-api` |
| Stop the API | `systemctl --user stop find-my-coach-api` |

Or run it in the foreground: `./backend/deployment/deploy.sh` (Ctrl-C stops it and the API).

## Logs

| Where | What is in it |
|---|---|
| `journalctl --user -u find-my-coach-api` | The deploy log: starts, new commits, deploys, rollbacks |
| `backend/deployment/.logs/app.log` | Spring Boot's output since the API last started |
| `backend/deployment/.logs/build.log` | The last build and test run — look here when a commit was not deployed |

## Data and secrets

- Database: H2 file at `backend/data/findmycoach.mv.db`. Deploys keep it.
- JWT signing secret: `backend/data/.jwt-secret`, created on first start (git-ignored). Deleting
  it signs everyone out.

## Settings

Environment variables, all optional (in the unit: `Environment=DEPLOY_POLL_SECONDS=30`).

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `9090` | Port the API listens on (the tunnel must point here) |
| `APP_CORS_ALLOWED_ORIGINS` | `https://coach.kamtechsolutions.eu` | Frontend origins allowed to call the API |
| `APP_JWT_SECRET` | from `backend/data/.jwt-secret` | JWT signing secret |
| `H2_CONSOLE_ENABLED` | `false` | Keep it off: the console needs no login |
| `JAVA_OPTS` | (none) | Extra JVM options, e.g. `-Xmx512m` |
| `DEPLOY_BRANCH` | `main` | Branch to follow |
| `DEPLOY_REMOTE` | `origin` | Git remote to fetch from |
| `DEPLOY_POLL_SECONDS` | `60` | How often to check GitHub and the API |
| `DEPLOY_RUN_TESTS` | `true` | `false` deploys without running the tests |
| `DEPLOY_HEALTH_TIMEOUT` | `90` | Seconds a new version gets to report healthy |
| `DEPLOY_STOP_TIMEOUT` | `30` | Seconds the API gets to shut down before it is killed |
