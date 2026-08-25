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
    $fromPath = Get-Command 'apksigner.bat' -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $sdkRoots = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path $_) } | Select-Object -Unique

    foreach ($sdkRoot in $sdkRoots) {
        $buildToolsRoot = Join-Path $sdkRoot 'build-tools'
        if (-not (Test-Path $buildToolsRoot)) {
            continue
        }

        $versions = Get-ChildItem $buildToolsRoot -Directory | Sort-Object Name -Descending
        foreach ($version in $versions) {
            $candidate = Join-Path $version.FullName 'apksigner.bat'
            if (Test-Path $candidate) {
                return $candidate
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

$releaseDir = Join-Path $root 'app\build\outputs\apk\release'
if (Test-Path $releaseDir) {
    Remove-Item $releaseDir -Recurse -Force
}

$distDir = Join-Path $root 'dist'
if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir | Out-Null

Write-Host "Building WhiteListChecker $Version"
Invoke-CheckedCommand -FilePath $gradle -Arguments @('assembleRelease')

$signedApk = Join-Path $releaseDir 'app-release.apk'
$unsignedApk = Join-Path $releaseDir 'app-release-unsigned.apk'

if (-not (Test-Path $signedApk)) {
    if (Test-Path $unsignedApk) {
        throw 'Release signing credentials are unavailable. A publishable release must be signed; configure WL_RELEASE_* outside the repository on the trusted runner.'
    }

    throw 'Signed release APK was not produced.'
}

$apkSigner = Find-ApkSigner
if (-not $apkSigner) {
    throw 'apksigner was not found. Release signature cannot be validated.'
}

Write-Host 'Verifying APK signature...'
$certOutput = @(& $apkSigner verify --verbose --print-certs $signedApk 2>&1)
if ($LASTEXITCODE -ne 0) {
    $certOutput | ForEach-Object { Write-Host $_ }
    throw 'apksigner verification failed.'
}

$certText = $certOutput -join "`n"
if ($certText -match 'CN=Android Debug') {
    throw 'Release APK is signed with the Android debug certificate. Refusing to publish it.'
}

$certOutput | ForEach-Object { Write-Host $_ }

$artifactName = "WhiteListChecker-$Version-release.apk"
$artifactPath = Join-Path $distDir $artifactName
Copy-Item $signedApk $artifactPath -Force

$hash = (Get-FileHash $artifactPath -Algorithm SHA256).Hash.ToUpperInvariant()
$checksumPath = Join-Path $distDir 'SHA256SUMS.txt'
Set-Content -Path $checksumPath -Value "$hash  $artifactName" -Encoding ASCII

$artifact = Get-Item $artifactPath
Write-Host 'Release package completed:'
Write-Host " - file: $($artifact.Name)"
Write-Host " - size: $($artifact.Length) bytes"
Write-Host " - SHA-256: $hash"
Write-Host " - checksum file: $(Split-Path $checksumPath -Leaf)"
