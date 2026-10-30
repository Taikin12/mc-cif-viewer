#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${MC_MAIN_HOST:-mc}"
CONTAINER="${MC_MAIN_CONTAINER:-mc-backend}"
REMOTE_DIR="${MC_MAIN_DIR:-/opt/mc/servers/main/plugins}"
SERVER_PROPERTIES="${MC_MAIN_PROPERTIES:-/opt/mc/servers/main/server.properties}"
GRADLE_CACHE="${GRADLE_CACHE:-/tmp/opencode/mc-gradle-cache}"
PLUGIN="${MC_MAIN_PLUGIN:-CifViewer}"
BUILD=1
RELOAD=1

usage() {
  cat <<'USAGE'
Usage: scripts/deploy-main.sh [--no-build] [--restart]

Builds mc-cif-viewer and deploys it to the main server on 192.168.100.13.
By default the plugin is reloaded with PlugManX (no server restart).
Use --restart when server.properties or the resource pack changed.
The test server is the failover server and is not touched.
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --no-build) BUILD=0 ;;
    --restart) RELOAD=0 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown option: $arg" >&2; usage >&2; exit 2 ;;
  esac
done

cd "$ROOT"

if [ "$BUILD" -eq 1 ]; then
  docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp \
    -e GRADLE_USER_HOME=/gradle-cache \
    -v "$GRADLE_CACHE:/gradle-cache" \
    -v "$ROOT:/workspace" -w /workspace \
    gradle:9.8.0-jdk25 ./gradlew shadowJar --console=plain
fi

jar="$(find build/libs -maxdepth 1 -name 'mc-cif-viewer-*.jar' ! -name '*-plain.jar' | sort | tail -1)"
if [ -z "$jar" ]; then
  echo "no shaded jar found; build first" >&2
  exit 1
fi

ssh "$HOST" "mkdir -p '$REMOTE_DIR/CifViewer/cif'"
ssh "$HOST" "rm -f '$REMOTE_DIR'/mc-cif-viewer-*.jar '$REMOTE_DIR'/cif-viewer-*.jar"
scp -q "$jar" "$HOST:$REMOTE_DIR/"
scp -q samples/*.cif "$HOST:$REMOTE_DIR/CifViewer/cif/"
ssh "$HOST" "chown -R 999:991 '$REMOTE_DIR/CifViewer' 2>/dev/null || true"

if [ "$RELOAD" -eq 0 ]; then
  RESOURCEPACK_ROOT="${MC_RESOURCEPACK_ROOT:-$ROOT/..}"
  if [ -f "$RESOURCEPACK_ROOT/resourcepack/build_pack.py" ]; then
    python3 "$RESOURCEPACK_ROOT/resourcepack/build_pack.py"
    bash "$RESOURCEPACK_ROOT/scripts/deploy-resourcepack.sh" --no-build
  fi
fi

if [ "$RELOAD" -eq 1 ]; then
  echo "reloading $PLUGIN on $HOST via PlugManX ..."
  started="$(ssh "$HOST" "date -u +%Y-%m-%dT%H:%M:%SZ")"
  if ! \
      ssh "$HOST" "MC_RCON_PROPERTIES='$SERVER_PROPERTIES' python3 - plugman reload $PLUGIN" \
      < scripts/rcon.py; then
    echo "reload failed; use --restart" >&2
    exit 1
  fi
  sleep 1
  echo "--- $CONTAINER (reload) ---"
  ssh "$HOST" "docker logs --since '$started' '$CONTAINER' 2>&1 | sed -e 's/\x1b\[[0-9;]*m//g' | grep -E '$PLUGIN|PlugMan|ERROR|Could not load|Failed to' | tail -30"
  exit 0
fi

ssh "$HOST" "docker restart '$CONTAINER' >/dev/null"
echo "restarting $CONTAINER on $HOST ..."

started="$(ssh "$HOST" "docker inspect --format '{{.State.StartedAt}}' '$CONTAINER'")"
for _ in $(seq 1 90); do
  if ssh "$HOST" "docker logs --since '$started' '$CONTAINER' 2>&1 | grep -q 'Done ('"; then
    break
  fi
  sleep 2
done

echo "--- $CONTAINER ---"
ssh "$HOST" "docker logs --since '$started' '$CONTAINER' 2>&1 | sed -e 's/\x1b\[[0-9;]*m//g' | grep -E 'Done \(|ERROR|Could not load|Enabling |Failed to' | tail -40"
