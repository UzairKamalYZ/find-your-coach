#!/usr/bin/env bash
#
# backend/deployment/deploy.sh — run the FindMyTrainer API and keep it on the latest main.
#
# Builds the backend (mvn package) and starts it, then every DEPLOY_POLL_SECONDS:
#   - restarts the API if it has died;
#   - asks GitHub whether main has moved; if it has: pulls it and hands over to the new
#     commit's version of this script, which builds and tests the backend if it changed,
#     restarts the API if its jar or settings changed, and checks /actuator/health.
#     So changes to this script apply to the very deploy that brings them in.
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
    SKIPPED_FILE="$RUN_DIR/skipped"       # a commit that could not be deployed

    # The environment the service started this script with. Each version of this script
    # hands over to the next with exactly this, so settings one version sets can't leak
    # into another (e.g. a bad setting into the version a rollback goes back to).
    mkdir -p "$RUN_DIR"
    if [ -z "${DEPLOY_HANDOVER:-}" ]; then
        (umask 077; export -p >"$RUN_DIR/base.env")
    fi

    # Settings passed to Spring Boot (see backend/src/main/resources/application.yaml)
    export PORT="${PORT:-9090}"
    export APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-https://coach.kamtechsolutions.eu}"
    # The H2 console needs no login; it must stay off while the API is public.
    export H2_CONSOLE_ENABLED="${H2_CONSOLE_ENABLED:-false}"
    # One line per request in app.log: time, visitor IP (Cloudflare passes it on), request,
    # status, bytes, duration. DEPLOY_ACCESS_LOG=false turns it off.
    if [ "${DEPLOY_ACCESS_LOG:-true}" = "true" ]; then
        export SERVER_TOMCAT_ACCESSLOG_ENABLED=true
        export SERVER_TOMCAT_ACCESSLOG_DIRECTORY=/dev
        export SERVER_TOMCAT_ACCESSLOG_PREFIX=stdout
        export SERVER_TOMCAT_ACCESSLOG_SUFFIX=
        export SERVER_TOMCAT_ACCESSLOG_FILE_DATE_FORMAT=
        export SERVER_TOMCAT_ACCESSLOG_BUFFERED=false
        export SERVER_TOMCAT_ACCESSLOG_PATTERN='%{yyyy-MM-dd HH:mm:ss}t [access] %{CF-Connecting-IP}i "%r" %s %b %{ms}Tms'
    fi
    HEALTH_URL="http://127.0.0.1:$PORT/actuator/health"

    cd "$REPO_DIR" || exit 1
    mkdir -p "$RUN_DIR" "$LOG_DIR" "$BACKEND_DIR/data"

    preflight
    jwt_secret
    trap 'log "stopping"; stop; exit 0' INT TERM

    # Set when the previous version of this script hands over to this one after pulling
    # a new commit: the commit to go back to if this one can't be deployed.
    local rollback_to="${1:-}"

    if [ -z "$rollback_to" ] && checkout_ok >/dev/null && fetch && behind &&
        [ "$(git rev-parse "$REMOTE/$BRANCH")" != "$(cat "$SKIPPED_FILE" 2>/dev/null)" ]; then
        log "checkout is behind $REMOTE/$BRANCH — updating before the first start"
        pull || die "could not fast-forward to $REMOTE/$BRANCH (see above)"
    fi
    deploy_head "$rollback_to"
    local deployed   # the commit the API is running
    deployed="$(git rev-parse HEAD)"
    log "running $(git rev-parse --short HEAD) on port $PORT; checking $REMOTE/$BRANCH every ${POLL_SECONDS}s"

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
        # a commit that could not be deployed — not retried until main moves again
        [ "$target" = "$(cat "$SKIPPED_FILE" 2>/dev/null)" ] && continue
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
            echo "$target" >"$SKIPPED_FILE"
            continue
        fi
        if ! pull; then
            log "NOT deployed — could not fast-forward"
            echo "$target" >"$SKIPPED_FILE"
            continue
        fi
        # Hand over to the new commit's version of this script, so changes to it (and to
        # the settings it gives the API) apply to this deploy. It rolls back to $previous
        # if the new commit can't be deployed.
        handover "$previous"
    done
}

# Brings the API in line with HEAD: rebuilds it when the backend code changed, restarts
# it when the jar or the settings this script gives it changed, starts it when it is not
# running. Given a commit to roll back to, a version that fails to build or to report
# healthy is undone: the checkout goes back to that commit, and that commit's version of
# this script takes over and starts the previous jar again.
deploy_head() {
    local rollback_to="$1" installed="" changed=""
    if ! jar_current; then
        if build; then
            log "deploying $(git rev-parse --short HEAD)"
            stop
            install_jar
            installed=yes
        elif [ -n "$rollback_to" ]; then
            roll_back "$rollback_to"
        elif ! is_running || [ ! -f "$JAR" ]; then
            die "build failed — see backend/deployment/.logs/build.log"
        else
            log "WARNING: build failed — the API keeps running the jar it has"
        fi
    fi
    if is_running && [ "$(cat "$RUN_DIR/app.settings" 2>/dev/null)" != "$(settings_id)" ]; then
        log "settings for the API changed — restarting it"
        stop
    fi
    if ! is_running; then
        start
        changed=yes
    fi
    if healthy; then
        if [ -n "$rollback_to" ]; then
            if [ -n "$changed" ]; then log "deployed $(git rev-parse --short HEAD)"
            else log "updated to $(git rev-parse --short HEAD) — nothing changed for the API"; fi
        fi
        return 0
    fi
    if [ -n "$rollback_to" ] && [ -n "$changed" ]; then
        log "new version does not report healthy — rolling back (see backend/deployment/.logs/app.log)"
        stop
        [ -n "$installed" ] && restore_jar
        roll_back "$rollback_to"
    fi
    log "WARNING: API does not report healthy at $HEALTH_URL — see backend/deployment/.logs/app.log"
}

# Puts the checkout back on the given commit, marks the current one as skipped and hands
# over to that commit's version of this script.
roll_back() {
    git rev-parse HEAD >"$SKIPPED_FILE"
    git reset --quiet --keep "$1" || die "could not go back to $(git rev-parse --short "$1")"
    log "NOT deployed — back on $(git rev-parse --short HEAD)"
    handover
}

# Runs the version of this script now on disk in place of this one, with the environment
# the service started the first version with.
handover() {
    exec env -i DEPLOY_HANDOVER=1 bash -c '. "$1" && shift && exec "$@"' _ "$RUN_DIR/base.env" "$SELF" "$@"
}

# Identifies the settings this script gives the API; start() records it.
settings_id() {
    { env | grep -E '^(PORT=|APP_|H2_|SERVER_|SPRING_|JAVA_OPTS=)' | sort; } | sha256sum | cut -d' ' -f1
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

# Identifies the backend code at a commit (backend/ except this deployment folder): equal
# ids mean the jar does not need rebuilding.
backend_tree() {
    git ls-tree -r "$1" -- backend | grep -v $'\tbackend/deployment/' | sha256sum | cut -d' ' -f1
}

# True when the API's jar was built from the backend code at HEAD.
jar_current() {
    [ -f "$JAR" ] && [ "$(cat "$RUN_DIR/app.commit" 2>/dev/null)" = "$(backend_tree HEAD)" ]
}

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
    settings_id >"$RUN_DIR/app.settings"
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
