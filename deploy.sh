#!/usr/bin/env bash
#
# Apply code changes to the native (non-Docker) local BookLore install set up
# by install.sh: pulls latest code, reinstalls frontend deps if needed, and
# restarts the booklore-api / booklore-ui systemd services. A fresh
# gradlew bootRun recompiles the backend and lets Flyway apply any new
# migrations against Postgres on boot.
#
# Usage:
#   ./deploy.sh              # git pull, then restart
#   ./deploy.sh --skip-pull  # restart only, e.g. to deploy uncommitted edits
#

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIP_PULL=false
[ "${1:-}" = "--skip-pull" ] && SKIP_PULL=true

log() { echo ">> $*"; }

cd "$REPO_DIR"

LOCK_CHANGED=false
if [ "$SKIP_PULL" = false ]; then
  log "Pulling latest changes..."
  BEFORE_LOCK="$(git rev-parse HEAD:booklore-ui/package-lock.json 2>/dev/null || true)"
  git pull --ff-only
  AFTER_LOCK="$(git rev-parse HEAD:booklore-ui/package-lock.json 2>/dev/null || true)"
  [ "$BEFORE_LOCK" != "$AFTER_LOCK" ] && LOCK_CHANGED=true
else
  log "Skipping git pull (--skip-pull)."
  git diff --quiet HEAD -- booklore-ui/package-lock.json || LOCK_CHANGED=true
fi

if [ "$LOCK_CHANGED" = true ]; then
  log "Frontend dependencies changed, running npm install..."
  (cd "$REPO_DIR/booklore-ui" && npm install)
else
  log "No frontend dependency changes, skipping npm install."
fi

log "Restarting services..."
sudo systemctl restart booklore-api booklore-ui

log "Waiting for backend to come up..."
for _ in $(seq 1 30); do
  if curl -fs http://localhost:6060/api/v1/healthcheck > /dev/null 2>&1; then
    log "Backend healthy."
    break
  fi
  sleep 2
done

echo
echo "--- booklore-api (last 20 lines) ---"
journalctl -u booklore-api -n 20 --no-pager
echo
echo "--- booklore-ui (last 10 lines) ---"
journalctl -u booklore-ui -n 10 --no-pager
echo
log "Done. Frontend: http://localhost:4200  Backend: http://localhost:6060"
