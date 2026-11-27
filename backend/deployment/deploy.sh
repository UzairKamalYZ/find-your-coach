#!/usr/bin/env bash
#
# backend/deployment/deploy.sh — run the FindMyTrainer API and keep it on the latest main.
#
# Builds the backend (mvn package) and starts it, then every DEPLOY_POLL_SECONDS:
#   - restarts the API if it has died;
#   - asks GitHub whether main has moved; if it has: pulls it and, when anything under
#     backend/ changed, builds and tests it, restarts the API on the new jar and checks
#     /actuator/health.
# A commit that fails to build or fails its tests is rolled back and skipped; the API
# keeps running the last good version. A commit that starts but does not report healthy
# is rolled back, and the previous jar is started again. Commits that only touch the
# frontend are pulled without restarting the API.
#
# Runs in the foreground until stopped (Ctrl-C / SIGTERM stops the API too).
# See backend/deployment/README.md.

# Everything lives in main() so bash has read the whole file before running any of
# it — the script may be replaced on disk by the very pull it performs.
main() {
    set -uo pipefail
    # Nothing here reads the keyboard (keeps `nohup … &` from being suspended).
    exec </dev/null

    BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    REPO_DIR="$(cd "$BACKEND_DIR/.." && pwd)"
    SELF="$BACKEND_DIR/deployment/deploy.sh"
    REMOTE="${DEPLOY_REMOTE:-origin}"
    BRANCH="${DEPLOY_BRANCH:-main}"
    POLL_SECONDS="${DEPLOY_POLL_SECONDS:-60}"
    RUN_TESTS="${DEPLOY_RUN_TESTS:-true}"
    STOP_TIMEOUT="${DEPLOY_STOP_TIMEOUT:-30}"
    HEALTH_TIMEOUT="${DEPLOY_HEALTH_TIMEOUT:-90}"
    RUN_DIR="$BACKEND_DIR/deployment/.run"
    LOG_DIR="$BACKEND_DIR/deployment/.logs"
    PID_FILE="$RUN_DIR/app.pid"
    JAR="$RUN_DIR/app.jar"                # the jar the API runs from
    PREVIOUS_JAR="$RUN_DIR/app.previous.jar"

    # Settings passed to Spring Boot (see backend/src/main/resources/application.yaml)
    export PORT="${PORT:-9090}"
    export APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-https://coach.kamtechsolutions.eu}"
    # The H2 console needs no login; it must stay off while the API is public.
    export H2_CONSOLE_ENABLED="${H2_CONSOLE_ENABLED:-false}"
    HEALTH_URL="http://127.0.0.1:$PORT/actuator/health"

    cd "$REPO_DIR" || exit 1
    mkdir -p "$RUN_DIR" "$LOG_DIR" "$BACKEND_DIR/data"

    preflight
    jwt_secret
    trap 'log "stopping"; stop; exit 0' INT TERM

    if checkout_ok >/dev/null && fetch && behind; then
        log "checkout is behind $REMOTE/$BRANCH — updating before the first start"
        pull || die "could not fast-forward to $REMOTE/$BRANCH (see above)"
    fi
    # After a deploy this script exec's itself with the new version already running.
    if ! is_running; then
        if [ ! -f "$JAR" ] || [ "$(cat "$RUN_DIR/app.commit" 2>/dev/null)" != "$(backend_tree HEAD)" ]; then
            build || die "build failed — see backend/deployment/.logs/build.log"
            install_jar
        fi
        start
    fi
    healthy || log "WARNING: API does not report healthy at $HEALTH_URL — see backend/deployment/.logs/app.log"
    local deployed   # the commit the API is running
    deployed="$(git rev-parse HEAD)"
    log "running $(git rev-parse --short HEAD) on port $PORT; checking $REMOTE/$BRANCH every ${POLL_SECONDS}s"

    local skipped=""   # a commit that could not be deployed — not retried until main moves again
    local paused=""    # why the checkout can't be updated right now (logged once)
    while true; do
        sleep "$POLL_SECONDS" &
        wait $!        # an interruptible sleep: the trap runs at once
        if ! is_running; then
            log "API is not running — restarting (its last output: backend/deployment/.logs/app.log)"
            start
        fi
        fetch || continue
        local target previous why
        target="$(git rev-parse "$REMOTE/$BRANCH")"
        [ "$target" = "$deployed" ] && continue
        [ "$target" = "$skipped" ] && continue
        if ! why="$(checkout_ok)"; then
            [ "$why" != "$paused" ] && log "NOT deploying yet — $why"
            paused="$why"
            continue
        fi
        paused=""
        previous="$(git rev-parse HEAD)"
        log "new commit on $BRANCH: $(git log -1 --format='%h %s' "$target")"
        if [ "$previous" != "$target" ] && ! behind; then
            log "NOT deployed — this checkout has commits that $REMOTE/$BRANCH doesn't"
            skipped="$target"
            continue
        fi
        pull || { log "NOT deployed — could not fast-forward"; skipped="$target"; continue; }
        if [ "$(backend_tree "$previous")" = "$(backend_tree HEAD)" ]; then
            log "no backend changes — API keeps running"
            deployed="$(git rev-parse HEAD)"
            exec "$SELF"   # pick up any change to this script
        fi
        if build; then
            log "deploying $(git rev-parse --short HEAD)"
            stop
            install_jar
            start
            if healthy; then
                log "deployed $(git rev-parse --short HEAD)"
                exec "$SELF"   # carry on as the new version of this script
            fi
            log "new version does not report healthy — rolling back (see backend/deployment/.logs/app.log)"
            stop
            restore_jar
            start
            healthy || log "WARNING: the previous version does not report healthy either"
        fi
        git reset --quiet --keep "$previous"
        log "NOT deployed — staying on $(git rev-parse --short "$previous")"
        skipped="$target"
    done
}

log() { printf '%s [deploy] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
die() { log "ERROR: $*"; exit 1; }

preflight() {
    command -v git >/dev/null || die "git is not installed"
    command -v java >/dev/null || die "java is not installed (needs JDK 21)"
    command -v mvn >/dev/null || die "maven is not installed"
    command -v curl >/dev/null || die "curl is not installed"
}

# The JWT signing secret: APP_JWT_SECRET if set, otherwise a random one kept in
# backend/data/.jwt-secret (git-ignored) so sign-ins survive restarts.
jwt_secret() {
    if [ -z "${APP_JWT_SECRET:-}" ]; then
        local file="$BACKEND_DIR/data/.jwt-secret"
        [ -s "$file" ] || (umask 077; openssl rand -hex 32 >"$file") || die "could not create $file"
        APP_JWT_SECRET="$(cat "$file")"
    fi
    export APP_JWT_SECRET
}

# This folder is also where the app is worked on, so only update it when it is on the
# branch and has no uncommitted changes to tracked files. Prints the reason otherwise.
checkout_ok() {
    local current
    current="$(git rev-parse --abbrev-ref HEAD)"
    [ "$current" = "$BRANCH" ] || { echo "checkout is on '$current', not '$BRANCH'"; return 1; }
    [ -z "$(git status --porcelain --untracked-files=no)" ] ||
        { echo "checkout has uncommitted changes"; return 1; }
}

# A failed fetch (GitHub or the network is down) just means "nothing new right now".
fetch() {
    git fetch --quiet "$REMOTE" "+refs/heads/$BRANCH:refs/remotes/$REMOTE/$BRANCH" 2>/dev/null ||
        { log "fetch failed — will retry"; return 1; }
}

# True when the remote branch has commits this checkout doesn't, and nothing the other way.
behind() {
    [ "$(git rev-parse HEAD)" != "$(git rev-parse "$REMOTE/$BRANCH")" ] &&
        git merge-base --is-ancestor HEAD "$REMOTE/$BRANCH"
}

pull() { git merge --quiet --ff-only "$REMOTE/$BRANCH"; }

# Identifies the backend/ code at a commit: equal ids mean nothing in backend/ changed.
backend_tree() { git rev-parse "$1:backend"; }

# Builds the jar, running the tests unless DEPLOY_RUN_TESTS=false.
build() {
    local skip=""
    [ "$RUN_TESTS" = "true" ] || skip="-DskipTests"
    log "building$([ -z "$skip" ] && echo " and testing")"
    (cd "$BACKEND_DIR" && mvn -B -q clean package $skip) >"$LOG_DIR/build.log" 2>&1 && return 0
    log "build or tests failed — see backend/deployment/.logs/build.log"
    return 1
}

# Copies the freshly built jar out of target/, so the next build can't disturb the
# running API, and keeps the current one for a rollback.
install_jar() {
    local built
    built="$(ls "$BACKEND_DIR"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -1)"
    [ -n "$built" ] || die "no jar in backend/target"
    [ -f "$JAR" ] && mv -f "$JAR" "$PREVIOUS_JAR" && cp -f "$RUN_DIR/app.commit" "$RUN_DIR/app.previous.commit" 2>/dev/null
    cp -f "$built" "$JAR"
    backend_tree HEAD >"$RUN_DIR/app.commit"
}

restore_jar() {
    [ -f "$PREVIOUS_JAR" ] || { log "no previous jar to roll back to"; return 1; }
    mv -f "$PREVIOUS_JAR" "$JAR"
    mv -f "$RUN_DIR/app.previous.commit" "$RUN_DIR/app.commit" 2>/dev/null
}

is_running() {
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null)"
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

start() {
    : >"$LOG_DIR/app.log"   # output of the current run only
    # Runs from backend/ so the H2 database stays at backend/data/
    (cd "$BACKEND_DIR" && exec java ${JAVA_OPTS:-} -jar "$JAR") >>"$LOG_DIR/app.log" 2>&1 &
    echo $! >"$PID_FILE"
    log "started API (pid $!)"
}

stop() {
    local pid waited=0
    pid="$(cat "$PID_FILE" 2>/dev/null)"
    rm -f "$PID_FILE"
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null || return 0
    kill -TERM "$pid" 2>/dev/null
    while kill -0 "$pid" 2>/dev/null && [ "$waited" -lt "$STOP_TIMEOUT" ]; do
        sleep 1
        waited=$((waited + 1))
    done
    kill -KILL "$pid" 2>/dev/null
    log "stopped API"
}

# Waits up to HEALTH_TIMEOUT seconds for the API to report UP.
healthy() {
    local waited=0
    while [ "$waited" -lt "$HEALTH_TIMEOUT" ]; do
        curl -fsS --max-time 2 "$HEALTH_URL" 2>/dev/null | grep -q '"UP"' && return 0
        is_running || return 1
        sleep 1
        waited=$((waited + 1))
    done
    return 1
}

main "$@"
exit
