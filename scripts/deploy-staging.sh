
#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
unset DOCKER_HOST DOCKER_CONTEXT DOCKER_CONFIG

readonly DEPLOY_DIR="/opt/portfolio-api-staging"
readonly IMAGE_REPO="docker.io/rizz404/portfolio-api"
readonly PROJECT="portfolio-api-staging"
readonly BACKEND="portfolio-api-staging-backend-1"
readonly DB="portfolio-api-staging-db-1"

SIMULATE_FAILURE=false

if [[ "${1:-}" == "--simulate-failure" ]]; then
    SIMULATE_FAILURE=true
    shift
fi

if [[ $# -ne 1 || ! "${1:-}" =~ ^sha256:[a-f0-9]{64}$ ]]; then
    echo "Usage: $0 [--simulate-failure] sha256:<digest>" >&2
    exit 1
fi

IMAGE="${IMAGE_REPO}@$1"

cd "$DEPLOY_DIR"

# Prevent concurrent deployments.
exec 9>.deploy.lock
flock -x 9

compose() {
    local image="$1"
    local env_file="$2"
    shift 2

    env -i PATH="$PATH" HOME=/root STAGING_BACKEND_IMAGE="$image" docker compose -p "$PROJECT" --env-file "$env_file" -f compose.staging.yaml "$@"
}

migration_history() {
    docker exec "$DB" sh -c 'exec psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -At -c "SELECT installed_rank, version, description, type, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank;"'
}

smoke_test() {
    curl --noproxy '*' -fsS --max-time 15 http://127.0.0.1:18001/api/v1/actuator/health |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' || return 1

    curl --noproxy '*' -fsS --max-time 15 http://127.0.0.1:18001/api/v1/projects |
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "success")' || return 1
}

record_event() {
    printf '%s\t%s\t%s\t%s\n' "$(date -u +%FT%TZ)" "$1" "$2" "$3" >> release-history.tsv
}

# Identify the Last Known Good image.
if [[ $(grep -c '^STAGING_BACKEND_IMAGE=' .env.staging) -ne 1 ]]; then
    echo "Invalid staging environment configuration." >&2
    exit 1
fi

OLD_IMAGE=$(sed -n 's/^STAGING_BACKEND_IMAGE=//p' .env.staging)
OLD_ID=$(docker inspect --format '{{.Image}}' "$BACKEND")
EXPECTED_OLD_ID=$(docker image inspect --format '{{.Id}}' "$OLD_IMAGE")

if [[ "$OLD_ID" != "$EXPECTED_OLD_ID" ]]; then
    echo "Running image does not match Last Known Good." >&2
    exit 1
fi

echo "Validating current staging baseline..."
smoke_test

OLD_HISTORY=$(migration_history)

if [[ -z "$OLD_HISTORY" ]] || grep -q '|f$' <<< "$OLD_HISTORY"; then
    echo "Invalid database migration history." >&2
    exit 1
fi

if [[ "$SIMULATE_FAILURE" == true && "$IMAGE" == "$OLD_IMAGE" ]]; then
    echo "Failure simulation requires a different image." >&2
    exit 1
fi

echo "Last Known Good: $OLD_IMAGE"
echo "Candidate: $IMAGE"

# Prepare candidate configuration.
TEMP_ENV=$(mktemp .env.staging.XXXXXX)
trap 'rm -f -- "$TEMP_ENV"' EXIT

sed "s|^STAGING_BACKEND_IMAGE=.*|STAGING_BACKEND_IMAGE=$IMAGE|" .env.staging > "$TEMP_ENV"

compose "$IMAGE" "$TEMP_ENV" config --quiet

echo "Pulling candidate image..."
compose "$IMAGE" "$TEMP_ENV" pull backend

CANDIDATE_ID=$(docker image inspect --format '{{.Id}}' "$IMAGE")

record_event "DEPLOY_STARTED" "$OLD_IMAGE" "$IMAGE"

deploy_candidate() {
    compose "$IMAGE" "$TEMP_ENV" up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend || return 1

    [[ "$(docker inspect --format '{{.Image}}' "$BACKEND")" == "$CANDIDATE_ID" ]] || return 1

    smoke_test || return 1

    if [[ "$SIMULATE_FAILURE" == true ]]; then
        echo "Simulating deployment failure." >&2
        return 1
    fi
}

echo "Deploying candidate..."

if deploy_candidate && mv -f "$TEMP_ENV" .env.staging; then
    record_event "DEPLOY_SUCCESS" "$OLD_IMAGE" "$IMAGE" || echo "Warning: unable to write release history." >&2
    echo "Staging deployment successful."
    echo "New Last Known Good: $IMAGE"
    exit 0
fi

echo "Deployment failed. Preparing automatic rollback..." >&2
record_event "DEPLOY_FAILED" "$OLD_IMAGE" "$IMAGE" || true

if ! compose "$IMAGE" "$TEMP_ENV" stop backend; then
    echo "Unable to stop failed backend. Manual intervention required." >&2
    exit 1
fi

if ! CURRENT_HISTORY=$(migration_history); then
    echo "Unable to verify database state. Rollback refused." >&2
    exit 1
fi

if [[ "$CURRENT_HISTORY" != "$OLD_HISTORY" ]]; then
    echo "Database migrations changed. Automatic rollback refused." >&2
    record_event "ROLLBACK_REFUSED" "$IMAGE" "$OLD_IMAGE" || true
    exit 1
fi

echo "Restoring Last Known Good: $OLD_IMAGE"

if compose "$OLD_IMAGE" .env.staging up -d --no-deps --no-build --pull never --wait --wait-timeout 180 backend && [[ "$(docker inspect --format '{{.Image}}' "$BACKEND")" == "$OLD_ID" ]] && smoke_test; then
    record_event "ROLLBACK_SUCCESS" "$IMAGE" "$OLD_IMAGE" || true
    echo "Automatic rollback successful." >&2
    echo "Restored image: $OLD_IMAGE"
else
    record_event "ROLLBACK_FAILED" "$IMAGE" "$OLD_IMAGE" || true
    echo "Automatic rollback failed. Manual intervention required." >&2
fi

# Deployment still failed, even if rollback succeeded.
exit 1
