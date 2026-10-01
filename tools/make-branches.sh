#!/usr/bin/env bash
# Builds the attendee branches from the solved `main`:
#   start       every lab stubbed           (attendees clone this)
#   lab-1-done  lab 1 solved, 2-4 stubbed   (catch-up checkpoints)
#   lab-2-done  ...
#   lab-3-done  ...
#   solved      == main
set -euo pipefail
cd "$(dirname "$0")/.."
git diff --quiet || { echo "commit or stash first"; exit 1; }

checkpoint() {
  local branch=$1; shift
  git checkout -q -B "$branch" main
  python3 tools/make-starter.py . "$@" >/dev/null
  git commit -qam "workshop: $branch"
  echo "built $branch"
}

checkpoint start      LAB-1 LAB-2 LAB-3 LAB-4
checkpoint lab-1-done       LAB-2 LAB-3 LAB-4
checkpoint lab-2-done             LAB-3 LAB-4
checkpoint lab-3-done                   LAB-4
git branch -f solved main
git checkout -q main
