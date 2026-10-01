#!/usr/bin/env bash
# Starts the proxy for the presenter's real-model demo, silently, in the background.
# Reads the key from the environment the shell already has; never prints it.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${ANTHROPIC_API_KEY:-}${ANTHROPIC_AUTH_TOKEN:-}" ]; then
  echo "no Anthropic credentials in this shell" >&2
  exit 1
fi
pkill -f dev.quietbox.proxy.ProxyKt 2>/dev/null || true
QUIETBOX_TOKEN=stage nohup ./gradlew -q :proxy:run > /tmp/quietbox-proxy.log 2>&1 &
for _ in $(seq 1 60); do
  curl -sf localhost:8787/health >/dev/null && { echo "proxy ready on :8787"; exit 0; }
  sleep 2
done
echo "proxy did not start, see /tmp/quietbox-proxy.log" >&2
exit 1
