#Requires -Version 5.1

<#
.SYNOPSIS
    Carrega o arquivo .env da raiz do projeto na sessao atual.

.DESCRIPTION
    Destinado a ser chamado com dot-source:

        . .\scripts\import-dotenv.ps1
        . .\scripts\import-dotenv.ps1 -ProjectRoot 'D:\Financeiro'

    Nao imprime valores. Sem -Override, nao sobrescreve variaveis ja definidas
    na sessao. Com -Override, o .env local prevalece (usado por start.ps1 e
    run-backend.ps1). Exige JWT_SECRET com no minimo 32 bytes (HS256).
#>

param (
    [string]$ProjectRoot = "",
    [switch]$Override
)

$previousEap = $ErrorActionPreference
$ErrorActionPreference = "Stop"

try {

    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $scriptDir = $PSScriptRoot
        if ([string]::IsNullOrWhiteSpace($scriptDir) -and $MyInvocation.MyCommand.Path) {
            $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
        }
        if ([string]::IsNullOrWhiteSpace($scriptDir)) {
            throw "Nao foi possivel localizar scripts\import-dotenv.ps1. Informe -ProjectRoot."
        }
        $ProjectRoot = Split-Path -Parent $scriptDir
    }

    $DotEnvFile = Join-Path $ProjectRoot ".env"

    if (-not (Test-Path -LiteralPath $DotEnvFile)) {
        throw ".env nao encontrado em $ProjectRoot. Copie .env.example para .env e defina JWT_SECRET (minimo 32 bytes), ou execute scripts\start.ps1."
    }

    function Import-DotEnvFile {
        param (
            [Parameter(Mandatory = $true)]
            [string]$Path
        )

        $lines = Get-Content -LiteralPath $Path

        foreach ($rawLine in $lines) {
            if ($null -eq $rawLine) {
                continue
            }

            $line = $rawLine.Trim()

            if ($line -eq "" -or $line.StartsWith("#")) {
                continue
            }

            $separatorIndex = $line.IndexOf("=")
            if ($separatorIndex -lt 1) {
                continue
            }

            $name = $line.Substring(0, $separatorIndex).Trim()
            $value = $line.Substring($separatorIndex + 1).Trim()

            if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
                continue
            }

            if (
                ($value.StartsWith('"') -and $value.EndsWith('"') -and $value.Length -ge 2) -or
                ($value.StartsWith("'") -and $value.EndsWith("'") -and $value.Length -ge 2)
            ) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            $existing = [Environment]::GetEnvironmentVariable($name, "Process")
            if (-not $Override -and -not [string]::IsNullOrEmpty($existing)) {
                continue
            }

            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }

    Import-DotEnvFile -Path $DotEnvFile

    $JwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")

    if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
        throw "JWT_SECRET nao esta definido no .env. Informe um valor com no minimo 32 bytes."
    }

    $JwtSecretBytes = [System.Text.Encoding]::UTF8.GetByteCount($JwtSecret)

    if ($JwtSecretBytes -lt 32) {
        throw "JWT_SECRET must be at least 32 bytes for HS256 (atual: $JwtSecretBytes). Atualize o arquivo .env local; o valor nao sera exibido."
    }

    Write-Host "[OK] Variaveis de ambiente carregadas de .env." -ForegroundColor Green

}
finally {
    $ErrorActionPreference = $previousEap
}
