#!/usr/bin/env bash
# gitea-up.sh [IMAGE] : (re)creates the "gitea" container on http://localhost:3000 with an admin
# (root / Passw0rd!) and builds the acme/webapp fixture (fixture.sh). Destroys any previous data.
set -euo pipefail
D=$(cd "$(dirname "$0")" && pwd)
STATE=${GITEA_UI_STATE:-$HOME/.gitea-ui}
IMG=${1:-gitea/gitea:1.27.3}
docker rm -f gitea >/dev/null 2>&1 || true
docker run -d --name gitea -p 3000:3000 -e GITEA__security__INSTALL_LOCK=true -e GITEA__database__DB_TYPE=sqlite3 \
  -e GITEA__server__ROOT_URL=http://localhost:3000/ "$IMG" >/dev/null
for i in $(seq 1 60); do curl -sf localhost:3000/api/v1/version && break; sleep 1; done; echo
docker exec -u git gitea gitea admin user create --admin --username root --password 'Passw0rd!' \
  --email root@example.com --must-change-password=false | tail -1
docker cp "$STATE/bin/tea" gitea:/usr/local/bin/tea
"$D/fixture.sh"
