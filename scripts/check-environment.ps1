#Requires -Version 5.1
<#
.SYNOPSIS
    Diagnostico somente leitura do ambiente de desenvolvimento Windows
    do Financial Control.

.DESCRIPTION
    Compara as ferramentas instaladas com o Environment Contract oficial
    (docs/22-stack-tecnologica.md, secao 30).

    Este script NAO instala, atualiza, remove ou modifica software,
    servicos, PATH, variaveis de ambiente, Docker, PostgreSQL ou
    arquivos do projeto.

.NOTES
    Execucao:
      .\scripts\check-environment.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# -----------------------------------------------------------------------------
# Environment Contract (docs/22-stack-tecnologica.md)
# -----------------------------------------------------------------------------

$Official = @{
    JavaMajor              = 25
    JavaLabel              = '25 LTS'
    AngularMajor           = 22
    AngularLabel           = '22.x'
    NodePreferredMajor     = 22
    NodePreferredMinor     = 22
    NodePreferredPatch     = 3
    NodeAcceptedMajor      = 24
    NodeAcceptedMinor      = 15
    NodeAcceptedPatch      = 0
    MavenMajor             = 3
    MavenMinor             = 9
    MavenPatch             = 12
    MavenLabel             = '3.9.x (>= 3.9.12)'
    GitMajor               = 2
    GitMinor               = 39
    GitPatch               = 0
    GitLabel               = '>= 2.39'
    DockerEngineMajor      = 24
    DockerEngineLabel      = '>= 24'
    ComposeMajor           = 2
    ComposeMinor           = 24
    ComposePatch           = 0
    ComposeLabel           = 'V2 >= 2.24'
    PostgresImage          = 'postgres:18-alpine'
    PostgresLabel          = '18-alpine via Docker'
}

$ProjectRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')
$script:Findings = New-Object 'System.Collections.Generic.List[object]'
$script:NodeFoundLabel = $null

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

function Get-StatusColor {
    param([string]$Status)
    switch ($Status) {
        'OK'      { 'Green' }
        'WARNING' { 'Yellow' }
        'ERROR'   { 'Red' }
        default   { 'Cyan' }
    }
}

function Add-Finding {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Tool,
        [Parameter(Mandatory = $true)]
        [ValidateSet('OK', 'WARNING', 'ERROR', 'INFO')]
        [string]$Status,
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [string]$Expected = ''
    )

    $item = [pscustomobject]@{
        Tool     = $Tool
        Status   = $Status
        Message  = $Message
        Expected = $Expected
    }
    [void]$script:Findings.Add($item)

    Write-Host "[$Status] $Message" -ForegroundColor (Get-StatusColor -Status $Status)
    if (-not [string]::IsNullOrWhiteSpace($Expected)) {
        Write-Host "        Esperado: $Expected"
    }
}

function Invoke-Check {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    try {
        & $Action
    }
    catch {
        Add-Finding -Tool $Name -Status ERROR -Message ("Falha inesperada ao verificar ${Name}: $($_.Exception.Message)")
    }
}

# =============================================================================
# Comandos nativos (somente leitura)
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
            [void][System.Threading.Tasks.Task]::WaitAll(@($stdoutTask, $stderrTask), 1000)
            $stdoutText = ''
            $stderrText = ''
            if ($stdoutTask.IsCompleted) { $stdoutText = $stdoutTask.Result }
            if ($stderrTask.IsCompleted) { $stderrText = $stderrTask.Result }
            $result.Output = (($stdoutText + "`n" + $stderrText).Trim())
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

function Get-FirstNonEmptyLine {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return '' }
    foreach ($line in ($Text -split '\r?\n')) {
        $trimmed = $line.Trim()
        if (-not [string]::IsNullOrWhiteSpace($trimmed)) { return $trimmed }
    }
    return ''
}

function Get-ParsedVersion {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }

    $major = 0
    $minor = 0
    $patch = 0
    $matched = $false

    if ($Text -match '"1\.(\d+)\.(\d+)') {
        $major = [int]$Matches[1]; $minor = [int]$Matches[2]; $matched = $true
    }
    elseif ($Text -match '"(\d+)\.(\d+)(?:\.(\d+))?"') {
        $major = [int]$Matches[1]; $minor = [int]$Matches[2]
        if ($Matches[3]) { $patch = [int]$Matches[3] }
        $matched = $true
    }
    elseif ($Text -match '(?im)(?:^|\s)v?(\d+)\.(\d+)(?:\.(\d+))?') {
        $major = [int]$Matches[1]; $minor = [int]$Matches[2]
        if ($Matches[3]) { $patch = [int]$Matches[3] }
        $matched = $true
    }
    elseif ($Text -match '(?im)(?:^|\s)v?(\d+)\b') {
        $major = [int]$Matches[1]; $matched = $true
    }

    if (-not $matched) { return $null }

    $full = "$major.$minor.$patch"
    return [pscustomobject]@{
        Major = $major
        Minor = $minor
        Patch = $patch
        Full  = $full
    }
}

function Test-VersionAtLeast {
    param(
        [Parameter(Mandatory = $true)]$Version,
        [Parameter(Mandatory = $true)][int]$Major,
        [int]$Minor = 0,
        [int]$Patch = 0
    )

    if ($Version.Major -ne $Major) {
        return ($Version.Major -gt $Major)
    }
    if ($Version.Minor -ne $Minor) {
        return ($Version.Minor -gt $Minor)
    }
    return ($Version.Patch -ge $Patch)
}

function Get-ComposeFilePath {
    foreach ($name in @('docker-compose.yml', 'docker-compose.yaml', 'compose.yml', 'compose.yaml')) {
        $path = Join-Path $ProjectRoot $name
        if (Test-Path -LiteralPath $path) { return $path }
    }
    return $null
}

function Get-PackageJsonPath {
    foreach ($path in @((Join-Path $ProjectRoot 'frontend\package.json'), (Join-Path $ProjectRoot 'package.json'))) {
        if (Test-Path -LiteralPath $path) { return $path }
    }
    return $null
}

function Get-MavenWrapperInfo {
    $candidates = @(
        (Join-Path $ProjectRoot 'backend\mvnw.cmd'),
        (Join-Path $ProjectRoot 'mvnw.cmd')
    )
    $wrapperCmd = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $wrapperCmd) {
        return $null
    }

    $propsCandidates = @(
        (Join-Path $ProjectRoot 'backend\.mvn\wrapper\maven-wrapper.properties'),
        (Join-Path $ProjectRoot '.mvn\wrapper\maven-wrapper.properties')
    )
    $propsPath = $propsCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    $wrapperVersion = $null
    if ($propsPath) {
        $propsText = Get-Content -LiteralPath $propsPath -Raw -ErrorAction SilentlyContinue
        if ($propsText -match 'apache-maven-(\d+\.\d+\.\d+)') {
            $wrapperVersion = $Matches[1]
        }
    }

    return [pscustomobject]@{
        Command = $wrapperCmd
        PropertiesPath = $propsPath
        Version = $wrapperVersion
    }
}

function Get-JsonProperty {
    param([object]$Object, [string[]]$Names)
    if ($null -eq $Object) { return $null }
    foreach ($name in $Names) {
        $property = $Object.PSObject.Properties[$name]
        if ($null -ne $property -and $null -ne $property.Value) { return $property.Value }
    }
    return $null
}

# =============================================================================
# Verificacoes
# =============================================================================

function Test-OperatingSystem {
    Write-Section 'Sistema operacional'
    try {
        $os = Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction Stop
        $caption = $os.Caption.Trim()
        $build = 0
        [void][int]::TryParse($os.BuildNumber, [ref]$build)
        $edition = if ($build -gt 0) { "$caption (build $build)" } else { $caption }
        Add-Finding -Tool 'Sistema' -Status OK -Message $edition
    }
    catch {
        Add-Finding -Tool 'Sistema' -Status WARNING -Message ("Nao foi possivel obter detalhes do SO. " + [System.Environment]::OSVersion.VersionString)
    }
    Add-Finding -Tool 'PowerShell' -Status INFO -Message ("PowerShell " + $PSVersionTable.PSVersion.ToString())
}

function Test-Git {
    Write-Section 'Git'
    $expected = $Official.GitLabel
    $command = Invoke-NativeCommand -FileName 'git' -ArgumentList @('--version')

    if (-not $command.Found) {
        Add-Finding -Tool 'Git' -Status ERROR -Message 'Git nao encontrado.' -Expected $expected
        return
    }
    if ($command.TimedOut) {
        Add-Finding -Tool 'Git' -Status ERROR -Message 'git --version excedeu o tempo limite.' -Expected $expected
        return
    }

    $parsed = Get-ParsedVersion -Text $command.Output
    if ($null -eq $parsed) {
        Add-Finding -Tool 'Git' -Status WARNING -Message ("Git encontrado, mas a versao nao foi identificada: " + (Get-FirstNonEmptyLine -Text $command.Output)) -Expected $expected
        return
    }

    if (Test-VersionAtLeast -Version $parsed -Major $Official.GitMajor -Minor $Official.GitMinor -Patch $Official.GitPatch) {
        Add-Finding -Tool 'Git' -Status OK -Message "Git encontrado: $($parsed.Full)" -Expected $expected
    }
    else {
        Add-Finding -Tool 'Git' -Status ERROR -Message "Git incompativel: $($parsed.Full)" -Expected $expected
    }
    Add-Finding -Tool 'Git' -Status INFO -Message "Caminho: $($command.Path)"
}

function Test-Java {
    Write-Section 'Java / JDK'
    $expected = $Official.JavaLabel
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME')
    $javaCmd = Invoke-NativeCommand -FileName 'java' -ArgumentList @('-version')
    $javacCmd = Invoke-NativeCommand -FileName 'javac' -ArgumentList @('-version')

    if (-not $javaCmd.Found -and -not $javacCmd.Found) {
        Add-Finding -Tool 'Java' -Status ERROR -Message 'Java/JDK nao encontrado.' -Expected $expected
        if (-not [string]::IsNullOrWhiteSpace($javaHome)) {
            Add-Finding -Tool 'Java' -Status INFO -Message "JAVA_HOME esta definido, mas java/javac nao estao no PATH: $javaHome"
        }
        return
    }

    if (-not $javaCmd.Found) {
        Add-Finding -Tool 'Java' -Status ERROR -Message 'Comando java nao encontrado no PATH.' -Expected $expected
    }
    else {
        $javaVersion = Get-ParsedVersion -Text $javaCmd.Output
        $javaLabel = if ($null -ne $javaVersion) { $javaVersion.Full } else { Get-FirstNonEmptyLine -Text $javaCmd.Output }
        if ($null -eq $javaVersion) {
            Add-Finding -Tool 'Java' -Status WARNING -Message "Java encontrado, mas a versao nao foi identificada: $javaLabel" -Expected $expected
        }
        elseif ($javaVersion.Major -eq $Official.JavaMajor) {
            Add-Finding -Tool 'Java' -Status OK -Message "Java encontrado: $javaLabel" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Java' -Status ERROR -Message "Java incompativel: $javaLabel" -Expected $expected
        }
        Add-Finding -Tool 'Java' -Status INFO -Message "Caminho do java: $($javaCmd.Path)"
    }

    if (-not $javacCmd.Found) {
        Add-Finding -Tool 'Java' -Status ERROR -Message 'JDK nao encontrado (javac ausente). Existe runtime, mas nao um JDK utilizavel.' -Expected $expected
    }
    else {
        $javacVersion = Get-ParsedVersion -Text $javacCmd.Output
        $javacLabel = if ($null -ne $javacVersion) { $javacVersion.Full } else { Get-FirstNonEmptyLine -Text $javacCmd.Output }
        if ($null -eq $javacVersion) {
            Add-Finding -Tool 'Java' -Status WARNING -Message "javac encontrado, mas a versao nao foi identificada: $javacLabel" -Expected $expected
        }
        elseif ($javacVersion.Major -eq $Official.JavaMajor) {
            Add-Finding -Tool 'Java' -Status OK -Message "JDK encontrado: $javacLabel (javac disponivel)" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Java' -Status ERROR -Message "JDK incompativel: $javacLabel" -Expected $expected
        }
        Add-Finding -Tool 'Java' -Status INFO -Message "Caminho do javac: $($javacCmd.Path)"
    }

    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        Add-Finding -Tool 'Java' -Status INFO -Message 'JAVA_HOME nao esta definido (somente informativo; o script nao altera variaveis de ambiente).'
    }
    else {
        $homeHasJdk = (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe')) -and
                      (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))
        if ($homeHasJdk) {
            Add-Finding -Tool 'Java' -Status INFO -Message "JAVA_HOME aponta para um JDK: $javaHome"
        }
        else {
            Add-Finding -Tool 'Java' -Status WARNING -Message "JAVA_HOME definido, mas nao parece um JDK completo: $javaHome"
        }
    }
}

function Test-Maven {
    Write-Section 'Maven'
    $expected = $Official.MavenLabel

    $wrapper = Get-MavenWrapperInfo
    if ($null -eq $wrapper) {
        Add-Finding -Tool 'Maven' -Status INFO -Message 'Maven Wrapper ainda nao existe (backend nao criado). Quando o Spring Boot for gerado, o wrapper sera a fonte preferencial da versao do projeto.'
    }
    else {
        Add-Finding -Tool 'Maven' -Status INFO -Message "Maven Wrapper encontrado: $($wrapper.Command)"
        if ($wrapper.Version) {
            $parsedWrapper = Get-ParsedVersion -Text $wrapper.Version
            if ($null -ne $parsedWrapper -and $parsedWrapper.Major -eq 4) {
                Add-Finding -Tool 'Maven' -Status ERROR -Message "Maven Wrapper declara $($wrapper.Version) (Maven 4 nao e o padrao atual do projeto)." -Expected $expected
            }
            elseif ($null -ne $parsedWrapper -and $parsedWrapper.Major -eq $Official.MavenMajor -and $parsedWrapper.Minor -eq $Official.MavenMinor -and (Test-VersionAtLeast -Version $parsedWrapper -Major $Official.MavenMajor -Minor $Official.MavenMinor -Patch $Official.MavenPatch)) {
                Add-Finding -Tool 'Maven' -Status OK -Message "Maven Wrapper (fonte preferencial do projeto): $($wrapper.Version)" -Expected $expected
            }
            elseif ($null -ne $parsedWrapper) {
                Add-Finding -Tool 'Maven' -Status ERROR -Message "Maven Wrapper declara $($wrapper.Version), fora da politica 3.9.x >= 3.9.12." -Expected $expected
            }
            else {
                Add-Finding -Tool 'Maven' -Status WARNING -Message "Maven Wrapper presente, mas a versao em maven-wrapper.properties nao foi identificada."
            }
        }
        else {
            Add-Finding -Tool 'Maven' -Status INFO -Message 'Maven Wrapper presente, mas maven-wrapper.properties ainda nao declara a versao.'
        }
    }

    $command = Invoke-NativeCommand -FileName 'mvn' -ArgumentList @('-version')
    if (-not $command.Found) {
        Add-Finding -Tool 'Maven' -Status ERROR -Message 'Maven instalado no PATH nao encontrado.' -Expected $expected
        return
    }
    if ($command.TimedOut) {
        Add-Finding -Tool 'Maven' -Status ERROR -Message 'mvn -version excedeu o tempo limite.' -Expected $expected
        return
    }

    $mavenVersion = $null
    if ($command.Output -match '(?im)Apache Maven\s+(\d+\.\d+(?:\.\d+)?)') {
        $mavenVersion = $Matches[1]
    }
    $parsed = if ($mavenVersion) { Get-ParsedVersion -Text $mavenVersion } else { Get-ParsedVersion -Text $command.Output }
    $foundLabel = if ($null -ne $parsed) { $parsed.Full } else { Get-FirstNonEmptyLine -Text $command.Output }

    if ($null -eq $parsed) {
        Add-Finding -Tool 'Maven' -Status WARNING -Message "Maven encontrado, mas a versao nao foi identificada: $foundLabel" -Expected $expected
    }
    elseif ($parsed.Major -ge 4) {
        Add-Finding -Tool 'Maven' -Status ERROR -Message "Maven $foundLabel e incompativel com o padrao atual do projeto (nao utilizar Maven 4)." -Expected $expected
    }
    elseif ($parsed.Major -eq $Official.MavenMajor -and $parsed.Minor -eq $Official.MavenMinor -and (Test-VersionAtLeast -Version $parsed -Major $Official.MavenMajor -Minor $Official.MavenMinor -Patch $Official.MavenPatch)) {
        Add-Finding -Tool 'Maven' -Status OK -Message "Maven encontrado no PATH: $foundLabel" -Expected $expected
    }
    elseif ($parsed.Major -eq $Official.MavenMajor -and $parsed.Minor -gt $Official.MavenMinor) {
        Add-Finding -Tool 'Maven' -Status WARNING -Message "Maven encontrado no PATH: $foundLabel (linha 3.9.x e a oficial; 3.$($parsed.Minor) nao e o padrao do projeto)." -Expected $expected
    }
    else {
        Add-Finding -Tool 'Maven' -Status ERROR -Message "Maven incompativel: $foundLabel" -Expected $expected
    }

    Add-Finding -Tool 'Maven' -Status INFO -Message "Caminho: $($command.Path)"

    $mavenJavaVersion = $null
    $mavenJavaHome = $null
    if ($command.Output -match '(?im)Java version:\s*([^\r\n,]+)') { $mavenJavaVersion = $Matches[1].Trim() }
    if ($command.Output -match '(?im)Java home:\s*([^\r\n]+)') { $mavenJavaHome = $Matches[1].Trim() }

    if ($mavenJavaVersion) {
        $parsedJava = Get-ParsedVersion -Text $mavenJavaVersion
        if ($null -ne $parsedJava -and $parsedJava.Major -eq $Official.JavaMajor) {
            Add-Finding -Tool 'Maven' -Status OK -Message "Maven esta utilizando Java $mavenJavaVersion" -Expected $Official.JavaLabel
        }
        elseif ($null -ne $parsedJava) {
            Add-Finding -Tool 'Maven' -Status ERROR -Message "Maven esta utilizando Java $mavenJavaVersion (incompativel com o projeto)" -Expected $Official.JavaLabel
        }
        else {
            Add-Finding -Tool 'Maven' -Status WARNING -Message "Maven reportou Java $mavenJavaVersion, mas a versao nao foi identificada com precisao." -Expected $Official.JavaLabel
        }
    }
    else {
        Add-Finding -Tool 'Maven' -Status WARNING -Message 'Nao foi possivel identificar qual Java o Maven esta utilizando.' -Expected $Official.JavaLabel
    }

    if ($mavenJavaHome) {
        Add-Finding -Tool 'Maven' -Status INFO -Message "Java home do Maven: $mavenJavaHome"
    }
}

function Test-Node {
    Write-Section 'Node.js'
    $expected = '22.x LTS >= 22.22.3 (preferencial) ou 24.x >= 24.15.0 (aceitavel)'
    $command = Invoke-NativeCommand -FileName 'node' -ArgumentList @('--version')

    if (-not $command.Found) {
        Add-Finding -Tool 'Node.js' -Status ERROR -Message 'Node.js nao encontrado.' -Expected $expected
        return
    }
    if ($command.TimedOut) {
        Add-Finding -Tool 'Node.js' -Status ERROR -Message 'node --version excedeu o tempo limite.' -Expected $expected
        return
    }

    $parsed = Get-ParsedVersion -Text $command.Output
    if ($null -eq $parsed) {
        Add-Finding -Tool 'Node.js' -Status WARNING -Message ("Node.js encontrado, mas a versao nao foi identificada: " + (Get-FirstNonEmptyLine -Text $command.Output)) -Expected $expected
        return
    }

    $script:NodeFoundLabel = $parsed.Full
    $major = $parsed.Major

    if ($major -le 20) {
        Add-Finding -Tool 'Node.js' -Status ERROR -Message "Node.js incompativel: $($parsed.Full) (20.x ou inferior nao e suportado pelo Angular 22)." -Expected $expected
    }
    elseif ($major -eq 22) {
        if (Test-VersionAtLeast -Version $parsed -Major 22 -Minor $Official.NodePreferredMinor -Patch $Official.NodePreferredPatch) {
            Add-Finding -Tool 'Node.js' -Status OK -Message "Node.js encontrado: $($parsed.Full) (linha preferencial)" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Node.js' -Status ERROR -Message "Node.js incompativel: $($parsed.Full) (22.x exige >= 22.22.3)." -Expected $expected
        }
    }
    elseif ($major -eq 24) {
        if (Test-VersionAtLeast -Version $parsed -Major 24 -Minor $Official.NodeAcceptedMinor -Patch $Official.NodeAcceptedPatch) {
            Add-Finding -Tool 'Node.js' -Status OK -Message "Node.js encontrado: $($parsed.Full) (linha compativel aceita)" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Node.js' -Status ERROR -Message "Node.js incompativel: $($parsed.Full) (24.x exige >= 24.15.0)." -Expected $expected
        }
    }
    elseif ($major -eq 26) {
        Add-Finding -Tool 'Node.js' -Status WARNING -Message "Node.js encontrado: $($parsed.Full) (Current; nao e LTS e nao e o padrao de instalacao do projeto). Angular 22 pode aceitar, mas a linha oficial e 22.x LTS." -Expected $expected
    }
    else {
        Add-Finding -Tool 'Node.js' -Status ERROR -Message "Node.js incompativel: $($parsed.Full)." -Expected $expected
    }

    Add-Finding -Tool 'Node.js' -Status INFO -Message "Caminho: $($command.Path)"
    Add-Finding -Tool 'Node.js' -Status INFO -Message 'Node 22 = linha preferencial. Node 24 = linha compativel aceita (nao e WARNING).'
}

function Test-Npm {
    Write-Section 'npm'
    $command = Invoke-NativeCommand -FileName 'npm' -ArgumentList @('--version')

    if (-not $command.Found) {
        Add-Finding -Tool 'npm' -Status ERROR -Message 'npm nao encontrado.'
        return
    }
    if ($command.TimedOut) {
        Add-Finding -Tool 'npm' -Status ERROR -Message 'npm --version excedeu o tempo limite.'
        return
    }

    $parsed = Get-ParsedVersion -Text $command.Output
    $foundLabel = if ($null -ne $parsed) { $parsed.Full } else { Get-FirstNonEmptyLine -Text $command.Output }

    Add-Finding -Tool 'npm' -Status OK -Message "npm encontrado: $foundLabel"
    Add-Finding -Tool 'npm' -Status INFO -Message 'npm e fornecido pelo Node.js. Nao ha versao independente exigida pelo projeto.'
    if ($script:NodeFoundLabel) {
        Add-Finding -Tool 'npm' -Status INFO -Message "Node.js encontrado: $($script:NodeFoundLabel) / npm encontrado: $foundLabel"
    }
    Add-Finding -Tool 'npm' -Status INFO -Message "Caminho: $($command.Path)"
    Add-Finding -Tool 'npm' -Status INFO -Message 'Nao utilizar npm install -g npm@latest como procedimento oficial.'
}

function Test-Angular {
    Write-Section 'Angular CLI'
    $expected = $Official.AngularLabel
    $global = Invoke-NativeCommand -FileName 'ng' -ArgumentList @('version') -TimeoutSeconds 30

    if (-not $global.Found) {
        Add-Finding -Tool 'Angular CLI' -Status WARNING -Message 'CLI global nao encontrada.' -Expected $expected
        Add-Finding -Tool 'Angular CLI' -Status INFO -Message 'A ausencia da CLI global nao e necessariamente um erro: o projeto pode usar a dependencia local declarada no package.json.'
    }
    elseif ($global.TimedOut) {
        Add-Finding -Tool 'Angular CLI' -Status WARNING -Message 'CLI global encontrada, mas ng version excedeu o tempo limite.' -Expected $expected
    }
    else {
        $globalVersion = $null
        if ($global.Output -match '(?im)Angular CLI(?:\s*:|\s+)\s*(\d+\.\d+\.\d+)') { $globalVersion = $Matches[1] }
        elseif ($global.Output -match '(?im)@angular/cli\s+(\d+\.\d+\.\d+)') { $globalVersion = $Matches[1] }
        else {
            $parsed = Get-ParsedVersion -Text $global.Output
            if ($null -ne $parsed) { $globalVersion = $parsed.Full }
        }

        if ($globalVersion) {
            $parsedGlobal = Get-ParsedVersion -Text $globalVersion
            if ($null -ne $parsedGlobal -and $parsedGlobal.Major -eq $Official.AngularMajor) {
                Add-Finding -Tool 'Angular CLI' -Status OK -Message "CLI global encontrada: $globalVersion" -Expected $expected
            }
            else {
                Add-Finding -Tool 'Angular CLI' -Status WARNING -Message "CLI global encontrada: $globalVersion" -Expected $expected
            }
        }
        else {
            Add-Finding -Tool 'Angular CLI' -Status WARNING -Message 'CLI global encontrada, mas a versao nao foi identificada.' -Expected $expected
        }
        Add-Finding -Tool 'Angular CLI' -Status INFO -Message "Caminho: $($global.Path)"
    }

    $packageJsonPath = Get-PackageJsonPath
    if ($null -eq $packageJsonPath) {
        Add-Finding -Tool 'Angular CLI' -Status INFO -Message 'Dependencia local ainda nao pode ser verificada: package.json do frontend nao existe (projeto Angular ainda nao foi criado).'
        Add-Finding -Tool 'Angular CLI' -Status INFO -Message "Quando o frontend existir, a versao oficial do projeto sera Angular $($Official.AngularLabel)."
        return
    }

    try {
        $package = (Get-Content -LiteralPath $packageJsonPath -Raw -ErrorAction Stop) | ConvertFrom-Json
        $cliSpec = Get-JsonProperty -Object $package.dependencies -Names @('@angular/cli')
        if ($null -eq $cliSpec) { $cliSpec = Get-JsonProperty -Object $package.devDependencies -Names @('@angular/cli') }
        $coreSpec = Get-JsonProperty -Object $package.dependencies -Names @('@angular/core')
        if ($null -eq $coreSpec) { $coreSpec = Get-JsonProperty -Object $package.devDependencies -Names @('@angular/core') }
        $relativePackage = $packageJsonPath.Substring($ProjectRoot.Path.Length).TrimStart('\', '/')

        if ($cliSpec) {
            $parsedCli = Get-ParsedVersion -Text ([string]$cliSpec)
            $status = if ($null -ne $parsedCli -and $parsedCli.Major -eq $Official.AngularMajor) { 'OK' } else { 'WARNING' }
            Add-Finding -Tool 'Angular CLI' -Status $status -Message "Dependencia local @angular/cli: $cliSpec ($relativePackage)" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Angular CLI' -Status INFO -Message "package.json encontrado ($relativePackage), mas @angular/cli nao esta declarado."
        }

        if ($coreSpec) {
            $parsedCore = Get-ParsedVersion -Text ([string]$coreSpec)
            $status = if ($null -ne $parsedCore -and $parsedCore.Major -eq $Official.AngularMajor) { 'OK' } else { 'WARNING' }
            Add-Finding -Tool 'Angular CLI' -Status $status -Message "Dependencia local @angular/core: $coreSpec" -Expected $expected
        }
    }
    catch {
        Add-Finding -Tool 'Angular CLI' -Status WARNING -Message "Nao foi possivel ler o package.json local: $($_.Exception.Message)"
    }

    $localNg = @(
        (Join-Path $ProjectRoot 'frontend\node_modules\.bin\ng.cmd'),
        (Join-Path $ProjectRoot 'node_modules\.bin\ng.cmd')
    ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

    if ($localNg) {
        Add-Finding -Tool 'Angular CLI' -Status OK -Message "CLI local encontrada em node_modules: $localNg"
    }
    else {
        Add-Finding -Tool 'Angular CLI' -Status INFO -Message 'CLI local em node_modules ainda nao esta instalada (ou o frontend ainda nao foi criado).'
    }
}

function Test-Docker {
    Write-Section 'Docker'
    $command = Invoke-NativeCommand -FileName 'docker' -ArgumentList @('--version')

    if (-not $command.Found) {
        Add-Finding -Tool 'Docker' -Status ERROR -Message 'Docker nao encontrado.'
        return
    }
    if ($command.TimedOut) {
        Add-Finding -Tool 'Docker' -Status ERROR -Message 'docker --version excedeu o tempo limite.'
        return
    }

    $parsed = Get-ParsedVersion -Text $command.Output
    $foundLabel = if ($null -ne $parsed) { $parsed.Full } else { Get-FirstNonEmptyLine -Text $command.Output }
    Add-Finding -Tool 'Docker' -Status OK -Message "Docker CLI encontrado: $foundLabel"
    Add-Finding -Tool 'Docker' -Status INFO -Message "Caminho: $($command.Path)"
    Add-Finding -Tool 'Docker' -Status INFO -Message 'A versao do CLI nao substitui a verificacao do Docker Engine.'
}

function Test-DockerEngine {
    Write-Section 'Docker Engine'
    $expected = $Official.DockerEngineLabel

    if ($null -eq (Get-NativeCommand -Name 'docker')) {
        Add-Finding -Tool 'Docker Engine' -Status ERROR -Message 'Docker Engine nao pode ser verificado porque o executavel docker nao foi encontrado.' -Expected $expected
        return
    }

    $info = Invoke-NativeCommand -FileName 'docker' -ArgumentList @('info') -TimeoutSeconds 25
    $server = Invoke-NativeCommand -FileName 'docker' -ArgumentList @('version', '--format', '{{.Server.Version}}') -TimeoutSeconds 20
    $serverVersionText = Get-FirstNonEmptyLine -Text $server.Output
    $parsedServer = $null
    if ($serverVersionText -match '^\d+') {
        $parsedServer = Get-ParsedVersion -Text $serverVersionText
    }

    $engineRunning = $false
    if ($info.TimedOut) {
        Add-Finding -Tool 'Docker Engine' -Status ERROR -Message 'Docker Engine nao respondeu a tempo. O daemon provavelmente nao esta em execucao.' -Expected $expected
        return
    }

    if ($info.ExitCode -eq 0 -and -not [string]::IsNullOrWhiteSpace($info.Output) -and $info.Output -notmatch '(?i)error during connect|cannot connect|is the docker daemon running') {
        $engineRunning = $true
    }
    elseif ($server.ExitCode -eq 0 -and $null -ne $parsedServer) {
        $engineRunning = $true
    }

    if (-not $engineRunning) {
        Add-Finding -Tool 'Docker Engine' -Status ERROR -Message 'Docker Engine nao esta em execucao.' -Expected $expected
        if ($info.Output -match '(?im)error during connect|cannot connect|is the docker daemon running|open //\./pipe/docker_engine') {
            $detail = Get-FirstNonEmptyLine -Text (($info.Output -split '\r?\n' | Where-Object { $_ -match '(?i)error|cannot connect|daemon' }) -join "`n")
            if (-not [string]::IsNullOrWhiteSpace($detail) -and $detail -notmatch '^(Client:|Server:)\s*$') {
                Add-Finding -Tool 'Docker Engine' -Status INFO -Message $detail
            }
        }
        return
    }

    if ($null -eq $parsedServer) {
        Add-Finding -Tool 'Docker Engine' -Status WARNING -Message 'Docker Engine em execucao, mas a versao do Engine nao foi identificada.' -Expected $expected
        return
    }

    if ($parsedServer.Major -ge $Official.DockerEngineMajor) {
        Add-Finding -Tool 'Docker Engine' -Status OK -Message "Docker Engine em execucao: $($parsedServer.Full)" -Expected $expected
    }
    else {
        Add-Finding -Tool 'Docker Engine' -Status ERROR -Message "Docker Engine incompativel: $($parsedServer.Full)" -Expected $expected
    }
}

function Test-DockerCompose {
    Write-Section 'Docker Compose'
    $expected = $Official.ComposeLabel

    if ($null -eq (Get-NativeCommand -Name 'docker')) {
        Add-Finding -Tool 'Docker Compose' -Status ERROR -Message 'Docker Compose V2 nao pode ser verificado porque o executavel docker nao foi encontrado.' -Expected $expected
        return
    }

    $compose = Invoke-NativeCommand -FileName 'docker' -ArgumentList @('compose', 'version')
    if ($compose.TimedOut) {
        Add-Finding -Tool 'Docker Compose' -Status ERROR -Message 'docker compose version excedeu o tempo limite.' -Expected $expected
        return
    }

    $composeFound = ($compose.ExitCode -eq 0 -and $compose.Output -match '(?i)compose')
    if (-not $composeFound) {
        Add-Finding -Tool 'Docker Compose' -Status ERROR -Message 'Docker Compose V2 (docker compose) nao encontrado.' -Expected $expected
    }
    else {
        $parsed = Get-ParsedVersion -Text $compose.Output
        if ($null -eq $parsed) {
            Add-Finding -Tool 'Docker Compose' -Status WARNING -Message ("Docker Compose V2 encontrado, mas a versao nao foi identificada: " + (Get-FirstNonEmptyLine -Text $compose.Output)) -Expected $expected
        }
        elseif (Test-VersionAtLeast -Version $parsed -Major $Official.ComposeMajor -Minor $Official.ComposeMinor -Patch $Official.ComposePatch) {
            Add-Finding -Tool 'Docker Compose' -Status OK -Message "Docker Compose V2 encontrado: $($parsed.Full)" -Expected $expected
        }
        else {
            Add-Finding -Tool 'Docker Compose' -Status ERROR -Message "Docker Compose V2 incompativel: $($parsed.Full)" -Expected $expected
        }
    }

    $legacy = Get-NativeCommand -Name 'docker-compose'
    if ($null -ne $legacy) {
        $legacyVersion = Invoke-NativeCommand -FileName 'docker-compose' -ArgumentList @('version')
        $legacyLabel = Get-FirstNonEmptyLine -Text $legacyVersion.Output
        if ([string]::IsNullOrWhiteSpace($legacyLabel)) { $legacyLabel = $legacy.Source }
        Add-Finding -Tool 'Docker Compose' -Status INFO -Message "docker-compose legado tambem esta presente (nao e requisito): $legacyLabel"
    }
}

function Test-PostgreSQL {
    Write-Section 'PostgreSQL'
    $expected = $Official.PostgresLabel
    $local = Invoke-NativeCommand -FileName 'psql' -ArgumentList @('--version')

    if (-not $local.Found) {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message 'PostgreSQL nao instalado diretamente no Windows (nao obrigatorio).' -Expected $expected
    }
    else {
        $parsed = Get-ParsedVersion -Text $local.Output
        $foundLabel = if ($null -ne $parsed) { $parsed.Full } else { Get-FirstNonEmptyLine -Text $local.Output }
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message "PostgreSQL local encontrado: $foundLabel (nao e o banco oficial de desenvolvimento)" -Expected $expected
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message "Caminho: $($local.Path)"
    }

    $postgresService = @(Get-Service -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '(?i)postgresql?' -or $_.DisplayName -match '(?i)postgresql?' } |
        Select-Object -First 3)
    foreach ($service in $postgresService) {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message "Servico Windows detectado: $($service.Name) (status: $($service.Status)). Nenhuma acao foi tomada."
    }

    $composePath = Get-ComposeFilePath
    if ($null -eq $composePath) {
        Add-Finding -Tool 'PostgreSQL' -Status ERROR -Message 'Arquivo Docker Compose do projeto nao encontrado (esperado docker-compose.yml).' -Expected $expected
        return
    }

    $relativeCompose = $composePath.Substring($ProjectRoot.Path.Length).TrimStart('\', '/')
    $composeText = Get-Content -LiteralPath $composePath -Raw -ErrorAction SilentlyContinue
    if ([string]::IsNullOrWhiteSpace($composeText)) {
        Add-Finding -Tool 'PostgreSQL' -Status ERROR -Message "Nao foi possivel ler $relativeCompose." -Expected $expected
        return
    }

    $image = $null
    if ($composeText -match '(?im)image:\s*([^\s#]+)') {
        $image = $Matches[1].Trim()
    }

    if ($image -and $image -match '^postgres:18([\d.]*)-alpine') {
        Add-Finding -Tool 'PostgreSQL' -Status OK -Message "PostgreSQL sera executado via Docker (imagem $image em $relativeCompose)." -Expected $expected
    }
    elseif ($image -and $image -match '^postgres:18') {
        Add-Finding -Tool 'PostgreSQL' -Status WARNING -Message "Docker Compose declara $image; o contrato oficial e $($Official.PostgresImage)." -Expected $expected
    }
    elseif ($image) {
        Add-Finding -Tool 'PostgreSQL' -Status ERROR -Message "Docker Compose declara imagem $image, incompativel com $($Official.PostgresImage)." -Expected $expected
    }
    else {
        Add-Finding -Tool 'PostgreSQL' -Status WARNING -Message "Nao foi possivel identificar a imagem PostgreSQL em $relativeCompose." -Expected $expected
    }

    if ($null -eq (Get-NativeCommand -Name 'docker')) {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message 'Container PostgreSQL nao verificado: Docker nao esta disponivel.'
        return
    }

    $engineOk = @($script:Findings | Where-Object { $_.Tool -eq 'Docker Engine' -and $_.Status -eq 'OK' })
    if ($engineOk.Count -eq 0) {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message 'Container PostgreSQL nao verificado: Docker Engine nao esta em execucao. Isso nao exige PostgreSQL instalado no Windows.'
        return
    }

    $ps = Invoke-NativeCommand -FileName 'docker' -ArgumentList @(
        'ps', '--filter', 'name=financial-control-postgres', '--format', '{{.Names}} {{.Status}} {{.Image}}'
    ) -TimeoutSeconds 20
    $containerLine = Get-FirstNonEmptyLine -Text $ps.Output
    if ($ps.ExitCode -eq 0 -and -not [string]::IsNullOrWhiteSpace($containerLine) -and $containerLine -notmatch '(?i)error') {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message "Container detectado: $containerLine (informativo; o script nao inicia nem para containers)."
    }
    else {
        Add-Finding -Tool 'PostgreSQL' -Status INFO -Message 'Container financial-control-postgres nao esta em execucao no momento (esperado ate docker compose up -d).'
    }
}

function Show-Summary {
    Write-Banner 'RESULTADO'
    $errors = @($script:Findings | Where-Object { $_.Status -eq 'ERROR' })
    $warnings = @($script:Findings | Where-Object { $_.Status -eq 'WARNING' })

    if ($errors.Count -eq 0) {
        Write-Host '[OK] Ambiente pronto para desenvolvimento (nenhum erro bloqueante).' -ForegroundColor Green
        if ($warnings.Count -gt 0) {
            Write-Host ''
            Write-Host 'Avisos:' -ForegroundColor Yellow
            foreach ($warning in $warnings) {
                Write-Host "- $($warning.Tool): $($warning.Message)" -ForegroundColor Yellow
            }
        }
    }
    else {
        Write-Host '[ERROR] Ambiente ainda nao esta pronto.' -ForegroundColor Red
        Write-Host ''
        Write-Host 'Problemas encontrados:' -ForegroundColor Red
        foreach ($item in $errors) {
            Write-Host "- $($item.Tool): $($item.Message)" -ForegroundColor Red
        }
        if ($warnings.Count -gt 0) {
            Write-Host ''
            Write-Host 'Avisos:' -ForegroundColor Yellow
            foreach ($warning in $warnings) {
                Write-Host "- $($warning.Tool): $($warning.Message)" -ForegroundColor Yellow
            }
        }
    }

    Write-Host ''
    Write-Host 'Environment Contract utilizado:' -ForegroundColor Gray
    Write-Host "  Java/JDK        : $($Official.JavaLabel)" -ForegroundColor Gray
    Write-Host "  Angular         : $($Official.AngularLabel)" -ForegroundColor Gray
    Write-Host '  Node.js         : 22.x LTS >= 22.22.3 (preferencial); 24.x >= 24.15.0 (aceitavel)' -ForegroundColor Gray
    Write-Host '  npm             : empacotado com o Node.js' -ForegroundColor Gray
    Write-Host "  Maven           : $($Official.MavenLabel)" -ForegroundColor Gray
    Write-Host "  Git             : $($Official.GitLabel)" -ForegroundColor Gray
    Write-Host "  Docker Engine   : $($Official.DockerEngineLabel)" -ForegroundColor Gray
    Write-Host "  Docker Compose  : $($Official.ComposeLabel)" -ForegroundColor Gray
    Write-Host "  PostgreSQL      : $($Official.PostgresLabel)" -ForegroundColor Gray
    Write-Host ''
    Write-Host 'Nenhuma alteracao foi realizada no sistema.' -ForegroundColor Gray
    Write-Host '========================================================' -ForegroundColor Cyan
    Write-Host ''
}

# =============================================================================
# Execucao
# =============================================================================

Write-Banner 'Financial Control - Environment Check'
Write-Host "Projeto: $ProjectRoot"
Write-Host 'Modo: diagnostico somente leitura (nenhuma instalacao ou alteracao).'
Write-Host 'Contrato: docs/22-stack-tecnologica.md (secao 30).'

Invoke-Check -Name 'Sistema'        -Action { Test-OperatingSystem }
Invoke-Check -Name 'Git'            -Action { Test-Git }
Invoke-Check -Name 'Java'           -Action { Test-Java }
Invoke-Check -Name 'Maven'          -Action { Test-Maven }
Invoke-Check -Name 'Node.js'        -Action { Test-Node }
Invoke-Check -Name 'npm'            -Action { Test-Npm }
Invoke-Check -Name 'Angular CLI'    -Action { Test-Angular }
Invoke-Check -Name 'Docker'         -Action { Test-Docker }
Invoke-Check -Name 'Docker Engine'  -Action { Test-DockerEngine }
Invoke-Check -Name 'Docker Compose' -Action { Test-DockerCompose }
Invoke-Check -Name 'PostgreSQL'     -Action { Test-PostgreSQL }

Show-Summary

if ((@($script:Findings | Where-Object { $_.Status -eq 'ERROR' })).Count -gt 0) { exit 1 }
exit 0
