#!/usr/bin/env bash
set -euo pipefail

input=$(cat)

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
file_path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')

if [[ -z "$file_path" ]]; then
  exit 0
fi

case "$file_path" in
  "$project_dir"/*) rel_path="${file_path#"$project_dir"/}" ;;
  /*) rel_path="$file_path" ;;
  *) rel_path="$file_path" ;;
esac

case "$rel_path" in
  backend/src/main/resources/db/migration/V*.sql) ;;
  *) exit 0 ;;
esac

if git -C "$project_dir" ls-files --error-unmatch "$rel_path" >/dev/null 2>&1; then
  message="Refusing to edit tracked Flyway migration '$rel_path'. Flyway migrations are append-only after they enter git history. Create a new V{next}__description.sql migration instead."
  jq -n --arg message "$message" '{
    hookSpecificOutput: { permissionDecision: "deny" },
    systemMessage: $message
  }' >&2
  exit 2
fi

exit 0
