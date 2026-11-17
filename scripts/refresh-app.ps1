# Hard-refresh SPARK on the connected phone.
#
# Rebuilds the debug APK from the current source, removes the installed app so
# no stale code or cache can survive, installs the fresh APK, re-opens the USB
# tunnel for the local API and launches the app.
#
# Usage:
#   .\scripts\refresh-app.ps1              # full wipe (also logs you out)
#   .\scripts\refresh-app.ps1 -KeepData    # replace the APK, keep token + offline cache
#   .\scripts\refresh-app.ps1 -NoBuild     # reuse the last APK, just reinstall
#
# Requires: phone connected over USB with USB debugging on, API running on the
# PC at http://127.0.0.1:3000 (npm run dev in api/).

[CmdletBinding()]
param(
    [switch]$KeepData,
    [switch]$NoBuild,
    [string]$Serial
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$package = 'com.spark.app.debug'
$activity = 'com.spark.app.debug/com.spark.app.MainActivity'
$apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'

function Find-Adb {
    $localProps = Join-Path $repoRoot 'local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $sdk = ($line -split '=', 2)[1].Replace('\:', ':').Replace('\\', '\')
            $candidate = Join-Path $sdk 'platform-tools\adb.exe'
            if (Test-Path $candidate) { return $candidate }
        }
    }
    foreach ($envName in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') {
        $sdk = [Environment]::GetEnvironmentVariable($envName)
        if ($sdk) {
            $candidate = Join-Path $sdk 'platform-tools\adb.exe'
            if (Test-Path $candidate) { return $candidate }
        }
    }
    $fallback = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (Test-Path $fallback) { return $fallback }

    $onPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    throw 'adb not found. Install Android SDK platform-tools or set ANDROID_HOME.'
}

$adb = Find-Adb
$adbArgs = @()
if ($Serial) { $adbArgs += @('-s', $Serial) }

Write-Host "adb: $adb"

$devices = (& $adb devices) | Where-Object { $_.Trim() -match '\sdevice$' }
if (-not $devices) {
    throw 'No phone connected. Plug it in, unlock it and accept the USB debugging prompt.'
}
Write-Host "device: $($devices -join ', ')"

if (-not $NoBuild) {
    Write-Host 'building debug APK...'
    & (Join-Path $repoRoot 'gradlew.bat') :app:assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)." }
}

if (-not (Test-Path $apk)) {
    throw "APK missing at $apk. Run without -NoBuild."
}
$apkTime = (Get-Item $apk).LastWriteTime
Write-Host "apk: $apk ($apkTime)"

if ($KeepData) {
    Write-Host 'installing over the existing app (data kept)...'
    & $adb @adbArgs install -r $apk
} else {
    Write-Host 'removing old install (clears code, data and cache)...'
    & $adb @adbArgs uninstall $package 2>$null | Out-Null
    Write-Host 'installing fresh APK...'
    & $adb @adbArgs install $apk
}
if ($LASTEXITCODE -ne 0) { throw "adb install failed (exit $LASTEXITCODE)." }

Write-Host 'opening USB tunnel to the local API (tcp:3000)...'
& $adb @adbArgs reverse tcp:3000 tcp:3000 | Out-Null

Write-Host 'launching SPARK...'
& $adb @adbArgs shell am force-stop $package
& $adb @adbArgs shell am start -n $activity

Start-Sleep -Seconds 3
$top = (& $adb @adbArgs shell dumpsys activity activities) | Select-String 'topResumedActivity'
Write-Host "foreground: $($top -join '')"
Write-Host 'done. If the API is not running (curl http://127.0.0.1:3000/api/mobile/v1/health), start it with: npm run dev (in api/)'
