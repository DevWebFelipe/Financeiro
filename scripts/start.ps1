#Requires -Version 5.1

<#
.SYNOPSIS
    Inicia todo o ambiente do Financial Control.

.DESCRIPTION
    1. Executa o check de ambiente.
    2. Garante o .env local (JWT_SECRET) e sobe o PostgreSQL via Docker Compose.
    3. Aguarda o PostgreSQL ficar disponível.
    4. Inicia o backend Spring Boot em um novo terminal, com as variáveis do .env.
    5. Inicia o frontend Angular em um novo terminal.

.NOTES
    Executar a partir de qualquer diretório.
    O script identifica automaticamente a raiz do projeto
    a partir da localização deste arquivo.
#>

$ErrorActionPreference = "Stop"

# ============================================================
# CONFIGURAÇÃO
# ============================================================

$ScriptsDir  = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptsDir

$BackendDir  = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"

$CheckEnvironment = Join-Path $ScriptsDir "check-environment.ps1"
$ImportDotEnv     = Join-Path $ScriptsDir "import-dotenv.ps1"

$BackendWrapper = Join-Path $BackendDir "mvnw.cmd"
$EnvFile        = Join-Path $ProjectRoot ".env"
$EnvExampleFile = Join-Path $ProjectRoot ".env.example"

# ============================================================
# FUNÇÕES
# ============================================================

function Write-Header {
    Clear-Host

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host " Financial Control - Start" -ForegroundColor Cyan
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

function Write-ErrorMessage {
    param ([string]$Message)

    Write-Host "[ERRO] $Message" -ForegroundColor Red
}

function Test-PathOrFail {
    param (
        [string]$Path,
        [string]$Description
    )

    if (-not (Test-Path $Path)) {
        throw "$Description não encontrado: $Path"
    }
}

function New-LocalJwtSecret {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    return [Convert]::ToBase64String($bytes)
}

function Initialize-LocalEnvFile {
    if (Test-Path -LiteralPath $EnvFile) {
        Write-Success ".env local encontrado."
        return
    }

    Test-PathOrFail $EnvExampleFile ".env.example"

    $content = Get-Content -LiteralPath $EnvExampleFile -Raw
    $secret = New-LocalJwtSecret

    $content = [regex]::Replace($content, '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$secret")

    # Alinha as senhas locais aos defaults já documentados em docker-compose.yml
    # e application.yml, para não divergir de um volume PostgreSQL existente.
    # Replace simples (não regex) para não falhar com CRLF do .env.example.
    $content = $content.Replace("POSTGRES_PASSWORD=CHANGE_ME", "POSTGRES_PASSWORD=financial_control_dev")
    $content = $content.Replace("DATABASE_PASSWORD=CHANGE_ME", "DATABASE_PASSWORD=financial_control_dev")

    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($EnvFile, $content, $utf8NoBom)

    Write-Success ".env local criado (JWT_SECRET gerado; arquivo fora do Git)."
}

# ============================================================
# INÍCIO
# ============================================================

try {

    Write-Header

    Write-Host "Raiz do projeto:" -ForegroundColor DarkGray
    Write-Host $ProjectRoot -ForegroundColor Gray
    Write-Host ""

    # --------------------------------------------------------
    # VALIDAÇÃO DA ESTRUTURA
    # --------------------------------------------------------

    Test-PathOrFail $BackendDir "Pasta do backend"
    Test-PathOrFail $FrontendDir "Pasta do frontend"
    Test-PathOrFail $BackendWrapper "Maven Wrapper"
    Test-PathOrFail $CheckEnvironment "Script check-environment.ps1"
    Test-PathOrFail $ImportDotEnv "Script import-dotenv.ps1"
    Test-PathOrFail (Join-Path $ScriptsDir "run-backend.ps1") "Script run-backend.ps1"
    Test-PathOrFail $EnvExampleFile ".env.example"

    # --------------------------------------------------------
    # 1. CHECK DE AMBIENTE
    # --------------------------------------------------------

    Write-Step "1/4" "Verificando ambiente..."

    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $CheckEnvironment

    if ($LASTEXITCODE -ne 0) {
        throw "A verificação de ambiente falhou. O sistema não será iniciado."
    }

    Write-Success "Ambiente validado."

    # --------------------------------------------------------
    # 2. POSTGRESQL
    # --------------------------------------------------------

    Write-Step "2/4" "Iniciando PostgreSQL via Docker Compose..."

    Initialize-LocalEnvFile

    . $ImportDotEnv -ProjectRoot $ProjectRoot -Override

    Write-Host ""

    Push-Location $ProjectRoot

    try {
        docker compose up -d

        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao executar 'docker compose up -d'."
        }
    }
    finally {
        Pop-Location
    }

    Write-Host ""
    Write-Host "Aguardando PostgreSQL ficar disponível..." -ForegroundColor Gray

    $PostgresReady = $false
    $MaxAttempts = 30

    for ($Attempt = 1; $Attempt -le $MaxAttempts; $Attempt++) {

        Push-Location $ProjectRoot

        try {
            docker compose exec -T postgres pg_isready *> $null
            $ReadyExitCode = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }

        if ($ReadyExitCode -eq 0) {
            $PostgresReady = $true
            break
        }

        Start-Sleep -Seconds 2
    }

    if (-not $PostgresReady) {
        throw "PostgreSQL não ficou disponível dentro do tempo esperado."
    }

    Write-Success "PostgreSQL está disponível."

    # --------------------------------------------------------
    # 3. BACKEND
    # --------------------------------------------------------

    Write-Step "3/4" "Iniciando backend Spring Boot..."

    $RunBackend = Join-Path $ScriptsDir "run-backend.ps1"

    # cmd start abre um novo console herdando o ambiente desta sessao
    # (JWT_SECRET, senha do banco e PATH). Start-Process no Windows PowerShell
    # 5.1 nao herda as variaveis de processo quando UseShellExecute e o padrao.
    cmd.exe /c "start `"Financial Control - Backend`" /D `"$BackendDir`" powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File `"$RunBackend`""

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao abrir o terminal do backend."
    }

    Write-Success "Terminal do backend iniciado."

    # --------------------------------------------------------
    # 4. FRONTEND
    # --------------------------------------------------------

    Write-Step "4/4" "Iniciando frontend Angular..."

    $FrontendCommand = @"
Set-Location -LiteralPath '$FrontendDir'
Write-Host ''
Write-Host '========================================================' -ForegroundColor Cyan
Write-Host ' Financial Control - Frontend' -ForegroundColor Cyan
Write-Host '========================================================' -ForegroundColor Cyan
Write-Host ''
Write-Host 'Diretório:' -ForegroundColor DarkGray
Write-Host '$FrontendDir' -ForegroundColor Gray
Write-Host ''
Write-Host 'Iniciando Angular...' -ForegroundColor Yellow
Write-Host ''
npm start
Write-Host ''
Write-Host 'Frontend encerrado.' -ForegroundColor Yellow
Read-Host 'Pressione ENTER para fechar este terminal'
"@

    Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @(
            "-NoExit",
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-Command", $FrontendCommand
        )

    Write-Success "Terminal do frontend iniciado."

    # ========================================================
    # FINAL
    # ========================================================

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host " Financial Control iniciado" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host ""

    Write-Host "PostgreSQL : Docker Compose" -ForegroundColor White
    Write-Host "Backend    : Spring Boot" -ForegroundColor White
    Write-Host "Frontend   : Angular" -ForegroundColor White

    Write-Host ""
    Write-Host "Os terminais do backend e frontend foram abertos." -ForegroundColor Gray
    Write-Host ""
    Write-Host "Para encerrar todo o ambiente:" -ForegroundColor Yellow
    Write-Host "    .\scripts\stop.ps1" -ForegroundColor White
    Write-Host ""

}
catch {

    Write-Host ""
    Write-ErrorMessage $_.Exception.Message
    Write-Host ""

    exit 1
}