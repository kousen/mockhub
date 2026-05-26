---
name: pre-push-gate
description: "Run MockHub's local pre-push verification gate before publishing changes."
disable-model-invocation: true
---

# Pre-Push Gate

Use before pushing or opening a PR for MockHub.

Run:

```bash
.claude/skills/pre-push-gate/scripts/pre-push-gate.sh
```

The script runs the backend and frontend checks that most often catch late
failures:

- backend tests plus JaCoCo report
- frontend Prettier check
- frontend ESLint
- frontend TypeScript check
- frontend Vitest coverage

Set `PRE_PUSH_E2E=1` to include Playwright E2E tests. Leave it unset for the
normal local gate; CI already runs the full browser matrix.
