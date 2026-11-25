
#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
unset DOCKER_HOST DOCKER_CONTEXT DOCKER_CONFIG

readonly DEPLOY_DIR="/opt/portfolio-api"
readonly STAGING_DIR="/opt/portfolio-api-staging"
readonly BACKUP_ROOT="/var/backups/portfolio-api"
readonly IMAGE_REPO="docker.io/rizz404/portfolio-api"
readonly PROJECT="company-web-backend"
readonly PROD_BACKEND="company-web-backend-backend-1"
readonly PROD_DB="company-web-backend-db-1"
readonly STAGING_BACKEND="portfolio-api-staging-backend-1"
readonly STAGING_DB="portfolio-api-staging-db-1"

CHECK_ONLY=false

if [[ "${1:-}" == "--check" ]]; then
    CHECK_ONLY=true
    shift
fi

if [[ $# -ne 1 || ! "${1:-}" =~ ^sha256:[a-f0-9]{64}$ ]]; then
    echo "Usage: $0 [--check] sha256:<digest>" >&2
    exit 1
fi

IMAGE="${IMAGE_REPO}@$1"

cd "$DEPLOY_DIR"

# Prevent concurrent production and staging deployments.
exec 9>.deploy.lock
flock -x 9

exec 8>"$STAGING_DIR/.deploy.lock"
flock -x 8

compose() {
    local image="$1"
    local env_file="$2"
    local override="$3"
    shift 3

    env -i PATH="$PATH" HOME=/root BACKEND_IMAGE="$image" PRODUCTION_SPRING_PROFILE=prod docker compose -p "$PROJECT" --env-file "$env_file" -f docker-compose.yml -f "$override" -f compose.monitoring.yaml "$@"
}

migration_history() {
    docker exec "$1" sh -c 'exec psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -At -c "SELECT installed_rank, version, description, type, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank;"'
}

smoke_test() {
    local port="$1"

    curl --noproxy '*' -fsS --max-time 15 "http://127.0.0.1:$port/api/v1/actuator/health" |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' || return 1

    curl --noproxy '*' -fsS --max-time 15 "http://127.0.0.1:$port/api/v1/projects" |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "success")' || return 1
}

record_event() {
    if ! printf '%s\t%s\t%s\t%s\t%s\n' "$(date -u +%FT%TZ)" "$1" "$2" "$3" "$(basename "$BACKUP_DIR")" >> "$DEPLOY_DIR/release-history.tsv"; then
        echo "Warning: unable to write release history." >&2
    fi
}

echo "Validating staging image..."

if ! grep -Fxq "STAGING_BACKEND_IMAGE=$IMAGE" "$STAGING_DIR/.env.staging"; then
    echo "Image has not been promoted from the current staging deployment." >&2
    exit 1
fi

CANDIDATE_ID=$(docker image inspect --format '{{.Id}}' "$IMAGE")
STAGING_ID=$(docker inspect --format '{{.Image}}' "$STAGING_BACKEND")

if [[ "$CANDIDATE_ID" != "$STAGING_ID" ]]; then
    echo "Staging image mismatch." >&2
    exit 1
fi

echo "Validating staging health and API..."
smoke_test 18001

echo "Validating production baseline..."
smoke_test 8001

echo "Comparing database migrations..."

PROD_HISTORY=$(migration_history "$PROD_DB")
STAGING_HISTORY=$(migration_history "$STAGING_DB")

if [[ -z "$PROD_HISTORY" || "$PROD_HISTORY" != "$STAGING_HISTORY" ]]; then
    echo "Database migration history mismatch. Deployment refused." >&2
    exit 1
fi

if grep -q '|f$' <<< "$PROD_HISTORY"; then
    echo "Failed database migration detected." >&2
    exit 1
fi

echo "Validating production Compose configuration..."

compose "$IMAGE" .env compose.production.yaml config --quiet

if [[ $(grep -c '^BACKEND_IMAGE=' .env) -ne 1 ]]; then
    echo "Invalid BACKEND_IMAGE configuration." >&2
    exit 1
fi

if [[ "$CHECK_ONLY" == true ]]; then
    echo "Production preflight checks passed."
    echo "Candidate: $IMAGE"
    echo "No production containers were modified."
    exit 0
fi

echo "Preparing production backup..."

install -d -m 700 "$BACKUP_ROOT"

BACKUP_DIR=$(mktemp -d "$BACKUP_ROOT/deploy-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")

OLD_ID=$(docker inspect --format '{{.Image}}' "$PROD_BACKEND")
OLD_REF=$(docker inspect --format '{{.Config.Image}}' "$PROD_BACKEND")

OLD_PROFILE=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$PROD_BACKEND" | sed -n 's/^SPRING_PROFILES_ACTIVE=//p')

ROLLBACK_IMAGE="portfolio-api-rollback:$(basename "$BACKUP_DIR")"

docker image tag "$OLD_ID" "$ROLLBACK_IMAGE"

cp .env "$BACKUP_DIR/.env"
cp docker-compose.yml "$BACKUP_DIR/docker-compose.yml"
cp compose.production.yaml "$BACKUP_DIR/compose.production.yaml"
cp compose.monitoring.yaml "$BACKUP_DIR/compose.monitoring.yaml"

printf '%s\n' "$OLD_ID" > "$BACKUP_DIR/image-id.txt"
printf '%s\n' "$ROLLBACK_IMAGE" > "$BACKUP_DIR/rollback-image.txt"
printf '%s\n' "$PROD_HISTORY" > "$BACKUP_DIR/migration-history.txt"

# Preserve the previous runtime profile for emergency rollback.
python3 -c '
import json, sys
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
' "$OLD_PROFILE" > "$BACKUP_DIR/rollback.compose.json"

echo "Backing up PostgreSQL..."

docker exec "$PROD_DB" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_dump --format=custom --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' > "$BACKUP_DIR/database.dump"

docker exec -i "$PROD_DB" pg_restore --list < "$BACKUP_DIR/database.dump" > "$BACKUP_DIR/database-contents.txt"

(
    cd "$BACKUP_DIR"
    sha256sum .env docker-compose.yml compose.production.yaml compose.monitoring.yaml database.dump rollback.compose.json image-id.txt rollback-image.txt migration-history.txt database-contents.txt > SHA256SUMS
    sha256sum -c SHA256SUMS
)

echo "Backup completed: $BACKUP_DIR"

compose "$ROLLBACK_IMAGE" "$BACKUP_DIR/.env" "$BACKUP_DIR/rollback.compose.json" config --quiet

echo "Pulling approved image..."
docker pull "$IMAGE"

TEMP_ENV=$(mktemp .env.production.XXXXXX)
trap 'rm -f -- "$TEMP_ENV"' EXIT

sed -e "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=$IMAGE|" -e '/^PRODUCTION_SPRING_PROFILE=/d' .env > "$TEMP_ENV"

printf '\nPRODUCTION_SPRING_PROFILE=prod\n' >> "$TEMP_ENV"

deploy_backend() {
    compose "$IMAGE" "$TEMP_ENV" compose.production.yaml up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend || return 1

    [[ "$(docker inspect --format '{{.Image}}' "$PROD_BACKEND")" == "$CANDIDATE_ID" ]] || return 1

    smoke_test 8001 || return 1

    [[ "$(migration_history "$PROD_DB")" == "$PROD_HISTORY" ]] || return 1
}

record_event "DEPLOY_STARTED" "$OLD_REF" "$IMAGE"

echo "Deploying production backend..."

if deploy_backend && mv -f "$TEMP_ENV" .env; then
    record_event "DEPLOY_SUCCESS" "$OLD_REF" "$IMAGE"

    echo "Production deployment successful."
    echo "Deployed image: $IMAGE"
    echo "Backup: $BACKUP_DIR"
    exit 0
fi

record_event "DEPLOY_FAILED" "$OLD_REF" "$IMAGE"

echo "Production deployment failed." >&2
echo "Preparing emergency rollback..." >&2

# Stop the failed backend before checking database state.

# Stop the failed backend before checking database state.
if ! compose "$IMAGE" "$TEMP_ENV" compose.production.yaml stop backend; then
    record_event "ROLLBACK_REFUSED" "$IMAGE" "$ROLLBACK_IMAGE"

    echo "Unable to stop failed backend. Manual intervention required." >&2
    exit 1
fi

if ! CURRENT_HISTORY=$(migration_history "$PROD_DB"); then
    record_event "ROLLBACK_REFUSED" "$IMAGE" "$ROLLBACK_IMAGE"

    echo "Unable to verify database state. Automatic rollback refused." >&2
    exit 1
fi

if [[ "$CURRENT_HISTORY" != "$PROD_HISTORY" ]]; then
    record_event "ROLLBACK_REFUSED" "$IMAGE" "$ROLLBACK_IMAGE"

    echo "Database migrations changed. Automatic rollback refused." >&2
    echo "Manual intervention required. Backup: $BACKUP_DIR" >&2
    exit 1
fi

if compose "$ROLLBACK_IMAGE" "$BACKUP_DIR/.env" "$BACKUP_DIR/rollback.compose.json" up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend && [[ "$(docker inspect --format '{{.Image}}' "$PROD_BACKEND")" == "$OLD_ID" ]] && smoke_test 8001; then
    record_event "ROLLBACK_SUCCESS" "$IMAGE" "$ROLLBACK_IMAGE"

    echo "Emergency rollback successful." >&2
    echo "Restored image: $ROLLBACK_IMAGE" >&2
else
    record_event "ROLLBACK_FAILED" "$IMAGE" "$ROLLBACK_IMAGE"

    echo "Emergency rollback failed. Manual intervention required." >&2
fi

exit 1
