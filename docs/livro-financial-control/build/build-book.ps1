[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

function Find-RepositoryRoot {
    param([Parameter(Mandatory)][string]$StartPath)

    $current = [System.IO.DirectoryInfo]::new((Resolve-Path -LiteralPath $StartPath).Path)
    while ($null -ne $current) {
        $frontendPackage = Join-Path $current.FullName 'frontend\package.json'
        $bookReadme = Join-Path $current.FullName 'docs\livro-financial-control\README.md'
        if ((Test-Path -LiteralPath $frontendPackage -PathType Leaf) -and
            (Test-Path -LiteralPath $bookReadme -PathType Leaf)) {
            return $current.FullName
        }
        $current = $current.Parent
    }

    throw "Não foi possível localizar a raiz do repositório a partir de '$StartPath'."
}

$bookRoot = Split-Path -Parent $PSScriptRoot
if ((Split-Path -Leaf $PSScriptRoot) -ne 'build') {
    # Permite validar o script antes de ele ser movido para build/.
    $bookRoot = $PSScriptRoot
}

$repositoryRoot = Find-RepositoryRoot -StartPath $bookRoot
$bookRoot = Join-Path $repositoryRoot 'docs\livro-financial-control'
$frontendRoot = Join-Path $repositoryRoot 'frontend'
$manuscriptRoot = Join-Path $bookRoot 'manuscrito'
$renderer = Join-Path $bookRoot 'build\render-book.cjs'
$distRoot = Join-Path $bookRoot 'dist'

if (-not (Test-Path -LiteralPath $manuscriptRoot -PathType Container)) {
    throw "Diretório de manuscrito não encontrado: $manuscriptRoot"
}

$chapters = @(
    Get-ChildItem -LiteralPath $manuscriptRoot -File -Filter '*.md' |
        Sort-Object { [regex]::Replace($_.Name, '\d+', { param($match) $match.Value.PadLeft(12, '0') }) }
)
if ($chapters.Count -eq 0) {
    throw "Nenhum capítulo Markdown foi encontrado em '$manuscriptRoot'."
}

$node = Get-Command node -ErrorAction SilentlyContinue
if ($null -eq $node) {
    throw 'Node.js não foi encontrado no PATH.'
}

$playwrightPackage = Join-Path $frontendRoot 'node_modules\playwright\package.json'
$playwrightTestPackage = Join-Path $frontendRoot 'node_modules\@playwright\test\package.json'
if (-not (Test-Path -LiteralPath $playwrightPackage -PathType Leaf) -and
    -not (Test-Path -LiteralPath $playwrightTestPackage -PathType Leaf)) {
    throw "Playwright não está instalado em '$frontendRoot\node_modules'. Execute 'npm install' no frontend."
}

if (-not (Test-Path -LiteralPath $renderer -PathType Leaf)) {
    throw "Renderer do livro não encontrado: $renderer"
}

New-Item -ItemType Directory -Force -Path $distRoot | Out-Null

Write-Host "Raiz do repositório: $repositoryRoot"
Write-Host "Capítulos encontrados ($($chapters.Count)):"
$chapters | ForEach-Object { Write-Host "  - $($_.Name)" }

& $node.Source $renderer --book-root $bookRoot --frontend-root $frontendRoot
if ($LASTEXITCODE -ne 0) {
    throw "O renderer terminou com código $LASTEXITCODE."
}

$html = Join-Path $distRoot 'financial-control-livro.html'
$pdf = Join-Path $distRoot 'financial-control-livro.pdf'
$report = Join-Path $distRoot 'validation-report.json'
foreach ($artifact in @($html, $pdf, $report)) {
    if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
        throw "Artefato esperado não foi criado: $artifact"
    }
    if ((Get-Item -LiteralPath $artifact).Length -eq 0) {
        throw "Artefato vazio: $artifact"
    }
}

$validation = Get-Content -LiteralPath $report -Raw -Encoding UTF8 | ConvertFrom-Json
if (-not $validation.pdfHeader -or -not $validation.pdfEof) {
    throw 'O PDF falhou na validação estrutural registrada pelo renderer.'
}
if ($validation.physicalPages -lt 2) {
    throw "PDF com número insuficiente de páginas: $($validation.physicalPages)"
}
if ($validation.emptyPages.Count -gt 0 -or $validation.overflowPages.Count -gt 0) {
    throw 'O relatório registrou páginas vazias ou conteúdo ultrapassando a área útil.'
}
if ($validation.pdfPageObjects -ne $validation.physicalPages) {
    throw 'A quantidade real de páginas do PDF difere do HTML paginado.'
}
if ($validation.outOfBoundsElements.Count -gt 0 -or $validation.orphanHeadings.Count -gt 0) {
    throw 'A inspeção registrou elementos cortados ou títulos órfãos.'
}

Write-Host ''
Write-Host 'Livro gerado e validado estruturalmente.'
Write-Host "HTML: $html"
Write-Host "PDF:  $pdf"
Write-Host "Páginas: $($validation.physicalPages)"

