---
name: add-mcp-tool
description: "Add or modify a MockHub MCP tool while keeping registration, tests, and agent docs in sync."
disable-model-invocation: true
---

# Add MCP Tool

Use when adding a new MockHub MCP tool method or tool class.

## Workflow

1. Identify the domain package and the service method the tool should delegate
   to. Keep business logic in services, not in the tool class.
2. Add or update an `@Tool` method under
   `backend/src/main/java/com/mockhub/mcp/tools/`.
3. Use explicit `@ToolParam` descriptions and mark required parameters
   accurately.
4. If the tool accepts a user email, resolve it with
   `ChatContext.resolveEmail(userEmail)` so website chat calls cannot spoof a
   different user.
5. If a new tool class is added, register it in
   `backend/src/main/java/com/mockhub/config/McpConfig.java`.
6. Update `backend/src/test/java/com/mockhub/config/McpConfigTest.java` so the
   callback count matches the actual registered surface.
7. Add focused tests for the tool class. Include ChatContext override coverage
   for user-scoped tools.
8. Update `backend/src/main/resources/static/llms.txt` with the new agent-facing
   capability.
9. Run at least:

```bash
cd backend
./gradlew test --tests '*McpConfigTest'
./gradlew test --tests '*<ToolClassName>Test'
```

Finish by checking whether docs under `docs/agentic-commerce.md` need a short
update.
