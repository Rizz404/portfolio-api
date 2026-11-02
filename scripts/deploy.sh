#!/usr/bin/env bash
# * Dijalankan DI SERVER (bukan lokal): sinkron compose dari repo, pull image dari Docker Hub, lalu up.
# * Pemakaian: scripts/deploy.sh <tag>   -- tag = short SHA dari CI (wajib, biar versi selalu eksplisit)
# * Rollback: jalankan lagi dengan tag SHA sebelumnya.
set -euo pipefail
cd "$(dirname "$0")/.."

TAG="${1:?Tag wajib diisi, contoh: scripts/deploy.sh b90173c}"
# * Tag dikirim dari luar (CI via SSH), jadi cuma terima karakter aman biar gak bisa disusupi command lain.
if ! [[ "${TAG}" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Tag tidak valid: ${TAG}" >&2
  exit 2
fi
IMAGE="docker.io/rizz404/portfolio-api:${TAG}"

git pull --ff-only

# * Pull dulu dengan image yang diminta; .env baru diubah kalau pull sukses, jadi tag yang salah
# * gak nyisa di .env.
BACKEND_IMAGE="${IMAGE}" docker compose pull backend

# * Satu-satunya nilai yang script ini ubah di .env adalah BACKEND_IMAGE.
if grep -q '^BACKEND_IMAGE=' .env; then
  sed -i "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=${IMAGE}|" .env
else
  printf '\nBACKEND_IMAGE=%s\n' "${IMAGE}" >> .env
fi

docker compose up -d --no-build --remove-orphans

# * Tunggu healthcheck backend (Dockerfile HEALTHCHECK) sampai healthy, maksimal ~3 menit.
cid="$(docker compose ps -q backend)"
for _ in $(seq 1 36); do
  status="$(docker inspect -f '{{.State.Health.Status}}' "${cid}")"
  if [ "${status}" = "healthy" ]; then
    echo "OK: ${IMAGE} healthy"
    exit 0
  fi
  sleep 5
done

echo "GAGAL: backend tidak healthy. Rollback: scripts/deploy.sh <tag-sebelumnya>" >&2
docker compose logs --tail=50 backend >&2
exit 1
