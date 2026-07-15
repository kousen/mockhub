# MCP OAuth2 Signing Key Setup (`MCP_OAUTH2_JWK`)

## Why this matters

The embedded OAuth2 authorization server signs MCP access tokens with an RSA key
pair. If `MCP_OAUTH2_JWK` is **not** set, an ephemeral key pair is generated at
startup — which means **every redeploy invalidates all outstanding tokens**,
forcing every connected agent (Claude Desktop, etc.) to re-authorize. Claude
mobile syncs connectors from desktop and cannot force a fresh token exchange, so
stale tokens cause persistent 401 failures.

With the key persisted, the refresh-token sliding window (default 60 days, see
`mockhub.mcp.oauth2.refresh-token-ttl`) works as designed: an actively-used
connector never re-authenticates; an idle one expires on schedule.

The signing key is only half of redeploy survival. Refresh tokens are opaque
server-side tokens, and DCR client registrations are server-side state — both
are persisted to Postgres (`oauth2_authorization` / `oauth2_registered_client`,
Flyway V36) rather than held in memory. Before that migration, every redeploy
wiped both stores and connectors demanded re-authentication daily even with the
JWK persisted (issue #266). No extra configuration is needed; the tables ship
with the schema.

You can confirm which mode you are in from the startup logs:

```
Loaded MCP OAuth2 RSA key from MCP_OAUTH2_JWK environment variable (kid=mockhub-mcp-1)   ← persisted
MCP_OAUTH2_JWK not set — using ephemeral RSA key (tokens will not survive redeploys)      ← ephemeral
```

## Value format

A full RSA key pair in JWK (JSON Web Key) format — **private parameters
included** — as a single line of JSON:

```json
{"kty":"RSA","kid":"mockhub-mcp-1","n":"...","e":"AQAB","d":"...","p":"...","q":"...","dp":"...","dq":"...","qi":"..."}
```

The `d`/`p`/`q`/`dp`/`dq`/`qi` fields are the private key (used for signing);
`n`/`e` are the public half (served via the JWKS endpoint for verification).
Include a `kid` — startup logs print it, and it keeps token-header key matching
unambiguous.

## Generating a key

Do not construct this by hand. Either option below prints a ready-to-paste line.

**Option A — JShell with Nimbus** (already in the Gradle cache from this project):

```bash
NIMBUS=$(find ~/.gradle/caches -name "nimbus-jose-jwt-*.jar" | head -1)
jshell --class-path "$NIMBUS" - <<'EOF'
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
System.out.println(new RSAKeyGenerator(2048).keyID("mockhub-mcp-1").generate().toJSONString());
EOF
```

**Option B — Python with jwcrypto:**

```bash
pip install jwcrypto
python3 -c "from jwcrypto import jwk; print(jwk.JWK.generate(kty='RSA', size=2048, kid='mockhub-mcp-1').export(private_key=True))"
```

## Setting it in Railway

Paste the entire JSON line, verbatim, as the value of `MCP_OAUTH2_JWK` in the
Railway service variables — no extra quoting or escaping in the dashboard.
Redeploy and check the startup log for the "Loaded MCP OAuth2 RSA key" line.

## Security handling

- **Treat it like the JWT secret.** Whoever holds this key can mint valid MCP
  access tokens as any user. It is the most sensitive secret in the OAuth2
  setup.
- Generate the key on your own machine and paste it directly into Railway.
  Never commit it, and keep it out of shell history (prefix the command with a
  space if your shell honors `HIST_IGNORE_SPACE`, or clean up with `history -d`).
- Do not generate production keys in shared or remote environments where the
  output may be logged or transcribed.

## Rotation

Rotation is replacement: generate a new key, swap the variable value, redeploy.
Every outstanding token dies on the next deploy — the same effect as today's
ephemeral behavior, but on your schedule. Bump the `kid` when rotating
(`mockhub-mcp-2`) so the logs make the rotation visible.
