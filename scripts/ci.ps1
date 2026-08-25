[CmdletBinding()]
param()

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

$gradle = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradle)) {
    throw 'gradlew.bat was not found in the repository root.'
}

$releaseDir = Join-Path $root 'app\build\outputs\apk\release'
if (Test-Path $releaseDir) {
    Remove-Item $releaseDir -Recurse -Force
}

Write-Host 'WhiteListChecker CI'
Write-Host "Repository: $root"

Invoke-CheckedCommand -FilePath $gradle -Arguments @(
    'testDebugUnitTest',
    'lintDebug',
    'assembleDebug',
    'assembleRelease'
)

$debugApk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $debugApk)) {
    throw "Debug APK was not produced: $debugApk"
}

if (-not (Test-Path $releaseDir)) {
    throw "Release output directory was not produced: $releaseDir"
}

$signedRelease = Join-Path $releaseDir 'app-release.apk'
$unsignedRelease = Join-Path $releaseDir 'app-release-unsigned.apk'

if (Test-Path $signedRelease) {
    $apkSigner = Find-ApkSigner
    if (-not $apkSigner) {
        throw 'A signed release APK was produced, but apksigner was not found for signature validation.'
    }

    Invoke-CheckedCommand -FilePath $apkSigner -Arguments @(
        'verify',
        '--verbose',
        $signedRelease
    )

    Write-Host 'Release signing: signed APK verified.'
} elseif (Test-Path $unsignedRelease) {
    Write-Host 'Release signing: credentials are not configured in this environment; unsigned release build validated.'
} else {
    $produced = @(Get-ChildItem $releaseDir -File -Filter '*.apk' -ErrorAction SilentlyContinue)
    if ($produced.Count -eq 0) {
        throw 'assembleRelease completed but no release APK was found.'
    }

    throw "Unexpected release APK name(s): $($produced.Name -join ', ')"
}

Write-Host 'CI completed successfully.'
