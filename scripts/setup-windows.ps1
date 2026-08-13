#Requires -Version 5.1
<#
.SYNOPSIS
    Prepara o ambiente Windows do Financial Control de forma idempotente.

.DESCRIPTION
    Instala ou atualiza somente o que o Environment Contract marcar como
    ausente ou incompativel. Nesta fase: Maven 3.9.x (>= 3.9.12) e
    Node.js 22.x LTS (>= 22.22.3).

    NAO instala PostgreSQL, pgAdmin, Git, JDK, Docker Desktop ou Angular CLI.
    NAO inicia/para Docker e NAO executa docker compose up.
    NAO altera JAVA_HOME.
    NAO executa npm install -g npm@latest.

.NOTES
    Fontes oficiais:
      Maven  https://maven.apache.org/download.cgi
      Node   https://nodejs.org/dist/index.json

    Execucao:
      .\scripts\setup-windows.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
}
catch {
}

$ProjectRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')
$CheckScript = Join-Path $PSScriptRoot 'check-environment.ps1'

$Contract = @{
    MavenLineMajor         = 3
    MavenLineMinor         = 9
    MavenMinPatch          = 12
    MavenPreferredVersion  = '3.9.16'
    NodePreferredMajor     = 22
    NodePreferredMinor     = 22
    NodePreferredPatch     = 3
    NodeAcceptedMajor      = 24
    NodeAcceptedMinor      = 15
    NodeAcceptedPatch      = 0
}

$script:Changes = New-Object 'System.Collections.Generic.List[string]'
$script:AlreadyOk = New-Object 'System.Collections.Generic.List[string]'
$script:Failures = New-Object 'System.Collections.Generic.List[string]'
$script:InstallRoot = $null

# =============================================================================
# Saida
# =============================================================================

function Write-Banner {
    param([string]$Title)
    Write-Host ''
    Write-Host '========================================================' -ForegroundColor Cyan
    Write-Host " $Title" -ForegroundColor Cyan
    Write-Host '========================================================' -ForegroundColor Cyan
    Write-Host ''
}

function Write-Section {
    param([string]$Name)
    Write-Host ''
    Write-Host $Name -ForegroundColor White
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# =============================================================================
# Deteccao (somente leitura)
# =============================================================================

function Get-NativeCommand {
    param([Parameter(Mandatory = $true)][string]$Name)
    Get-Command -Name $Name -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1
}

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FileName,
        [string[]]$ArgumentList = @(),
        [int]$TimeoutSeconds = 20
    )

    $result = [pscustomobject]@{
        Found    = $false
        TimedOut = $false
        ExitCode = $null
        Output   = ''
        Path     = $null
    }

    $commandInfo = Get-NativeCommand -Name $FileName
    if ($null -eq $commandInfo) { return $result }

    $executable = $commandInfo.Source
    if ([string]::IsNullOrWhiteSpace($executable)) { $executable = $commandInfo.Path }
    if ([string]::IsNullOrWhiteSpace($executable)) { return $result }

    $result.Found = $true
    $result.Path = $executable

    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $executable
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.CreateNoWindow = $true
        if ($ArgumentList.Count -gt 0) {
            $quoted = foreach ($argument in $ArgumentList) {
                if ($argument -match '[\s"]') { '"' + ($argument -replace '"', '\"') + '"' }
                else { $argument }
            }
            $psi.Arguments = [string]::Join(' ', [string[]]$quoted)
        }

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $psi
        $null = $process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try { $process.Kill() } catch { }
            $result.TimedOut = $true
            $result.ExitCode = -1
            return $result
        }

        [void][System.Threading.Tasks.Task]::WaitAll(@($stdoutTask, $stderrTask), 2000)
        $result.ExitCode = $process.ExitCode
        $result.Output = (($stdoutTask.Result + "`n" + $stderrTask.Result).Trim())
        return $result
    }
    catch {
        $result.ExitCode = -1
        $result.Output = $_.Exception.Message
        return $result
    }
}

function Get-ParsedVersion {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }

    $major = 0; $minor = 0; $patch = 0; $matched = $false
    if ($Text -match '"(\d+)\.(\d+)(?:\.(\d+))?"') {
        $major = [int]$Matches[1]; $minor = [int]$Matches[2]
        if ($Matches[3]) { $patch = [int]$Matches[3] }
        $matched = $true
    }
    elseif ($Text -match '(?im)(?:^|\s)v?(\d+)\.(\d+)(?:\.(\d+))?') {
        $major = [int]$Matches[1]; $minor = [int]$Matches[2]
        if ($Matches[3]) { $patch = [int]$Matches[3] }
        $matched = $true
    }

    if (-not $matched) { return $null }
    return [pscustomobject]@{
        Major = $major
        Minor = $minor
        Patch = $patch
        Full  = "$major.$minor.$patch"
    }
}

function Test-VersionAtLeast {
    param(
        [Parameter(Mandatory = $true)]$Version,
        [Parameter(Mandatory = $true)][int]$Major,
        [int]$Minor = 0,
        [int]$Patch = 0
    )
    if ($Version.Major -ne $Major) { return ($Version.Major -gt $Major) }
    if ($Version.Minor -ne $Minor) { return ($Version.Minor -gt $Minor) }
    return ($Version.Patch -ge $Patch)
}

function Get-InstalledMaven {
    $command = Invoke-NativeCommand -FileName 'mvn' -ArgumentList @('-version')
    if (-not $command.Found) {
        return [pscustomobject]@{ Found = $false; Version = $null; Home = $null; Path = $null; Output = '' }
    }

    $label = $null
    $installHome = $null
    if ($command.Output -match '(?im)Apache Maven\s+(\d+\.\d+(?:\.\d+)?)') { $label = $Matches[1] }
    if ($command.Output -match '(?im)Maven home:\s*([^\r\n]+)') { $installHome = $Matches[1].Trim() }
    $parsed = if ($label) { Get-ParsedVersion -Text $label } else { Get-ParsedVersion -Text $command.Output }

    return [pscustomobject]@{
        Found   = $true
        Version = $parsed
        Home    = $installHome
        Path    = $command.Path
        Output  = $command.Output
    }
}

function Get-InstalledNode {
    $command = Invoke-NativeCommand -FileName 'node' -ArgumentList @('--version')
    if (-not $command.Found) {
        return [pscustomobject]@{ Found = $false; Version = $null; Path = $null }
    }
    return [pscustomobject]@{
        Found   = $true
        Version = Get-ParsedVersion -Text $command.Output
        Path    = $command.Path
    }
}

function Test-MavenCompliant {
    param($Installed)
    if (-not $Installed.Found -or $null -eq $Installed.Version) { return $false }
    $v = $Installed.Version
    if ($v.Major -ge 4) { return $false }
    if ($v.Major -ne $Contract.MavenLineMajor) { return $false }
    if ($v.Minor -ne $Contract.MavenLineMinor) { return $false }
    return ($v.Patch -ge $Contract.MavenMinPatch)
}

function Test-NodeCompliant {
    param($Installed)
    if (-not $Installed.Found -or $null -eq $Installed.Version) { return $false }
    $v = $Installed.Version
    if ($v.Major -eq $Contract.NodePreferredMajor) {
        return (Test-VersionAtLeast -Version $v -Major $Contract.NodePreferredMajor -Minor $Contract.NodePreferredMinor -Patch $Contract.NodePreferredPatch)
    }
    if ($v.Major -eq $Contract.NodeAcceptedMajor) {
        return (Test-VersionAtLeast -Version $v -Major $Contract.NodeAcceptedMajor -Minor $Contract.NodeAcceptedMinor -Patch $Contract.NodeAcceptedPatch)
    }
    return $false
}

# =============================================================================
# PATH do usuario (nunca Machine, nunca JAVA_HOME)
# =============================================================================

function Get-InstallRoot {
    $dev = 'C:\Dev'
    if (Test-Path -LiteralPath $dev) {
        try {
            $probe = Join-Path $dev ('.fc-write-probe-' + [Guid]::NewGuid().ToString('N'))
            [System.IO.File]::WriteAllText($probe, 'ok')
            Remove-Item -LiteralPath $probe
            return $dev
        }
        catch {
        }
    }

    $fallback = Join-Path $env:LOCALAPPDATA 'FinancialControl\tools'
    if (-not (Test-Path -LiteralPath $fallback)) {
        New-Item -ItemType Directory -Path $fallback | Out-Null
    }
    return $fallback
}

function Update-UserPath {
    param(
        [string[]]$AddPaths = @(),
        [string[]]$RemovePrefixes = @()
    )

    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ($null -eq $userPath) { $userPath = '' }

    $parts = New-Object 'System.Collections.Generic.List[string]'
    foreach ($part in ($userPath -split ';')) {
        $trimmed = $part.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) { continue }

        $shouldRemove = $false
        foreach ($prefix in $RemovePrefixes) {
            if ([string]::IsNullOrWhiteSpace($prefix)) { continue }
            if ($trimmed.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
                $shouldRemove = $true
                break
            }
        }
        if (-not $shouldRemove) {
            [void]$parts.Add($trimmed)
        }
    }

    foreach ($add in $AddPaths) {
        if ([string]::IsNullOrWhiteSpace($add)) { continue }
        $normalized = $add.TrimEnd('\')
        $kept = New-Object 'System.Collections.Generic.List[string]'
        foreach ($existing in $parts) {
            if (-not [string]::Equals($existing.TrimEnd('\'), $normalized, [StringComparison]::OrdinalIgnoreCase)) {
                [void]$kept.Add($existing)
            }
        }
        $parts = $kept
        $parts.Insert(0, $normalized)
    }

    $newUserPath = [string]::Join(';', $parts.ToArray())
    [Environment]::SetEnvironmentVariable('Path', $newUserPath, 'User')

    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    if ($null -eq $machinePath) { $machinePath = '' }
    $sessionParts = New-Object 'System.Collections.Generic.List[string]'
    foreach ($add in $AddPaths) {
        if (-not [string]::IsNullOrWhiteSpace($add)) {
            [void]$sessionParts.Add($add.TrimEnd('\'))
        }
    }
    foreach ($part in @(($machinePath + ';' + $newUserPath) -split ';')) {
        $trimmed = $part.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) { continue }
        $already = $false
        foreach ($existing in $sessionParts) {
            if ([string]::Equals($existing.TrimEnd('\'), $trimmed.TrimEnd('\'), [StringComparison]::OrdinalIgnoreCase)) {
                $already = $true
                break
            }
        }
        if (-not $already) {
            [void]$sessionParts.Add($trimmed)
        }
    }
    $env:Path = [string]::Join(';', $sessionParts.ToArray())
}

# =============================================================================
# Downloads oficiais
# =============================================================================

function Save-OfficialFile {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    $partial = $Destination + '.partial'
    if (Test-Path -LiteralPath $partial) {
        Remove-Item -LiteralPath $partial
    }
    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination
    }

    Write-Info "Baixando: $Url"
    $webClient = New-Object System.Net.WebClient
    try {
        $webClient.DownloadFile($Url, $partial)
        if (-not (Test-Path -LiteralPath $partial) -or ((Get-Item -LiteralPath $partial).Length -le 0)) {
            throw "Download vazio: $Url"
        }
        Move-Item -LiteralPath $partial -Destination $Destination
    }
    catch {
        if (Test-Path -LiteralPath $partial) {
            Remove-Item -LiteralPath $partial -ErrorAction SilentlyContinue
        }
        throw
    }
    finally {
        $webClient.Dispose()
    }
}

function Get-TextFromUrl {
    param([Parameter(Mandatory = $true)][string]$Url)
    $webClient = New-Object System.Net.WebClient
    $webClient.Encoding = [System.Text.Encoding]::UTF8
    try {
        return $webClient.DownloadString($Url)
    }
    finally {
        $webClient.Dispose()
    }
}

function Test-UrlExists {
    param([Parameter(Mandatory = $true)][string]$Url)
    try {
        $request = [System.Net.WebRequest]::Create($Url)
        $request.Method = 'HEAD'
        $request.Timeout = 20000
        $response = $request.GetResponse()
        $status = [int]$response.StatusCode
        $response.Close()
        return ($status -ge 200 -and $status -lt 400)
    }
    catch {
        try {
            $get = [System.Net.WebRequest]::Create($Url)
            $get.Method = 'GET'
            $get.Timeout = 20000
            $getResponse = $get.GetResponse()
            $statusGet = [int]$getResponse.StatusCode
            $getResponse.Close()
            return ($statusGet -ge 200 -and $statusGet -lt 400)
        }
        catch {
            return $false
        }
    }
}

function Get-OfficialMavenArtifact {
    $preferred = $Contract.MavenPreferredVersion
    $bases = @(
        "https://dlcdn.apache.org/maven/maven-3/$preferred/binaries",
        "https://downloads.apache.org/maven/maven-3/$preferred/binaries",
        "https://archive.apache.org/dist/maven/maven-3/$preferred/binaries"
    )

    $zipName = "apache-maven-$preferred-bin.zip"
    $selectedBase = $null
    foreach ($base in $bases) {
        $zipUrl = "$base/$zipName"
        Write-Info "Verificando fonte Maven: $zipUrl"
        if (Test-UrlExists -Url $zipUrl) {
            $selectedBase = $base
            break
        }
    }

    if ([string]::IsNullOrWhiteSpace($selectedBase)) {
        throw "Maven $preferred nao encontrado nas fontes oficiais da Apache. Nenhuma URL inventada sera utilizada."
    }

    $shaUrl = "$selectedBase/$zipName.sha512"
    $shaText = Get-TextFromUrl -Url $shaUrl
    $sha = $null
    if ($shaText -match '([A-Fa-f0-9]{128})') {
        $sha = $Matches[1].ToLowerInvariant()
    }
    if ([string]::IsNullOrWhiteSpace($sha)) {
        throw "Checksum SHA512 oficial do Maven $preferred nao pode ser lido ($shaUrl)."
    }

    return [pscustomobject]@{
        Version    = $preferred
        ZipUrl     = "$selectedBase/$zipName"
        Sha512     = $sha
        FolderName = "apache-maven-$preferred"
    }
}

function Get-OfficialNode22Artifact {
    Write-Info 'Consultando indice oficial https://nodejs.org/dist/index.json'
    $index = Invoke-RestMethod -Uri 'https://nodejs.org/dist/index.json'
    if ($null -eq $index) {
        throw 'Falha ao ler o indice oficial do Node.js.'
    }

    $isArm = ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64' -or $env:PROCESSOR_ARCHITEW6432 -eq 'ARM64')
    $fileTag = if ($isArm) { 'win-arm64-zip' } else { 'win-x64-zip' }
    $zipSuffix = if ($isArm) { 'win-arm64.zip' } else { 'win-x64.zip' }

    $best = $null
    foreach ($entry in $index) {
        if ($entry.version -notmatch '^v(22)\.(\d+)\.(\d+)$') { continue }
        if (@($entry.files) -notcontains $fileTag) { continue }
        $candidate = Get-ParsedVersion -Text $entry.version
        if ($null -eq $candidate) { continue }
        if (-not (Test-VersionAtLeast -Version $candidate -Major $Contract.NodePreferredMajor -Minor $Contract.NodePreferredMinor -Patch $Contract.NodePreferredPatch)) {
            continue
        }
        if ($null -eq $best -or (Test-VersionAtLeast -Version $candidate -Major $best.Major -Minor $best.Minor -Patch $best.Patch)) {
            $best = $candidate
        }
    }

    if ($null -eq $best) {
        throw 'Nenhuma build oficial Node.js 22.x LTS (>= 22.22.3) com zip Windows foi encontrada no indice.'
    }

    $versionTag = "v$($best.Full)"
    $zipName = "node-$versionTag-$zipSuffix"
    $zipUrl = "https://nodejs.org/dist/$versionTag/$zipName"
    $sumUrl = "https://nodejs.org/dist/$versionTag/SHASUMS256.txt"

    if (-not (Test-UrlExists -Url $zipUrl)) {
        throw "Pacote oficial nao encontrado: $zipUrl"
    }

    $sums = Get-TextFromUrl -Url $sumUrl
    $sha = $null
    foreach ($line in ($sums -split '\r?\n')) {
        if ($line -match ('^([A-Fa-f0-9]{64})\s+' + [regex]::Escape($zipName) + '\s*$')) {
            $sha = $Matches[1].ToLowerInvariant()
            break
        }
    }
    if ([string]::IsNullOrWhiteSpace($sha)) {
        throw "Checksum SHA256 oficial de $zipName nao encontrado em $sumUrl"
    }

    return [pscustomobject]@{
        Version    = $best.Full
        ZipUrl     = $zipUrl
        Sha256     = $sha
        ZipName    = $zipName
        FolderName = "node-$versionTag-$($zipSuffix.Replace('.zip', ''))"
    }
}

function Assert-FileHash {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][ValidateSet('SHA256', 'SHA512')][string]$Algorithm
    )

    $actual = (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash.ToLowerInvariant()
    if ($actual -ne $Expected.ToLowerInvariant()) {
        throw "Checksum $Algorithm invalido para $(Split-Path -Leaf $Path). Esperado $Expected, obtido $actual. O arquivo sera descartado."
    }
}

function Expand-OfficialZip {
    param(
        [Parameter(Mandatory = $true)][string]$ZipPath,
        [Parameter(Mandatory = $true)][string]$DestinationParent,
        [Parameter(Mandatory = $true)][string]$ExpectedFolderName
    )

    $expectedPath = Join-Path $DestinationParent $ExpectedFolderName
    $staging = Join-Path $DestinationParent ('.fc-extract-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $staging | Out-Null

    try {
        Expand-Archive -LiteralPath $ZipPath -DestinationPath $staging
        $extracted = Get-ChildItem -LiteralPath $staging -Directory | Select-Object -First 1
        if ($null -eq $extracted) {
            throw "O zip nao continha um diretorio extraido esperado ($ExpectedFolderName)."
        }

        if (Test-Path -LiteralPath $expectedPath) {
            Remove-Item -LiteralPath $expectedPath -Recurse
        }
        Move-Item -LiteralPath $extracted.FullName -Destination $expectedPath
        return $expectedPath
    }
    catch {
        if (Test-Path -LiteralPath $expectedPath) {
            Remove-Item -LiteralPath $expectedPath -Recurse -ErrorAction SilentlyContinue
        }
        throw
    }
    finally {
        if (Test-Path -LiteralPath $staging) {
            Remove-Item -LiteralPath $staging -Recurse -ErrorAction SilentlyContinue
        }
    }
}

# =============================================================================
# Instaladores
# =============================================================================

function Install-OfficialMaven {
    $tempDir = $null
    try {
        $artifact = Get-OfficialMavenArtifact
        Write-Info "Maven oficial selecionado: $($artifact.Version) (Apache, linha 3.9.x atual)"

        $targetHome = Join-Path $script:InstallRoot $artifact.FolderName
        $targetBin = Join-Path $targetHome 'bin'
        $mvnCmd = Join-Path $targetBin 'mvn.cmd'

        if (Test-Path -LiteralPath $mvnCmd) {
            Write-Info "Maven $($artifact.Version) ja extraido em $targetHome. Apenas o PATH do usuario sera ajustado."
            $installHome = $targetHome
        }
        else {
            $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ('fc-maven-' + [Guid]::NewGuid().ToString('N'))
            New-Item -ItemType Directory -Path $tempDir | Out-Null
            $zipPath = Join-Path $tempDir ($artifact.FolderName + '-bin.zip')

            Save-OfficialFile -Url $artifact.ZipUrl -Destination $zipPath
            Assert-FileHash -Path $zipPath -Expected $artifact.Sha512 -Algorithm SHA512
            Write-Ok 'Checksum SHA512 do Maven verificado.'

            $installHome = Expand-OfficialZip -ZipPath $zipPath -DestinationParent $script:InstallRoot -ExpectedFolderName $artifact.FolderName
            if (-not (Test-Path -LiteralPath $mvnCmd)) {
                throw "Instalacao Maven incompleta: $mvnCmd nao existe. O diretorio novo sera removido."
            }
        }

        $removePrefixes = @()
        $installed = Get-InstalledMaven
        if ($installed.Found -and -not [string]::IsNullOrWhiteSpace($installed.Home)) {
            $removePrefixes += $installed.Home
        }
        $removePrefixes += (Join-Path $script:InstallRoot 'apache-maven-3.9.11')

        Update-UserPath -AddPaths @($targetBin) -RemovePrefixes $removePrefixes

        $verify = Invoke-NativeCommand -FileName $mvnCmd -ArgumentList @('-version')
        $parsed = $null
        if ($verify.Output -match '(?im)Apache Maven\s+(\d+\.\d+(?:\.\d+)?)') {
            $parsed = Get-ParsedVersion -Text $Matches[1]
        }
        if ($null -eq $parsed -or $parsed.Full -ne $artifact.Version) {
            throw "Maven extraido, mas $mvnCmd nao reportou $($artifact.Version)."
        }

        [void]$script:Changes.Add("Maven atualizado para $($artifact.Version) em $installHome")
        Write-Ok "Maven $($artifact.Version) disponivel em $installHome"
        Write-Info 'O diretorio Maven anterior nao foi removido; apenas o PATH do usuario passou a apontar para a 3.9.x nova.'
    }
    catch {
        $message = $_.Exception.Message
        [void]$script:Failures.Add("Maven: $message")
        Write-Err $message
        $partialHome = Join-Path $script:InstallRoot ('apache-maven-' + $Contract.MavenPreferredVersion)
        $partialBin = Join-Path $partialHome 'bin\mvn.cmd'
        if ((Test-Path -LiteralPath $partialHome) -and -not (Test-Path -LiteralPath $partialBin)) {
            Remove-Item -LiteralPath $partialHome -Recurse -ErrorAction SilentlyContinue
            Write-Info 'Diretorio Maven incompleto removido.'
        }
    }
    finally {
        if ($tempDir -and (Test-Path -LiteralPath $tempDir)) {
            Remove-Item -LiteralPath $tempDir -Recurse -ErrorAction SilentlyContinue
        }
    }
}

function Install-OfficialNode {
    $tempDir = $null
    $artifact = $null
    try {
        $artifact = Get-OfficialNode22Artifact
        Write-Info "Node.js oficial selecionado: $($artifact.Version) (nodejs.org, linha 22.x LTS)"

        $targetHome = Join-Path $script:InstallRoot $artifact.FolderName
        $nodeExe = Join-Path $targetHome 'node.exe'
        $npmCmd = Join-Path $targetHome 'npm.cmd'

        if ((Test-Path -LiteralPath $nodeExe) -and (Test-Path -LiteralPath $npmCmd)) {
            Write-Info "Node.js $($artifact.Version) ja extraido em $targetHome. Apenas o PATH do usuario sera ajustado."
            $installHome = $targetHome
        }
        else {
            $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ('fc-node-' + [Guid]::NewGuid().ToString('N'))
            New-Item -ItemType Directory -Path $tempDir | Out-Null
            $zipPath = Join-Path $tempDir $artifact.ZipName

            Save-OfficialFile -Url $artifact.ZipUrl -Destination $zipPath
            Assert-FileHash -Path $zipPath -Expected $artifact.Sha256 -Algorithm SHA256
            Write-Ok 'Checksum SHA256 do Node.js verificado.'

            $installHome = Expand-OfficialZip -ZipPath $zipPath -DestinationParent $script:InstallRoot -ExpectedFolderName $artifact.FolderName
            if (-not (Test-Path -LiteralPath $nodeExe) -or -not (Test-Path -LiteralPath $npmCmd)) {
                throw "Instalacao Node.js incompleta em $installHome. O diretorio novo sera removido."
            }
        }

        Update-UserPath -AddPaths @($installHome) -RemovePrefixes @()

        $verify = Invoke-NativeCommand -FileName $nodeExe -ArgumentList @('--version')
        $parsed = Get-ParsedVersion -Text $verify.Output
        if ($null -eq $parsed -or $parsed.Full -ne $artifact.Version) {
            throw "Node.js extraido, mas $nodeExe nao reportou v$($artifact.Version)."
        }

        $npmVerify = Invoke-NativeCommand -FileName $npmCmd -ArgumentList @('--version')
        if (-not $npmVerify.Found) {
            throw 'Node.js instalado, mas npm nao foi encontrado no mesmo pacote oficial.'
        }

        [void]$script:Changes.Add("Node.js $($artifact.Version) instalado em $installHome (npm $($npmVerify.Output.Trim()) empacotado)")
        Write-Ok "Node.js $($artifact.Version) disponivel em $installHome"
        Write-Info 'npm e o cliente empacotado com este Node.js. npm@latest global nao foi executado.'
    }
    catch {
        $message = $_.Exception.Message
        [void]$script:Failures.Add("Node.js: $message")
        Write-Err $message
        if ($null -ne $artifact -and -not [string]::IsNullOrWhiteSpace($artifact.FolderName)) {
            $partialHome = Join-Path $script:InstallRoot $artifact.FolderName
            $partialExe = Join-Path $partialHome 'node.exe'
            if ((Test-Path -LiteralPath $partialHome) -and -not (Test-Path -LiteralPath $partialExe)) {
                Remove-Item -LiteralPath $partialHome -Recurse -ErrorAction SilentlyContinue
                Write-Info 'Diretorio Node.js incompleto removido.'
            }
        }
    }
    finally {
        if ($tempDir -and (Test-Path -LiteralPath $tempDir)) {
            Remove-Item -LiteralPath $tempDir -Recurse -ErrorAction SilentlyContinue
        }
    }
}

function Invoke-EnvironmentCheck {
    if (-not (Test-Path -LiteralPath $CheckScript)) {
        Write-Err "Script de diagnostico nao encontrado: $CheckScript"
        return 1
    }

    $process = Start-Process -FilePath 'powershell.exe' `
        -ArgumentList @('-NoProfile', '-File', $CheckScript) `
        -WorkingDirectory $ProjectRoot.Path `
        -Wait -PassThru -NoNewWindow
    return $process.ExitCode
}

# =============================================================================
# Execucao
# =============================================================================

Write-Banner 'Financial Control - Windows Setup'
Write-Host "Projeto: $ProjectRoot"
Write-Host 'Escopo: Maven 3.9.x (>= 3.9.12, preferencia 3.9.16) e Node.js 22.x LTS.'
Write-Host 'Este script NAO instala PostgreSQL, pgAdmin, Git, JDK, Docker ou Angular CLI.'
Write-Host 'JAVA_HOME nao sera alterado. Docker nao sera iniciado nem parado.'

Write-Section 'Diagnostico atual (somente leitura)'
$initialExit = Invoke-EnvironmentCheck
Write-Info ("Diagnostico inicial concluido (exit $initialExit). Em seguida so Maven/Node serao avaliados para instalacao.")

Write-Section 'Plano de acao'
$script:InstallRoot = Get-InstallRoot
Write-Info "Diretorio de instalacao: $($script:InstallRoot)"

$maven = Get-InstalledMaven
$node = Get-InstalledNode
$needMaven = -not (Test-MavenCompliant -Installed $maven)
$needNode = -not (Test-NodeCompliant -Installed $node)

if ($maven.Found -and $null -ne $maven.Version) {
    Write-Info "Maven detectado: $($maven.Version.Full) ($($maven.Path))"
}
else {
    Write-Info 'Maven nao detectado no PATH.'
}

if ($node.Found -and $null -ne $node.Version) {
    Write-Info "Node.js detectado: $($node.Version.Full) ($($node.Path))"
}
else {
    Write-Info 'Node.js nao detectado no PATH.'
}

if (-not $needMaven) {
    [void]$script:AlreadyOk.Add("Maven $($maven.Version.Full) ja atende 3.9.x >= 3.9.12")
    Write-Ok "Maven $($maven.Version.Full) ja esta correto. Nenhuma acao."
}
elseif ($maven.Found -and $null -ne $maven.Version -and $maven.Version.Major -ge 4) {
    [void]$script:Failures.Add('Maven 4 detectado. O contrato exige 3.9.x; o setup nao instala nem substitui Maven 4 automaticamente.')
    Write-Err 'Maven 4 detectado. Nenhuma alteracao de Maven sera feita.'
    $needMaven = $false
}

if (-not $needNode) {
    $label = if ($node.Version.Major -eq 24) {
        "Node.js $($node.Version.Full) (24.x aceito; sem downgrade)"
    }
    else {
        "Node.js $($node.Version.Full) ja atende 22.x LTS >= 22.22.3"
    }
    [void]$script:AlreadyOk.Add($label)
    Write-Ok "$label. Nenhuma acao."
}
elseif ($node.Found -and $null -ne $node.Version -and $node.Version.Major -eq 26) {
    Write-Warn "Node.js $($node.Version.Full) e Current (nao LTS). O setup instalara Node.js 22.x LTS e colocara no PATH do usuario, sem remover a instalacao Current."
}

[void]$script:AlreadyOk.Add('Git, Java/JDK, Docker Desktop e Docker Compose nao serao alterados por este script.')
[void]$script:AlreadyOk.Add('PostgreSQL/pgAdmin Windows nao serao instalados. O banco oficial permanece postgres:18-alpine via Docker.')
[void]$script:AlreadyOk.Add('Angular CLI global nao sera instalada.')

if ($needMaven) {
    Write-Section 'Maven'
    Install-OfficialMaven
}

if ($needNode) {
    Write-Section 'Node.js'
    Install-OfficialNode
}

if (-not $needMaven -and -not $needNode -and $script:Failures.Count -eq 0) {
    Write-Info 'Nenhuma instalacao necessaria.'
}

Write-Section 'Ferramentas que ja estavam corretas'
if ($script:AlreadyOk.Count -eq 0) {
    Write-Info 'Nenhuma.'
}
else {
    foreach ($item in $script:AlreadyOk) {
        Write-Ok $item
    }
}

Write-Section 'Alteracoes realizadas'
if ($script:Changes.Count -eq 0) {
    Write-Info 'Nenhuma alteracao de instalacao/PATH foi realizada.'
}
else {
    foreach ($item in $script:Changes) {
        Write-Host "- $item" -ForegroundColor Green
    }
}

if ($script:Failures.Count -gt 0) {
    Write-Section 'Falhas'
    foreach ($item in $script:Failures) {
        Write-Err $item
    }
    Write-Warn 'O ambiente pode estar incompleto. Corrija a falha e execute novamente este script (ele e idempotente).'
}

Write-Section 'Diagnostico final'
$finalExit = Invoke-EnvironmentCheck

Write-Banner 'SETUP RESULTADO'
if ($script:Failures.Count -gt 0) {
    Write-Err 'Setup terminou com falha em uma ou mais instalacoes.'
    exit 1
}
if ($finalExit -ne 0) {
    Write-Err 'Setup executado, mas o diagnostico final ainda reporta ERROR.'
    exit 1
}

Write-Ok 'Setup concluido. Ambiente de acordo com o Environment Contract.'
exit 0
