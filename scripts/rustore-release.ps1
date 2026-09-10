[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    Write-Host ">> $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
    }
}

function Find-ApkSigner {
    foreach ($commandName in @('apksigner.bat', 'apksigner')) {
        $fromPath = Get-Command $commandName -ErrorAction SilentlyContinue
        if ($fromPath) {
            return $fromPath.Source
        }
    }

    $androidSdkRoot = [Environment]::GetEnvironmentVariable('ANDROID_SDK_ROOT')
    $androidHome = [Environment]::GetEnvironmentVariable('ANDROID_HOME')
    $sdkRoots = @(
        $androidSdkRoot,
        $androidHome
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path $_) } | Select-Object -Unique

    foreach ($sdkRoot in $sdkRoots) {
        $buildToolsRoot = Join-Path $sdkRoot 'build-tools'
        if (-not (Test-Path $buildToolsRoot)) {
            continue
        }

        $versions = Get-ChildItem $buildToolsRoot -Directory | Sort-Object Name -Descending
        foreach ($version in $versions) {
            foreach ($fileName in @('apksigner.bat', 'apksigner')) {
                $candidate = Join-Path $version.FullName $fileName
                if (Test-Path $candidate) {
                    return $candidate
                }
            }
        }
    }

    return $null
}

if ($Version -notmatch '^v\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$') {
    throw "Invalid release version '$Version'. Expected vX.Y.Z or a SemVer prerelease tag."
}

$plainVersion = $Version.Substring(1)
$buildFile = Join-Path $root 'app\build.gradle.kts'
$buildText = Get-Content $buildFile -Raw
$versionMatch = [regex]::Match($buildText, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) {
    throw 'Could not read versionName from app/build.gradle.kts.'
}

$appVersion = $versionMatch.Groups[1].Value
if ($appVersion -ne $plainVersion) {
    throw "Release tag '$Version' does not match Android versionName '$appVersion'."
}

$gradle = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradle)) {
    throw 'gradlew.bat was not found in the repository root.'
}

$releaseDir = Join-Path $root 'app\build\outputs\apk\rustoreRelease'
if (Test-Path $releaseDir) {
    Remove-Item $releaseDir -Recurse -Force
}

$distDir = Join-Path $root 'dist\rustore'
if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

Write-Host "Building WhiteListChecker $Version for RuStore (DNS diagnostics disabled)"
Invoke-CheckedCommand -FilePath $gradle -Arguments @('assembleRustoreRelease')

$signedApk = Join-Path $releaseDir 'app-rustoreRelease.apk'
$unsignedApk = Join-Path $releaseDir 'app-rustoreRelease-unsigned.apk'

if (-not (Test-Path $signedApk)) {
    if (Test-Path $unsignedApk) {
        throw 'Release signing credentials are unavailable. Configure WL_RELEASE_* outside the repository before creating the RuStore package.'
    }

    $produced = @(Get-ChildItem $releaseDir -File -Filter '*.apk' -ErrorAction SilentlyContinue)
    if ($produced.Count -gt 0) {
        throw "Unexpected RuStore APK name(s): $($produced.Name -join ', ')"
    }
    throw 'Signed RuStore APK was not produced.'
}

$apkSigner = Find-ApkSigner
if (-not $apkSigner) {
    throw 'apksigner was not found. RuStore release signature cannot be validated.'
}

Write-Host 'Verifying RuStore APK signature...'
$certOutput = @(& $apkSigner verify --verbose --print-certs $signedApk 2>&1)
if ($LASTEXITCODE -ne 0) {
    $certOutput | ForEach-Object { Write-Host $_ }
    throw 'apksigner verification failed.'
}

$certText = $certOutput -join "`n"
if ($certText -match 'CN=Android Debug') {
    throw 'RuStore APK is signed with the Android debug certificate. Refusing to package it.'
}

$certOutput | ForEach-Object { Write-Host $_ }

$artifactName = "WhiteListChecker-$Version-rustore.apk"
$artifactPath = Join-Path $distDir $artifactName
Copy-Item $signedApk $artifactPath -Force

$hash = (Get-FileHash $artifactPath -Algorithm SHA256).Hash.ToUpperInvariant()
$checksumPath = Join-Path $distDir 'SHA256SUMS.txt'
Set-Content -Path $checksumPath -Value "$hash  $artifactName" -Encoding ASCII

$artifact = Get-Item $artifactPath
Write-Host 'RuStore package completed:'
Write-Host " - file: $($artifact.Name)"
Write-Host " - size: $($artifact.Length) bytes"
Write-Host " - SHA-256: $hash"
Write-Host " - checksum file: $(Split-Path $checksumPath -Leaf)"
