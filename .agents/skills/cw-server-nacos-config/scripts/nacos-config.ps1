[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('list', 'get', 'set', 'delete')]
    [string]$Action,

    [string]$DataId = 'rich-code-weaver.yaml',
    [string]$Group = 'DEFAULT_GROUP',
    [string]$Namespace = $env:NACOS_NAMESPACE,
    [string]$Server = $env:CW_NACOS_BASE_URL,
    [string]$File,
    [string]$BackupDir = (Join-Path $env:TEMP 'cw-nacos-backups'),
    [int]$PageSize = 100,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($Server)) {
    $Server = 'http://192.144.141.149:8848/nacos'
}

if ($Server -notmatch '^[a-zA-Z][a-zA-Z0-9+.-]*://') {
    $Server = "http://$Server"
}

$Server = $Server.TrimEnd('/')
if ($Server -notmatch '/nacos$') {
    $Server = "$Server/nacos"
}

function Get-NacosAccessToken {
    param([string]$NacosBase)

    if (-not [string]::IsNullOrWhiteSpace($env:NACOS_ACCESS_TOKEN)) {
        return $env:NACOS_ACCESS_TOKEN
    }

    if ([string]::IsNullOrWhiteSpace($env:NACOS_USERNAME) -or
        [string]::IsNullOrWhiteSpace($env:NACOS_PASSWORD)) {
        return $null
    }

    $loginResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$NacosBase/v1/auth/login" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{
            username = $env:NACOS_USERNAME
            password = $env:NACOS_PASSWORD
        }

    if ([string]::IsNullOrWhiteSpace($loginResponse.accessToken)) {
        throw 'Nacos authentication succeeded without returning an access token.'
    }

    return [string]$loginResponse.accessToken
}

function New-NacosQuery {
    param(
        [hashtable]$Values,
        [string[]]$AllowEmpty = @()
    )

    $parts = [System.Collections.Generic.List[string]]::new()
    foreach ($entry in $Values.GetEnumerator()) {
        if ($null -eq $entry.Value) {
            continue
        }

        $stringValue = [string]$entry.Value
        if ([string]::IsNullOrWhiteSpace($stringValue) -and $entry.Key -notin $AllowEmpty) {
            continue
        }

        $name = [uri]::EscapeDataString([string]$entry.Key)
        $value = [uri]::EscapeDataString($stringValue)
        [void]$parts.Add("$name=$value")
    }

    return ($parts -join '&')
}

function Get-NacosConfigContent {
    param(
        [string]$NacosBase,
        [string]$ConfigDataId,
        [string]$ConfigGroup,
        [string]$ConfigNamespace,
        [string]$AccessToken
    )

    $query = New-NacosQuery @{
        dataId = $ConfigDataId
        group = $ConfigGroup
        tenant = $ConfigNamespace
        accessToken = $AccessToken
    }

    try {
        $response = Invoke-WebRequest `
            -Method Get `
            -Uri "$NacosBase/v1/cs/configs?$query" `
            -UseBasicParsing
        $content = [string]$response.Content
        if ([string]::IsNullOrEmpty($content)) {
            return $null
        }

        return $content
    }
    catch {
        $statusCode = $null
        if ($null -ne $_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            catch {
                $statusCode = $null
            }
        }

        if ($statusCode -eq 404) {
            return $null
        }

        throw
    }
}

function Backup-NacosConfig {
    param(
        [string]$Content,
        [string]$ConfigDataId,
        [string]$ConfigGroup
    )

    if ([string]::IsNullOrEmpty($Content)) {
        return $null
    }

    if (-not (Test-Path -LiteralPath $BackupDir -PathType Container)) {
        New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    }

    $safeGroup = $ConfigGroup -replace '[^a-zA-Z0-9._-]', '_'
    $safeDataId = $ConfigDataId -replace '[^a-zA-Z0-9._-]', '_'
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $backupPath = Join-Path $BackupDir "$timestamp-$safeGroup-$safeDataId"
    Set-Content -LiteralPath $backupPath -Value $Content -Encoding utf8 -NoNewline
    return $backupPath
}

$accessToken = Get-NacosAccessToken -NacosBase $Server

switch ($Action) {
    'list' {
        $searchDataId = if ($PSBoundParameters.ContainsKey('DataId') -and $DataId -ne '*') { $DataId } else { '' }
        $searchGroup = if ($Group -eq '*') { '' } else { $Group }
        $query = New-NacosQuery -Values @{
            search = 'accurate'
            dataId = $searchDataId
            group = $searchGroup
            pageNo = '1'
            pageSize = [string]$PageSize
            tenant = $Namespace
            accessToken = $accessToken
        } -AllowEmpty @('dataId', 'group')

        $response = Invoke-RestMethod `
            -Method Get `
            -Uri "$Server/v1/cs/configs?$query"

        if ($null -eq $response.pageItems -or $response.pageItems.Count -eq 0) {
            Write-Output 'No matching Nacos configurations.'
            break
        }

        $response.pageItems |
            Select-Object `
                @{ Name = 'DataId'; Expression = { $_.dataId } },
                @{ Name = 'Group'; Expression = { $_.group } },
                @{ Name = 'Type'; Expression = { $_.type } }
    }

    'get' {
        $content = Get-NacosConfigContent `
            -NacosBase $Server `
            -ConfigDataId $DataId `
            -ConfigGroup $Group `
            -ConfigNamespace $Namespace `
            -AccessToken $accessToken

        if ($null -eq $content) {
            throw "Nacos configuration '$DataId' in group '$Group' was not found."
        }

        Write-Output $content
    }

    'set' {
        if ([string]::IsNullOrWhiteSpace($File)) {
            throw 'The set action requires -File.'
        }

        if (-not (Test-Path -LiteralPath $File -PathType Leaf)) {
            throw "Configuration file '$File' does not exist."
        }

        $content = Get-Content -LiteralPath $File -Raw -Encoding utf8
        $existing = Get-NacosConfigContent `
            -NacosBase $Server `
            -ConfigDataId $DataId `
            -ConfigGroup $Group `
            -ConfigNamespace $Namespace `
            -AccessToken $accessToken

        $backupPath = Backup-NacosConfig `
            -Content $existing `
            -ConfigDataId $DataId `
            -ConfigGroup $Group

        if ($null -ne $backupPath) {
            Write-Host "Backed up the existing configuration to $backupPath"
        }

        $type = if ($DataId -match '\.([^.]+)$') {
            $Matches[1].ToLowerInvariant()
        }
        else {
            'yaml'
        }

        $body = @{
            dataId = $DataId
            group = $Group
            type = $type
            content = $content
            tenant = $Namespace
            accessToken = $accessToken
        }

        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$Server/v1/cs/configs" `
            -ContentType 'application/x-www-form-urlencoded' `
            -Body $body

        Write-Output "Published '$DataId' to group '$Group'. Nacos response: $response"
    }

    'delete' {
        if (-not $Force) {
            throw 'Refusing to delete a Nacos configuration without -Force.'
        }

        $existing = Get-NacosConfigContent `
            -NacosBase $Server `
            -ConfigDataId $DataId `
            -ConfigGroup $Group `
            -ConfigNamespace $Namespace `
            -AccessToken $accessToken

        if ($null -eq $existing) {
            throw "Nacos configuration '$DataId' in group '$Group' was not found."
        }

        $backupPath = Backup-NacosConfig `
            -Content $existing `
            -ConfigDataId $DataId `
            -ConfigGroup $Group
        Write-Host "Backed up the configuration to $backupPath"

        $query = New-NacosQuery @{
            dataId = $DataId
            group = $Group
            tenant = $Namespace
            accessToken = $accessToken
        }

        $response = Invoke-RestMethod `
            -Method Delete `
            -Uri "$Server/v1/cs/configs?$query"

        Write-Output "Deleted '$DataId' from group '$Group'. Nacos response: $response"
    }
}
