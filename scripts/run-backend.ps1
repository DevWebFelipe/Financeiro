#Requires -Version 5.1

<#
.SYNOPSIS
    Inicia o backend Spring Boot com as variaveis do .env local.

.DESCRIPTION
    Carrega JWT_SECRET e demais variaveis de .env, depois executa
    mvnw spring-boot:run a partir de backend/.

    Usado por scripts/start.ps1 (novo terminal) e tambem pode ser
    executado manualmente a partir da raiz do projeto:

        powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
#>

$ErrorActionPreference = "Stop"

$ScriptsDir  = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptsDir
$BackendDir  = Join-Path $ProjectRoot "backend"
$ImportDotEnv = Join-Path $ScriptsDir "import-dotenv.ps1"
$BackendWrapper = Join-Path $BackendDir "mvnw.cmd"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Financial Control - Backend" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Diretorio:" -ForegroundColor DarkGray
Write-Host $BackendDir -ForegroundColor Gray
Write-Host ""

$mvnExit = 0

try {
    if (-not (Test-Path -LiteralPath $BackendWrapper)) {
        throw "Maven Wrapper nao encontrado: $BackendWrapper"
    }

    . $ImportDotEnv -ProjectRoot $ProjectRoot -Override

    Set-Location -LiteralPath $BackendDir

    Write-Host "Iniciando Spring Boot..." -ForegroundColor Yellow
    Write-Host ""

    # mvnw.cmd e um hibrido batch/PowerShell. Executa-lo via cmd.exe evita
    # que o ErrorActionPreference desta sessao interrompa o wrapper.
    $ErrorActionPreference = "Continue"
    cmd.exe /c "mvnw.cmd spring-boot:run"
    $mvnExit = $LASTEXITCODE
    $ErrorActionPreference = "Stop"

    if ($mvnExit -ne 0) {
        throw "O backend encerrou com codigo $mvnExit."
    }
}
catch {
    Write-Host ""
    Write-Host ("[ERRO] " + $_.Exception.Message) -ForegroundColor Red
    Write-Host ""
}
finally {
    Write-Host ""
    Write-Host "Backend encerrado." -ForegroundColor Yellow
    Read-Host "Pressione ENTER para fechar este terminal"
}
