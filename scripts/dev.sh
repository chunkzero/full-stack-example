#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# The pack download endpoint is shared by every gameplay JVM.
python3 -m http.server 8080 --bind 127.0.0.1 --directory assets &
pack_server=$!
trap 'kill "$pack_server" 2>/dev/null || true; wait "$pack_server" 2>/dev/null || true' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
sleep 0.2
kill -0 "$pack_server"
chunk dev "$@"
