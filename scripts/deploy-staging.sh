
#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly DEPLOY_DIR="/opt/portfolio-api-staging"
readonly IMAGE_REPO="docker.io/rizz404/portfolio-api"

# Accept only a valid SHA-256 image digest.
if [[ $# -ne 1 || ! "${1:-}" =~ ^sha256:[a-f0-9]{64}$ ]]; then
    echo "Usage: $0 sha256:<64-character-digest>" >&2
    exit 1
fi

IMAGE="${IMAGE_REPO}@$1"

cd "$DEPLOY_DIR"

# Prevent simultaneous deployments.
exec 9>.deploy.lock
flock -x 9

# Prevent inherited variables from overriding staging configuration.
unset STAGING_BACKEND_IMAGE \
      STAGING_DB_PASSWORD \
      STAGING_REDIS_PASSWORD \
      STAGING_JWT_SECRET \
      STAGING_CLOUDINARY_CLOUD_NAME \
      STAGING_CLOUDINARY_API_KEY \
      STAGING_CLOUDINARY_API_SECRET

# Prepare a temporary environment file.
TEMP_ENV=$(mktemp .env.staging.XXXXXX)
trap 'rm -f "$TEMP_ENV"' EXIT

if [[ $(grep -c '^STAGING_BACKEND_IMAGE=' .env.staging) -ne 1 ]]; then
    echo "Invalid staging environment configuration" >&2
    exit 1
fi

sed "s|^STAGING_BACKEND_IMAGE=.*|STAGING_BACKEND_IMAGE=$IMAGE|" \
    .env.staging > "$TEMP_ENV"

COMPOSE=(
    docker compose
    -p portfolio-api-staging
    --env-file "$TEMP_ENV"
    -f compose.staging.yaml
)

echo "Validating configuration..."
"${COMPOSE[@]}" config --quiet

echo "Pulling image: $IMAGE"
"${COMPOSE[@]}" pull backend

echo "Deploying staging..."
"${COMPOSE[@]}" up -d --no-build --pull never \
    --wait --wait-timeout 180

echo "Running health checks..."

curl -fsS --retry 5 --retry-connrefused \
    --retry-delay 3 --max-time 15 \
    http://127.0.0.1:18001/api/v1/actuator/health |
    python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")'

echo "Running API smoke test..."

curl -fsS --max-time 15 \
    http://127.0.0.1:18001/api/v1/projects |
    python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "success")'

# Persist the image reference only after validation succeeds.
mv -f "$TEMP_ENV" .env.staging

echo "Staging deployment successful."
echo "Deployed image: $IMAGE"
