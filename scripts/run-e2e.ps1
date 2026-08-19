#Requires -Version 5.1

<#
.SYNOPSIS
    Runs the Financial Control Phase 22 Playwright E2E suite (Chromium).

.DESCRIPTION
    1. Ensures PostgreSQL is up (docker compose).
    2. Checks backend health at /api/v1/health.
    3. Runs `ng e2e` from frontend/ (Playwright starts or reuses the Angular server).

    Does not start a new backend process if health is already UP.
    If the backend is down, run scripts/start.ps1 or scripts/run-backend.ps1 first.
#>

$ErrorActionPreference = "Stop"

$ScriptsDir  = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptsDir
$FrontendDir = Join-Path $ProjectRoot "frontend"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Financial Control - E2E (Playwright / Chromium)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -LiteralPath $ProjectRoot
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start PostgreSQL."
}

$healthUrl = "http://localhost:8080/api/v1/health"
try {
    $health = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 5
    if ($health.status -ne "UP") {
        throw "Backend health is $($health.status)."
    }
    Write-Host "[OK] Backend is UP." -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Backend is not reachable at $healthUrl" -ForegroundColor Red
    Write-Host "Start it with: powershell.exe -ExecutionPolicy Bypass -File .\scripts\start.ps1" -ForegroundColor Yellow
    throw
}

Set-Location -LiteralPath $FrontendDir
npx ng e2e
if ($LASTEXITCODE -ne 0) {
    throw "Playwright E2E failed."
}
