# SPARK Developer Environment Launcher
#
# Sets up the USB tunnel (adb reverse tcp:3000 tcp:3000) and launches the Next.js API server
# from the canonical project directory, verifying that all mobile endpoints are healthy.

[CmdletBinding()]
param(
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$apiDir = Join-Path $repoRoot 'api'

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   SPARK Development Environment Setup   " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Locate adb
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

    return $null
}

$adb = Find-Adb
if ($adb) {
    Write-Host "[1/3] Checking connected Android devices..." -ForegroundColor Yellow
    $devices = (& $adb devices) | Where-Object { $_ -match '\sdevice$' }
    if ($devices) {
        Write-Host "  -> Device found: $($devices -join ', ')" -ForegroundColor Green
        Write-Host "  -> Forwarding port 3000 (adb reverse tcp:3000 tcp:3000)..." -ForegroundColor Yellow
        & $adb reverse tcp:3000 tcp:3000
        Write-Host "  -> USB tunnel active!" -ForegroundColor Green
    } else {
        Write-Host "  -> No phone connected via USB. (App can connect once plugged in with USB debugging)." -ForegroundColor DarkGray
    }
} else {
    Write-Host "[1/3] adb not found. Skipping USB tunnel." -ForegroundColor DarkGray
}

# 2. Check / Start API
Write-Host "[2/3] Checking API server on port 3000..." -ForegroundColor Yellow
$portInUse = Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue

if (-not $portInUse) {
    if ($CheckOnly) {
        Write-Host "  -> API server is not running." -ForegroundColor Red
        exit 1
    }
    Write-Host "  -> Starting API server in $apiDir..." -ForegroundColor Yellow
    Start-Process -FilePath "npm" -ArgumentList "run dev" -WorkingDirectory $apiDir -NoNewWindow
    Write-Host "  -> Waiting for server to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 4
} else {
    Write-Host "  -> API server is already running on port 3000." -ForegroundColor Green
}

# 3. Test core endpoints
Write-Host "[3/3] Verifying core endpoints..." -ForegroundColor Yellow

$healthOk = $false
$booksOk = $false

try {
    $healthRes = Invoke-RestMethod -Uri "http://127.0.0.1:3000/api/mobile/v1/health" -Method Get -TimeoutSec 3
    if ($healthRes.data.ok -eq $true) {
        $healthOk = $true
        Write-Host "  -> /health: OK (v1)" -ForegroundColor Green
    }
} catch {
    Write-Host "  -> /health: FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

try {
    $null = Invoke-RestMethod -Uri "http://127.0.0.1:3000/api/mobile/v1/books" -Method Get -TimeoutSec 3
    $booksOk = $true
    Write-Host "  -> /books: OK" -ForegroundColor Green
} catch {
    if ($_.Exception.Message -match '401') {
        $booksOk = $true
        Write-Host "  -> /books: OK (Route active, requires token)" -ForegroundColor Green
    } else {
        Write-Host "  -> /books: FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

Write-Host "------------------------------------------" -ForegroundColor Cyan
if ($healthOk -and $booksOk) {
    Write-Host "SPARK API is fully ready and healthy!" -ForegroundColor Green
    Write-Host "Open SPARK on your phone and enjoy reading/listening." -ForegroundColor Green
} else {
    Write-Host "One or more endpoints had issues. Check logs above." -ForegroundColor Yellow
}
Write-Host "==========================================" -ForegroundColor Cyan
