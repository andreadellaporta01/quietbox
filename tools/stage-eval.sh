#!/usr/bin/env bash
# The on-stage real-model eval: just the scoreboard, nothing else on screen.
set -euo pipefail
cd "$(dirname "$0")/.."
QUIETBOX_PROXY_URL=http://localhost:8787 QUIETBOX_TOKEN=stage \
  ./gradlew -q :core:eval -Pengine=proxy 2>/dev/null | sed -n '/── QuietBox eval/,$p'
