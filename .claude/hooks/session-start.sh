#!/bin/bash
set -euo pipefail

# Only run in Claude Code on the web
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-/home/user/mockhub}"

# ──────────────────────────────────────────────
# 1. Configure apt proxy (container routes traffic through an egress proxy)
# ──────────────────────────────────────────────
if [ -n "${GLOBAL_AGENT_HTTP_PROXY:-}" ] && [ ! -f /etc/apt/apt.conf.d/99proxy ]; then
  echo "Acquire::http::Proxy \"$GLOBAL_AGENT_HTTP_PROXY\";" | sudo tee /etc/apt/apt.conf.d/99proxy > /dev/null
  echo "Acquire::https::Proxy \"$GLOBAL_AGENT_HTTP_PROXY\";" | sudo tee -a /etc/apt/apt.conf.d/99proxy > /dev/null
fi

# ──────────────────────────────────────────────
# 2. Install system packages (Java 25, GitHub CLI)
#    The container image caches apt package lists,
#    so we install directly without apt-get update.
# ──────────────────────────────────────────────
APT_PACKAGES=""

if [ ! -d /usr/lib/jvm/java-25-openjdk-amd64 ]; then
  APT_PACKAGES="openjdk-25-jdk-headless"
fi

if ! command -v gh &> /dev/null; then
  APT_PACKAGES="$APT_PACKAGES gh"
fi

if [ -n "$APT_PACKAGES" ]; then
  echo "Installing: $APT_PACKAGES"
  # shellcheck disable=SC2086
  if ! sudo apt-get install -y -qq $APT_PACKAGES 2>/dev/null; then
    echo "Cached package lists stale — updating..."
    sudo apt-get update -qq 2>/dev/null || true
    # shellcheck disable=SC2086
    sudo apt-get install -y -qq $APT_PACKAGES
  fi
fi

# Set Java 25 as default
if [ -d /usr/lib/jvm/java-25-openjdk-amd64 ]; then
  sudo update-java-alternatives -s java-1.25.0-openjdk-amd64 2>/dev/null || true
  echo "export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64" >> "$CLAUDE_ENV_FILE"
  echo "export PATH=/usr/lib/jvm/java-25-openjdk-amd64/bin:\$PATH" >> "$CLAUDE_ENV_FILE"
  export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
  export PATH=/usr/lib/jvm/java-25-openjdk-amd64/bin:$PATH
fi

echo "Java: $(java -version 2>&1 | grep -oP 'openjdk version \"\K[^"]+')"
command -v gh &> /dev/null && echo "gh: $(gh --version 2>&1 | head -1)"

# ──────────────────────────────────────────────
# 3. Frontend dependencies
# ──────────────────────────────────────────────
if [ -d "$PROJECT_DIR/frontend" ]; then
  echo "Installing frontend dependencies..."
  cd "$PROJECT_DIR/frontend"
  npm ci --prefer-offline --no-audit --no-fund 2>&1 | tail -1
fi

# ──────────────────────────────────────────────
# 4. Pre-warm Gradle dependencies (uses wrapper in backend/)
# ──────────────────────────────────────────────
if [ -x "$PROJECT_DIR/backend/gradlew" ]; then
  echo "Resolving Gradle dependencies..."
  cd "$PROJECT_DIR/backend"
  ./gradlew dependencies --quiet > /dev/null 2>&1 || true
fi

echo "Session startup complete."
