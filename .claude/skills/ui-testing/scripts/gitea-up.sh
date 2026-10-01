#!/usr/bin/env bash
# gitea-up.sh [VERSION|IMAGE] : (re)creates the "gitea" container on http://localhost:3000 with an
# admin (root / Passw0rd!) and builds the acme/webapp fixture (fixture.sh). Destroys any previous data.
# VERSION (default 1.27.3) is pulled from Gitea's registry, docker.gitea.com, falling back to Docker
# Hub; a full IMAGE name (one with a "/") is used as given.
set -euo pipefail
D=$(cd "$(dirname "$0")" && pwd)
STATE=${GITEA_UI_STATE:-$HOME/.gitea-ui}
V=${1:-1.27.3}
if [[ $V == */* ]]; then CANDIDATES=("$V"); else CANDIDATES=("docker.gitea.com/gitea:$V" "gitea/gitea:$V"); fi
IMG=
for c in "${CANDIDATES[@]}"; do
  if docker pull -q "$c" >/dev/null; then IMG=$c; break; fi
  echo "gitea-up: pulling $c failed" >&2
done
if [[ -z $IMG ]]; then # offline or rate-limited: an image pulled earlier still does
  for c in "${CANDIDATES[@]}"; do docker image inspect "$c" >/dev/null 2>&1 && { IMG=$c; break; }; done
fi
[[ -n $IMG ]] || { echo "gitea-up: no image for $V" >&2; exit 1; }
echo "gitea-up: using $IMG"
docker rm -f gitea >/dev/null 2>&1 || true
docker run -d --name gitea -p 3000:3000 -e GITEA__security__INSTALL_LOCK=true -e GITEA__database__DB_TYPE=sqlite3 \
  -e GITEA__server__ROOT_URL=http://localhost:3000/ "$IMG" >/dev/null
for i in $(seq 1 60); do curl -sf localhost:3000/api/v1/version && break; sleep 1; done; echo
docker exec -u git gitea gitea admin user create --admin --username root --password 'Passw0rd!' \
  --email root@example.com --must-change-password=false | tail -1
docker cp "$STATE/bin/tea" gitea:/usr/local/bin/tea
"$D/fixture.sh"
