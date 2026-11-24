
#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
unset DOCKER_HOST DOCKER_CONTEXT DOCKER_CONFIG

readonly DEPLOY_DIR="/opt/portfolio-api"
readonly BACKUP_ROOT="/var/backups/portfolio-api"
readonly PROJECT="company-web-backend"
readonly BACKEND="company-web-backend-backend-1"
readonly DB="company-web-backend-db-1"

CHECK_ONLY=false

if [[ "${1:-}" == "--check" ]]; then
    CHECK_ONLY=true
    shift
fi

if [[ $# -ne 1 || ! "${1:-}" =~ ^(deploy|rollback)-[0-9]{8}T[0-9]{6}Z-[a-zA-Z0-9]{6}$ ]]; then
    echo "Usage: $0 [--check] <backup-id>" >&2
    exit 1
fi

TARGET_DIR="$BACKUP_ROOT/$1"

cd "$DEPLOY_DIR"

exec 9>.deploy.lock
flock -x 9

compose() {
    local image="$1"
    local profile="$2"
    local env_file="$3"
    shift 3

    env -i PATH="$PATH" HOME=/root BACKEND_IMAGE="$image" PRODUCTION_SPRING_PROFILE="$profile" docker compose -p "$PROJECT" --env-file "$env_file" -f docker-compose.yml -f compose.production.yaml "$@"
}

migration_history() {
    docker exec "$DB" sh -c 'exec psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -At -c "SELECT installed_rank, version, description, type, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank;"'
}

smoke_test() {
    curl --noproxy '*' -fsS --max-time 15 http://127.0.0.1:8001/api/v1/actuator/health |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' || return 1

    curl --noproxy '*' -fsS --max-time 15 http://127.0.0.1:8001/api/v1/projects |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "success")' || return 1
}

record_event() {
    if ! printf '%s\t%s\t%s\t%s\t%s\n' "$(date -u +%FT%TZ)" "$1" "$2" "$3" "$(basename "$BACKUP_DIR")" >> release-history.tsv; then
        echo "Warning: unable to write release history." >&2
    fi
}

echo "Validating rollback snapshot..."

if [[ ! -d "$TARGET_DIR" || -L "$TARGET_DIR" ]]; then
    echo "Rollback snapshot not found or invalid." >&2
    exit 1
fi

(
    cd "$TARGET_DIR"
    sha256sum -c SHA256SUMS
)

TARGET_IMAGE=$(cat "$TARGET_DIR/rollback-image.txt")
TARGET_ID=$(cat "$TARGET_DIR/image-id.txt")

if [[ "$TARGET_IMAGE" != "portfolio-api-rollback:$(basename "$TARGET_DIR")" ]]; then
    echo "Invalid rollback image reference." >&2
    exit 1
fi

if [[ ! "$TARGET_ID" =~ ^sha256:[a-f0-9]{64}$ ]]; then
    echo "Invalid rollback image ID." >&2
    exit 1
fi

if [[ "$(docker image inspect --format '{{.Id}}' "$TARGET_IMAGE")" != "$TARGET_ID" ]]; then
    echo "Rollback image ID mismatch." >&2
    exit 1
fi

TARGET_PROFILE=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["services"]["backend"]["environment"]["SPRING_PROFILES_ACTIVE"])' "$TARGET_DIR/rollback.compose.json")

if [[ "$TARGET_PROFILE" != "prod" && -n "$TARGET_PROFILE" ]]; then
    echo "Unsupported historical Spring profile." >&2
    exit 1
fi

echo "Checking Compose compatibility..."

if ! cmp -s docker-compose.yml "$TARGET_DIR/docker-compose.yml"; then
    echo "Base Compose configuration has changed." >&2
    echo "Manual configuration review required." >&2
    exit 1
fi

echo "Checking database migration compatibility..."

CURRENT_HISTORY=$(migration_history)

if [[ -z "$CURRENT_HISTORY" ]] || grep -q '|f$' <<< "$CURRENT_HISTORY"; then
    echo "Invalid current database migration history." >&2
    exit 1
fi

if ! printf '%s\n' "$CURRENT_HISTORY" | cmp -s - "$TARGET_DIR/migration-history.txt"; then
    echo "Database migration history mismatch." >&2
    echo "Rollback refused." >&2
    exit 1
fi

echo "Validating current production configuration..."

if [[ $(grep -c '^BACKEND_IMAGE=' .env) -ne 1 ]]; then
    echo "Invalid production image configuration." >&2
    exit 1
fi

CURRENT_IMAGE=$(sed -n 's/^BACKEND_IMAGE=//p' .env)
CURRENT_ID=$(docker inspect --format '{{.Image}}' "$BACKEND")
CURRENT_PROFILE=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$BACKEND" | sed -n 's/^SPRING_PROFILES_ACTIVE=//p')

if [[ "$(docker image inspect --format '{{.Id}}' "$CURRENT_IMAGE")" != "$CURRENT_ID" ]]; then
    echo "Current image configuration mismatch." >&2
    exit 1
fi

CONFIGURED_PROFILE=$(env -i PATH="$PATH" HOME=/root docker compose -p "$PROJECT" --env-file .env -f docker-compose.yml -f compose.production.yaml config --format json | python3 -c 'import json,sys; print(json.load(sys.stdin)["services"]["backend"]["environment"]["SPRING_PROFILES_ACTIVE"])')

if [[ "$CONFIGURED_PROFILE" != "$CURRENT_PROFILE" ]]; then
    echo "Current Spring profile configuration mismatch." >&2
    exit 1
fi

compose "$TARGET_IMAGE" "$TARGET_PROFILE" .env config --quiet

echo "Current image: $CURRENT_IMAGE"
echo "Target image: $TARGET_IMAGE"
echo "Target profile: ${TARGET_PROFILE:-<empty>}"

if [[ "$CHECK_ONLY" == true ]]; then
    echo "Production rollback preflight passed."
    echo "No production containers were modified."
    exit 0
fi

if [[ "$CURRENT_ID" == "$TARGET_ID" && "$CURRENT_PROFILE" == "$TARGET_PROFILE" ]]; then
    if smoke_test; then
        echo "Target release is already running and healthy."
        exit 0
    fi

    echo "Target release is already running but unhealthy." >&2
    exit 1
fi

echo "Backing up current production before rollback..."

BACKUP_DIR=$(mktemp -d "$BACKUP_ROOT/rollback-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")

RECOVERY_IMAGE="portfolio-api-rollback:$(basename "$BACKUP_DIR")"

docker image tag "$CURRENT_ID" "$RECOVERY_IMAGE"

cp .env "$BACKUP_DIR/.env"
cp docker-compose.yml "$BACKUP_DIR/docker-compose.yml"
cp compose.production.yaml "$BACKUP_DIR/compose.production.yaml"

printf '%s\n' "$CURRENT_ID" > "$BACKUP_DIR/image-id.txt"
printf '%s\n' "$RECOVERY_IMAGE" > "$BACKUP_DIR/rollback-image.txt"
printf '%s\n' "$CURRENT_HISTORY" > "$BACKUP_DIR/migration-history.txt"

python3 -c '
import json,sys
json.dump({
    "services": {
        "backend": {
            "image": "${BACKEND_IMAGE}",
            "environment": {
                "SPRING_PROFILES_ACTIVE": sys.argv[1]
            }
        }
    }
}, sys.stdout)
' "$CURRENT_PROFILE" > "$BACKUP_DIR/rollback.compose.json"

docker exec "$DB" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_dump --format=custom --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' > "$BACKUP_DIR/database.dump"

docker exec -i "$DB" pg_restore --list < "$BACKUP_DIR/database.dump" > "$BACKUP_DIR/database-contents.txt"

(
    cd "$BACKUP_DIR"
    sha256sum .env docker-compose.yml compose.production.yaml database.dump rollback.compose.json image-id.txt rollback-image.txt migration-history.txt database-contents.txt > SHA256SUMS
    sha256sum -c SHA256SUMS
)

echo "Recovery snapshot: $BACKUP_DIR"

TEMP_ENV=$(mktemp .env.rollback.XXXXXX)
trap 'rm -f -- "$TEMP_ENV"' EXIT

sed -e "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=$TARGET_IMAGE|" -e '/^PRODUCTION_SPRING_PROFILE=/d' .env > "$TEMP_ENV"

printf '\nPRODUCTION_SPRING_PROFILE=%s\n' "$TARGET_PROFILE" >> "$TEMP_ENV"

compose "$TARGET_IMAGE" "$TARGET_PROFILE" "$TEMP_ENV" config --quiet

record_event "MANUAL_ROLLBACK_STARTED" "$CURRENT_IMAGE" "$TARGET_IMAGE"

restore_target() {
    compose "$TARGET_IMAGE" "$TARGET_PROFILE" "$TEMP_ENV" up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend || return 1

    [[ "$(docker inspect --format '{{.Image}}' "$BACKEND")" == "$TARGET_ID" ]] || return 1

    smoke_test || return 1

    [[ "$(migration_history)" == "$CURRENT_HISTORY" ]] || return 1
}

echo "Restoring previous production release..."

if restore_target && mv -f "$TEMP_ENV" .env; then
    record_event "MANUAL_ROLLBACK_SUCCESS" "$CURRENT_IMAGE" "$TARGET_IMAGE"

    echo "Production rollback successful."
    echo "Restored image: $TARGET_IMAGE"
    echo "Recovery snapshot: $BACKUP_DIR"
    exit 0
fi

record_event "MANUAL_ROLLBACK_FAILED" "$CURRENT_IMAGE" "$TARGET_IMAGE"

echo "Rollback failed. Attempting to recover previous state..." >&2

if ! compose "$TARGET_IMAGE" "$TARGET_PROFILE" "$TEMP_ENV" stop backend; then
    record_event "RECOVERY_REFUSED" "$TARGET_IMAGE" "$CURRENT_IMAGE"
    echo "Unable to stop backend. Manual intervention required." >&2
    exit 1
fi

if ! NEW_HISTORY=$(migration_history); then
    record_event "RECOVERY_REFUSED" "$TARGET_IMAGE" "$CURRENT_IMAGE"
    echo "Unable to verify database state." >&2
    exit 1
fi

if [[ "$NEW_HISTORY" != "$CURRENT_HISTORY" ]]; then
    record_event "RECOVERY_REFUSED" "$TARGET_IMAGE" "$CURRENT_IMAGE"
    echo "Database migrations changed. Recovery refused." >&2
    exit 1
fi

if compose "$CURRENT_IMAGE" "$CURRENT_PROFILE" .env up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend && [[ "$(docker inspect --format '{{.Image}}' "$BACKEND")" == "$CURRENT_ID" ]] && smoke_test; then
    record_event "RECOVERY_SUCCESS" "$TARGET_IMAGE" "$CURRENT_IMAGE"
    echo "Previous production state recovered." >&2
else
    record_event "RECOVERY_FAILED" "$TARGET_IMAGE" "$CURRENT_IMAGE"
    echo "Recovery failed. Manual intervention required." >&2
fi

exit 1
