---
name: cw-server-management
description: Access and operate the CW test server, connect through SSH or OrcaTerm, and automate Baota panel login from the existing Chrome session.
---

# CW Server Management

Use this skill for CW test-server access, Baota operations, Docker management, and infrastructure checks.

## Environment boundary

Local work is limited to source development, builds, tests, and running the frontend and Spring Boot application.

MySQL, Redis, RabbitMQ, Nacos, PostgreSQL with pgvector, and Docker run on the CW test server. Do not start local substitutes for these services.

Prometheus, Grafana, and Higress are currently stopped. Mail and Spring Boot Admin are not used locally.

## Server access

| Item | Value |
| --- | --- |
| Server IP | `192.144.141.149` |
| Internal IP | `10.2.0.16` |
| SSH | `ssh -p 22 root@192.144.141.149` |
| Baota external URL | `https://192.144.141.149:9941/tencentcloud` |
| Baota internal URL | `https://10.2.0.16:9941/tencentcloud` |
| Nacos console | `http://192.144.141.149:8848/nacos` |
| RabbitMQ management | `http://192.144.141.149:15672` |
| Prometheus | stopped |
| Grafana | stopped |
| Higress | stopped |

SSH requires an SSH key or password supplied outside this repository. The current OrcaTerm and Baota terminal sessions can be used when direct SSH credentials are unavailable.

The external Tencent Cloud firewall allows TCP `5432` for PostgreSQL. Do not close ports used by MySQL `3306`, Redis `6379`, RabbitMQ `5672` and `15672`, Nacos `8848` and `9848`, Baota `9941`, or PostgreSQL `5432`.

## Docker services

| Container | Image | Ports |
| --- | --- | --- |
| `nacos` | `nacos/nacos-server:v2.4.3` | `8848`, `9848` |
| `rabbitmq` | `rabbitmq:3.13-management` | `5672`, `15672` |
| `pgvector` | `pgvector/pgvector:pg16` | `5432` |

The pgvector container uses a 384 MB memory limit, a 512 MB memory-plus-swap limit, and stores data at `/opt/rich-code-weaver/pgvector`. Its PostgreSQL credentials are stored in `/opt/rich-code-weaver/pgvector.env` with mode `600`.

The vector extension is installed with:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Application table creation is handled by LangChain4j. The current table is `rag_embeddings` with vector dimension `1536`.

## Baota credentials

Never commit the Baota username or password. Store them as a DPAPI-encrypted PowerShell credential outside the tracked files.

Initialize or replace the local credential:

```powershell
$env:CW_BAOTA_USERNAME = 'set-through-a-local-secret-source'
$env:CW_BAOTA_PASSWORD = 'set-through-a-local-secret-source'
pwsh .\.agents\skills\cw-server-management\scripts\set-baota-credential.ps1
Remove-Item Env:\CW_BAOTA_USERNAME
Remove-Item Env:\CW_BAOTA_PASSWORD
```

The default credential file is `tmp/cw-baota-credential.xml`. The repository ignores `tmp/`; keep it out of commits.

## Automated Baota login

Use Chrome through `cua_repl`. Prefer the already open Baota tab and reuse the existing authenticated session. Open a new tab only when no Baota tab exists.

1. Copy the username to the Windows clipboard without printing it:

```powershell
pwsh .\.agents\skills\cw-server-management\scripts\prepare-baota-login.ps1 -Field Username
```

2. In the current Baota tab, read the clipboard into a variable and fill the username field:

```javascript
const username = await tab.clipboard.readText();
await tab.setValue(usernameFieldIndex, username);
```

3. Copy and fill the password the same way:

```powershell
pwsh .\.agents\skills\cw-server-management\scripts\prepare-baota-login.ps1 -Field Password
```

```javascript
const password = await tab.clipboard.readText();
await tab.setValue(passwordFieldIndex, password);
await tab.click(loginButtonIndex);
```

4. Refresh the accessibility tree and verify that the Baota home page is visible.

Do not print either clipboard value. Prefer `setValue` or `paste` over character-by-character typing. Clear the clipboard when the login flow finishes:

```powershell
Set-Clipboard -Value ' '
```

If the existing tab is already authenticated, do not log in again. Navigate directly to the requested Baota page.

## Operational hints

Use the Baota Docker page for container lifecycle tasks and the Baota terminal for host commands that do not require a separate SSH session.

Prefer read-only checks first:

```bash
docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}'
ss -lntp
free -h
df -h
```

Use `cw-server-nacos-config` for Nacos configuration reads and writes.
