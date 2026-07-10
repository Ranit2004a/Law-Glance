# ============================================================
#  Law Glance — Dev Startup Script
#  Run this every time you start the project.
#  It will:
#    1. Start Docker (gemini_service on port 8000)
#    2. Start Cloudflare tunnel and get the public URL
#    3. Auto-patch NetworkConfig.java with the new URL
#  After this runs, rebuild the APK in Android Studio.
# ============================================================

$ErrorActionPreference = "Stop"

$CLOUDFLARED = "C:\Program Files (x86)\cloudflared\cloudflared.exe"
$NETWORK_CFG = "$PSScriptRoot\LAW_GLANCE\app\src\main\java\com\example\ywinked\NetworkConfig.java"
$TUNNEL_LOG  = "$PSScriptRoot\.tunnel.log"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   Law Glance Dev Startup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Start Docker
Write-Host "[1/3] Starting Docker backend..." -ForegroundColor Yellow
$dockerRunning = docker ps --filter "name=gemini_service" --filter "status=running" -q 2>$null
if ($dockerRunning) {
    Write-Host "      gemini_service already running. Skipping." -ForegroundColor Green
} else {
    & docker compose -f "$PSScriptRoot\docker-compose.yml" up -d
    Write-Host "      Waiting for service to be healthy..." -ForegroundColor Gray
    $maxWait = 60; $waited = 0
    do {
        Start-Sleep -Seconds 2; $waited += 2
        $status = docker inspect --format="{{.State.Health.Status}}" gemini_service 2>$null
    } while ($status -ne "healthy" -and $waited -lt $maxWait)
    if ($status -eq "healthy") {
        Write-Host "      gemini_service is healthy!" -ForegroundColor Green
    } else {
        Write-Host "      Warning: service status = $status" -ForegroundColor DarkYellow
    }
}

# Step 2: Start Cloudflare tunnel
Write-Host ""
Write-Host "[2/3] Starting Cloudflare tunnel..." -ForegroundColor Yellow
Get-Process -Name "cloudflared" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Remove-Item $TUNNEL_LOG -ErrorAction SilentlyContinue

$cfProcess = Start-Process -FilePath $CLOUDFLARED `
    -ArgumentList "tunnel --url http://localhost:8000" `
    -RedirectStandardError $TUNNEL_LOG `
    -NoNewWindow -PassThru

$tunnelUrl = $null; $timeout = 25; $elapsed = 0
Write-Host "      Waiting for tunnel URL..." -ForegroundColor Gray
while (-not $tunnelUrl -and $elapsed -lt $timeout) {
    Start-Sleep -Seconds 1; $elapsed++
    if (Test-Path $TUNNEL_LOG) {
        $logContent = Get-Content $TUNNEL_LOG -Raw -ErrorAction SilentlyContinue
        if ($logContent -match "https://[a-z0-9\-]+\.trycloudflare\.com") {
            $tunnelUrl = $Matches[0]
        }
    }
}

if (-not $tunnelUrl) {
    Write-Host "      ERROR: Could not get tunnel URL. Check cloudflared manually." -ForegroundColor Red
    exit 1
}
Write-Host "      Tunnel URL: $tunnelUrl" -ForegroundColor Green

# Step 3: Patch NetworkConfig.java
Write-Host ""
Write-Host "[3/3] Patching NetworkConfig.java..." -ForegroundColor Yellow

$newContent = "package com.example.ywinked;`n`npublic class NetworkConfig {`n    // Auto-updated by start_dev.ps1 each session.`n    // After this script runs, rebuild the APK in Android Studio.`n    public static final String BASE_URL = `"$tunnelUrl`";`n}`n"

[System.IO.File]::WriteAllText($NETWORK_CFG, $newContent, [System.Text.UTF8Encoding]::new($false))
Write-Host "      NetworkConfig.java updated!" -ForegroundColor Green

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " DONE! Now rebuild APK in Android Studio" -ForegroundColor Cyan
Write-Host " and reinstall on your phone." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host " Health check: $tunnelUrl/health" -ForegroundColor Gray
Write-Host ""
Write-Host " Keep this terminal open while testing!" -ForegroundColor DarkYellow
Write-Host ""
