---
name: cw-server-nacos-config
description: Access the CW test server and manage Nacos configuration for local development, including safe list, get, publish, and delete operations.
---

# CW Server and Nacos Config

Use this skill when developing or running Rich Code Weaver locally and you need the test server endpoints or Nacos configuration access.

## Environment boundary

The local workstation is only for source code development, Maven build/test, and running the Spring Boot application locally.

Do not start MySQL, Redis, RabbitMQ, Nacos, Higress, Prometheus, or Grafana on the local workstation. Those services run on the CW test server at `192.144.141.149`.

Keep the server ports open. The current test environment depends on remote access to MySQL `3306`, Redis `6379`, RabbitMQ `5672` and `15672`, and Nacos `8848`.

## Server access

| Item | Value |
| --- | --- |
| Server IP | `192.144.141.149` |
| SSH | `ssh -p 22 root@192.144.141.149` |
| Baota panel | `https://192.144.141.149:9941/home` |
| Nacos console | `http://192.144.141.149:8848/nacos` |
| Nacos API base | `http://192.144.141.149:8848/nacos/v1/cs/configs` |
| RabbitMQ management | `http://192.144.141.149:15672` |
| Prometheus | `http://192.144.141.149:9090` |
| Grafana | `http://192.144.141.149:3000` |

SSH requires an SSH key already configured on this workstation or a password supplied interactively. Do not store a password or private key in this repository.

## Nacos configuration

The application uses these defaults; the matching placeholders are in `rich-code-weaver-monolith/src/main/resources/application.yml`.

| Setting | Default |
| --- | --- |
| Nacos address | `192.144.141.149:8848` |
| Data ID | `rich-code-weaver.yaml` |
| Group | `DEFAULT_GROUP` |
| Namespace | public namespace, meaning no tenant |
| Config format | YAML |

The test Nacos instance currently has authentication disabled. Keep authentication parameters outside the repository so the same workflow continues to work if authentication is enabled later.

For local Spring Boot runs, set:

```powershell
$env:NACOS_SERVER_ADDR = '192.144.141.149:8848'
$env:NACOS_CONFIG_DATA_ID = 'rich-code-weaver.yaml'
$env:NACOS_CONFIG_GROUP = 'DEFAULT_GROUP'
$env:NACOS_DISCOVERY_ENABLED = 'false'
$env:SPRING_PROFILES_ACTIVE = 'local'
```

MySQL, Redis, RabbitMQ, API keys, and other sensitive values must remain in Nacos configuration or environment variables. Never copy them into a tracked application configuration file.

## Read and write Nacos configuration

Use the helper from the repository root:

```powershell
$tool = '.\.agents\skills\cw-server-nacos-config\scripts\nacos-config.ps1'
```

List configurations in the default group:

```powershell
pwsh $tool list
```

Read the application configuration:

```powershell
pwsh $tool get -DataId rich-code-weaver.yaml
```

Save the current content outside the repository before editing:

```powershell
pwsh $tool get -DataId rich-code-weaver.yaml |
    Set-Content -Encoding utf8 "$env:TEMP\rich-code-weaver.yaml"
```

Publish a replacement from a temporary file outside the repository:

```powershell
pwsh $tool set -DataId rich-code-weaver.yaml -File "$env:TEMP\rich-code-weaver.yaml"
```

Delete a configuration only after checking the exact Data ID and group:

```powershell
pwsh $tool delete -DataId obsolete.yaml -Group DEFAULT_GROUP -Force
```

The script automatically backs up an existing value under `$env:TEMP\cw-nacos-backups` before a publish or delete. The backup can contain secrets, so remove it after use and never add it to Git.

Optional parameters and environment variables:

- `-Server` or `CW_NACOS_BASE_URL`: Nacos base URL, with or without the `/nacos` suffix.
- `-Namespace` or `NACOS_NAMESPACE`: non-public namespace ID.
- `NACOS_USERNAME` and `NACOS_PASSWORD`: used to obtain an access token when authentication is enabled.
- `NACOS_ACCESS_TOKEN`: existing token, when supplied by the environment.
- `-BackupDir`: backup location; defaults to `$env:TEMP\cw-nacos-backups`.

## Safety rules

Read the current value before changing it. Confirm the Data ID, group, and namespace against the requested change.

Use `set` only for the intended configuration document and preserve unrelated keys. Do not print full configuration values into logs, commits, or issue comments.

Do not stop or remove remote infrastructure during local code work. If the application cannot reach Nacos, check the remote address, firewall rule for `8848`, and Nacos console availability instead of starting a local Nacos instance.
