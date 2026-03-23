# Claude Code for the Web — Environment Setup

Instructions for running MockHub in Claude Code on the Web (cloud sessions).

## Environment Variables (Persistent Secrets)

Set these in the **environment configuration UI** on claude.ai/code. They persist across all sessions using that environment.

### Setup Steps

1. Go to [claude.ai/code](https://claude.ai/code)
2. Click the current environment name to open the environment selector
3. Click the **settings button** (gear icon) next to the environment name
4. Add environment variables in `.env` format:

```
GH_TOKEN=github_pat_xxxx...
ANTHROPIC_API_KEY=sk-ant-xxxx...
```

5. Save

### Creating the GitHub PAT (Fine-Grained)

1. Go to [github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta)
2. Click **Generate new token**
3. Configure:
   - **Name:** `claude-code-web-mockhub`
   - **Expiration:** 90 days
   - **Repository access:** "Only select repositories" → `kousen/mockhub`
   - **Permissions** (Repository permissions):
     - Issues: Read and write
     - Pull requests: Read and write
     - Contents: Read-only
4. Click **Generate token** and copy the `github_pat_...` value
5. Add it as `GH_TOKEN` in the environment config (step 4 above)

### Why GH_TOKEN?

- `git` operations (push, pull, clone) already work — Claude Code Web uses a dedicated proxy with scoped credentials
- `gh` CLI operations (issues, PRs) go through the GitHub REST API and need a separate token
- `gh` automatically recognizes the `GH_TOKEN` environment variable — no `gh auth login` needed

## SessionStart Hook (Runtime Setup)

The hook at `.claude/hooks/session-start.sh` handles runtime environment setup. It runs after Claude Code launches, on every new session (matcher: `startup`).

### What it does

1. Configures apt proxy (container routes traffic through egress proxy)
2. Installs Java 25 and `gh` CLI if not present
3. Exports `JAVA_HOME` and `PATH` to `CLAUDE_ENV_FILE` (runtime-computed vars)
4. Runs `npm ci` in frontend/
5. Pre-warms Gradle dependencies in backend/

### CLAUDE_ENV_FILE vs Environment UI

Two separate mechanisms for two different purposes:

| Mechanism | Purpose | Example | Persists? |
|---|---|---|---|
| **Environment UI** | Secrets and config | `GH_TOKEN`, `ANTHROPIC_API_KEY` | Across all sessions |
| **`CLAUDE_ENV_FILE`** | Runtime-computed vars discovered by hooks | `JAVA_HOME`, `PATH` | Within one session |

You would never put a PAT in `CLAUDE_ENV_FILE` (it's written by a committed script). You would never put `JAVA_HOME` in the environment UI (it depends on what gets installed at runtime).

## Setup Script vs SessionStart Hook

The environment UI also has a **Setup script** field. Here's when to use which:

| | Setup scripts | SessionStart hooks |
|---|---|---|
| Attached to | The cloud environment (UI) | Your repository (`.claude/settings.json`) |
| Runs | Before Claude Code launches, new sessions only | After Claude Code launches, every session |
| Scope | Cloud environments only | Both local and cloud |
| Use for | Installing system packages the cloud lacks | Project-level setup (npm ci, Gradle deps) |

MockHub uses the SessionStart hook for everything because:
- The hook guards with `CLAUDE_CODE_REMOTE` check (skips on local)
- Java 25 installation and gh install are idempotent (check before installing)
- npm ci and Gradle deps should run in both environments

An alternative: move `apt-get install` commands to a Setup script (runs once, before Claude Code) and keep `npm ci` / Gradle in the SessionStart hook (project-level).

## Network Access

The default "Limited" network access already allows:
- `github.com`, `api.github.com` — for `gh` CLI
- `registry.npmjs.org` — for npm
- `repo.maven.org`, `services.gradle.org`, `plugins.gradle.org` — for Gradle/Maven
- `spring.io`, `repo.spring.io` — for Spring dependencies

No need to change from "Limited" unless you need access to additional domains.
