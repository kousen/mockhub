#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"

step() {
  printf '\n==> %s\n' "$1"
}

run() {
  local dir="$1"
  shift
  step "$*"
  (cd "$dir" && "$@")
}

step "Repository status"
git -C "$ROOT" status --short

run "$ROOT/backend" ./gradlew test jacocoTestReport
run "$ROOT/frontend" npm run format:check
run "$ROOT/frontend" npm run lint
run "$ROOT/frontend" npm run typecheck
run "$ROOT/frontend" npm run test -- --coverage

if [[ "${PRE_PUSH_E2E:-}" == "1" ]]; then
  run "$ROOT/frontend" npm run test:e2e
fi

step "Pre-push gate passed"
