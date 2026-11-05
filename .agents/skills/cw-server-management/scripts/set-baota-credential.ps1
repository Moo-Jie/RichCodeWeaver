[CmdletBinding()]
param(
    [string]$Username = $env:CW_BAOTA_USERNAME,
    [string]$CredentialPath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($CredentialPath)) {
    $projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
    $CredentialPath = Join-Path $projectRoot 'tmp\cw-baota-credential.xml'
}

if ([string]::IsNullOrWhiteSpace($Username)) {
    $Username = Read-Host 'Baota username'
}

if (-not [string]::IsNullOrEmpty($env:CW_BAOTA_PASSWORD)) {
    $securePassword = ConvertTo-SecureString $env:CW_BAOTA_PASSWORD -AsPlainText -Force
    Remove-Item Env:\CW_BAOTA_PASSWORD
}
else {
    $securePassword = Read-Host 'Baota password' -AsSecureString
}

$credential = [System.Management.Automation.PSCredential]::new($Username, $securePassword)
$parent = Split-Path -Parent $CredentialPath
if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
}

$credential | Export-Clixml -LiteralPath $CredentialPath -Force

$acl = Get-Acl -LiteralPath $CredentialPath
$acl.SetAccessRuleProtection($true, $false)
$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$accessRule = [System.Security.AccessControl.FileSystemAccessRule]::new(
    $currentUser,
    [System.Security.AccessControl.FileSystemRights]::FullControl,
    [System.Security.AccessControl.AccessControlType]::Allow
)
$acl.AddAccessRule($accessRule)
Set-Acl -LiteralPath $CredentialPath -AclObject $acl

Write-Output "Stored the encrypted Baota credential at $CredentialPath"
