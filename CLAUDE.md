# MockHub — Project Instructions for Claude Code

@AGENTS.md

All project instructions live in `AGENTS.md`, the canonical instruction file
shared by every AI coding agent on this project. The `@AGENTS.md` import above
loads it into every Claude Code session — edit instructions there, not here.

Claude Code specifics (hooks, skills, subagents, permissions) live in
`.claude/`:

- `hooks/prevent-flyway-migration-edit.sh` — blocks edits to committed Flyway migrations (append-only rule, enforced)
- `hooks/session-start.sh` — cloud (Claude Code on the web) environment setup; no-op locally
- `skills/pre-push-gate/` — local verification gate before pushing
- `skills/add-mcp-tool/` — checklist for adding or modifying MCP tools
- `agents/` — read-only reviewers for docs, security, and test coverage
