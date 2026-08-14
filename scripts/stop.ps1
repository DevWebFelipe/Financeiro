#Requires -Version 5.1

<#
.SYNOPSIS
    Encerra todo o ambiente do Financial Control.

.DESCRIPTION
    1. Encerra processos do frontend Angular.
    2. Encerra processos do backend Spring Boot/Maven.
    3. Executa docker compose down.

.NOTES
    NÃO utiliza docker compose down -v.
    Os volumes e dados do PostgreSQL são preservados.
#>

$ErrorActionPreference = "Stop"

# ============================================================
# CONFIGURAÇÃO
# ============================================================

$ScriptsDir  = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptsDir

# ============================================================
# FUNÇÕES
# ============================================================

function Write-Header {
    Clear-Host

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host " Financial Control - Stop" -ForegroundColor Cyan
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step {
    param (
        [string]$Number,
        [string]$Message
    )

    Write-Host ""
    Write-Host "[$Number] $Message" -ForegroundColor Yellow
    Write-Host ""
}

function Write-Success {
    param ([string]$Message)

    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Stop-ProcessTree {
    param (
        [int]$ProcessId
    )

    try {
        $Process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue

        if ($null -eq $Process) {
            return
        }

        # Obtém processos filhos recursivamente usando CIM.
        $Children = Get-CimInstance Win32_Process |
            Where-Object { $_.ParentProcessId -eq $ProcessId }

        foreach ($Child in $Children) {
            Stop-ProcessTree -ProcessId ([int]$Child.ProcessId)
        }

        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
    catch {
        # Processo pode já ter sido encerrado.
    }
}

# ============================================================
# INÍCIO
# ============================================================

try {

    Write-Header

    # --------------------------------------------------------
    # 1. FRONTEND
    # --------------------------------------------------------

    Write-Step "1/3" "Encerrando frontend Angular..."

    $FrontendProcesses = Get-CimInstance Win32_Process |
        Where-Object {
            $_.CommandLine -and (
                $_.CommandLine -match "npm(\.cmd)?\s+start" -or
                $_.CommandLine -match "ng\s+(serve|start)"
            )
        }

    if ($FrontendProcesses) {

        foreach ($Process in $FrontendProcesses) {
            Stop-ProcessTree -ProcessId ([int]$Process.ProcessId)
        }

        Write-Success "Frontend encerrado."

    }
    else {

        Write-Host "[INFO] Nenhum processo do frontend encontrado." -ForegroundColor Gray
    }

    # --------------------------------------------------------
    # 2. BACKEND
    # --------------------------------------------------------

    Write-Step "2/3" "Encerrando backend Spring Boot..."

    $BackendProcesses = Get-CimInstance Win32_Process |
        Where-Object {
            $_.CommandLine -and (
                $_.CommandLine -match "run-backend\.ps1" -or
                $_.CommandLine -match "spring-boot:run" -or
                $_.CommandLine -match "org\.springframework\.boot" -or
                $_.CommandLine -match "mvnw(\.cmd)?"
            )
        }

    if ($BackendProcesses) {

        foreach ($Process in $BackendProcesses) {
            Stop-ProcessTree -ProcessId ([int]$Process.ProcessId)
        }

        Write-Success "Backend encerrado."

    }
    else {

        Write-Host "[INFO] Nenhum processo do backend encontrado." -ForegroundColor Gray
    }

    # --------------------------------------------------------
    # 3. DOCKER
    # --------------------------------------------------------

    Write-Step "3/3" "Derrubando containers do Docker Compose..."

    Push-Location $ProjectRoot

    try {

        docker compose down

        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao executar 'docker compose down'."
        }

    }
    finally {
        Pop-Location
    }

    Write-Success "Containers encerrados."

    # ========================================================
    # FINAL
    # ========================================================

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host " Financial Control encerrado" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host ""

    Write-Host "PostgreSQL : parado" -ForegroundColor White
    Write-Host "Backend    : parado" -ForegroundColor White
    Write-Host "Frontend   : parado" -ForegroundColor White

    Write-Host ""
    Write-Host "Os dados do PostgreSQL foram preservados." -ForegroundColor Green
    Write-Host ""

}
catch {

    Write-Host ""
    Write-Host "[ERRO] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""

    exit 1
}