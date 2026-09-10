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

function Assert-ReleaseApk {
    param(
        [Parameter(Mandatory = $true)][string]$Directory,
        [Parameter(Mandatory = $true)][string]$SignedName,
        [Parameter(Mandatory = $true)][string]$UnsignedName,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if (-not (Test-Path $Directory)) {
        throw "$Label output directory was not produced: $Directory"
    }

    $signedApk = Join-Path $Directory $SignedName
    $unsignedApk = Join-Path $Directory $UnsignedName

    if (Test-Path $signedApk) {
        $apkSigner = Find-ApkSigner
        if (-not $apkSigner) {
            throw "A signed $Label APK was produced, but apksigner was not found for signature validation."
        }

        Invoke-CheckedCommand -FilePath $apkSigner -Arguments @(
            'verify',
            '--verbose',
            $signedApk
        )

        Write-Host "$Label signing: signed APK verified."
        return
    }

    if (Test-Path $unsignedApk) {
        Write-Host "$Label signing: credentials are not configured in this environment; unsigned build validated."
        return
    }

    $produced = @(Get-ChildItem $Directory -File -Filter '*.apk' -ErrorAction SilentlyContinue)
    if ($produced.Count -eq 0) {
        throw "$Label build completed but no APK was found."
    }

    throw "Unexpected $Label APK name(s): $($produced.Name -join ', ')"
}

$gradle = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradle)) {
    throw 'gradlew.bat was not found in the repository root.'
}

$releaseDir = Join-Path $root 'app\build\outputs\apk\release'
$rustoreReleaseDir = Join-Path $root 'app\build\outputs\apk\rustoreRelease'
foreach ($directory in @($releaseDir, $rustoreReleaseDir)) {
    if (Test-Path $directory) {
        Remove-Item $directory -Recurse -Force
    }
}

Write-Host 'WhiteListChecker CI'
Write-Host "Repository: $root"

Invoke-CheckedCommand -FilePath $gradle -Arguments @(
    'testDebugUnitTest',
    'lintDebug',
    'assembleDebug',
    'assembleRelease',
    'assembleRustoreRelease'
)

$debugApk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $debugApk)) {
    throw "Debug APK was not produced: $debugApk"
}

Assert-ReleaseApk `
    -Directory $releaseDir `
    -SignedName 'app-release.apk' `
    -UnsignedName 'app-release-unsigned.apk' `
    -Label 'Release'

Assert-ReleaseApk `
    -Directory $rustoreReleaseDir `
    -SignedName 'app-rustoreRelease.apk' `
    -UnsignedName 'app-rustoreRelease-unsigned.apk' `
    -Label 'RuStore release'

Write-Host 'CI completed successfully.'
