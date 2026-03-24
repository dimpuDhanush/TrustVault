param(
    [string]$ConfigPath = $(Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")).Path "trustvault.properties"),
    [string]$MavenCommand,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Resolve-MavenCommand {
    param(
        [string]$ProjectRoot,
        [string]$ExplicitCommand
    )

    if ($ExplicitCommand) {
        return $ExplicitCommand
    }

    $wrapper = Join-Path $ProjectRoot "mvnw.cmd"
    if (Test-Path $wrapper) {
        return $wrapper
    }

    $maven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($maven) {
        return $maven.Source
    }

    throw "Maven was not found. Install Maven 3.9+ or add mvnw.cmd to the project."
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jarPath = Get-ChildItem -Path (Join-Path $projectRoot "target") -Filter "*-all.jar" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -ExpandProperty FullName -First 1

if (-not $SkipBuild -or -not (Test-Path $jarPath)) {
    $maven = Resolve-MavenCommand -ProjectRoot $projectRoot -ExplicitCommand $MavenCommand
    & $maven -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }
}

if (-not (Test-Path $jarPath)) {
    throw "Fat JAR matching '*-all.jar' not found under target."
}

$javaArgs = @()
if (Test-Path $ConfigPath) {
    $javaArgs += "-Dtrustvault.config.file=$ConfigPath"
}
$javaArgs += @("-jar", $jarPath)

& java @javaArgs
