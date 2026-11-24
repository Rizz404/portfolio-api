
#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

readonly BACKUP_ROOT="/var/backups/portfolio-api"
readonly DEPLOY_DIR="/opt/portfolio-api"
readonly RETENTION_DAYS=30
readonly KEEP_MINIMUM=5

cd "$DEPLOY_DIR"

exec 9>.deploy.lock
flock -x 9

CURRENT_REF=$(sed -n 's/^BACKEND_IMAGE=//p' .env)
CURRENT_ID=$(docker inspect --format '{{.Image}}' company-web-backend-backend-1)

mapfile -t snapshots < <(
    find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -t- -k2,2r
)

valid=0
eligible=0

for name in "${snapshots[@]}"; do
    [[ "$name" =~ ^(deploy|rollback)-[0-9]{8}T[0-9]{6}Z-[a-zA-Z0-9]{6}$ ]] || continue

    dir="$BACKUP_ROOT/$name"

    if ! (cd "$dir" && sha256sum -c --status SHA256SUMS); then
        echo "INVALID: $name"
        continue
    fi

    image=$(cat "$dir/rollback-image.txt")
    expected_id=$(cat "$dir/image-id.txt")

    if [[ "$image" != "portfolio-api-rollback:$name" ]] || ! actual_id=$(docker image inspect --format '{{.Id}}' "$image") || [[ "$actual_id" != "$expected_id" ]]; then
        echo "INVALID IMAGE: $name"
        continue
    fi

    valid=$((valid + 1))

    if [[ "$valid" -le "$KEEP_MINIMUM" ]]; then
        echo "KEEP (minimum snapshots): $name"
        continue
    fi

    if [[ "$image" == "$CURRENT_REF" || "$actual_id" == "$CURRENT_ID" ]]; then
        echo "KEEP (active image): $name"
        continue
    fi

    if [[ -n "$(find "$dir" -maxdepth 0 -mtime "+$RETENTION_DAYS" -print)" ]]; then
        echo "CANDIDATE FOR REVIEW: $name"
        eligible=$((eligible + 1))
    else
        echo "KEEP (retention period): $name"
    fi
done

echo
echo "Valid snapshots: $valid"
echo "Cleanup candidates: $eligible"
echo "Dry-run completed. Nothing was deleted."
