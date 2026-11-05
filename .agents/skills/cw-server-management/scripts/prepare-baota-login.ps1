[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Username', 'Password')]
    [string]$Field,
    [string]$CredentialPath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($CredentialPath)) {
    $projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
    $CredentialPath = Join-Path $projectRoot 'tmp\cw-baota-credential.xml'
}

if (-not (Test-Path -LiteralPath $CredentialPath -PathType Leaf)) {
    throw "Baota credential file was not found at $CredentialPath"
}

$credential = Import-Clixml -LiteralPath $CredentialPath
$value = if ($Field -eq 'Username') {
    $credential.UserName
}
else {
    $credential.GetNetworkCredential().Password
}

Set-Clipboard -Value $value
Write-Output "Copied $Field to the clipboard."
