param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$PackageType = "exe",
    [string]$AppVersion,
    [string]$Vendor = "TrustVault",
    [string]$MavenCommand,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Get-ProjectVersion {
    param([string]$PomPath)

    [xml]$pom = Get-Content -Path $PomPath
    return $pom.project.version
}

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

function Assert-Tool {
    param([string]$ToolName)

    if (-not (Get-Command $ToolName -ErrorAction SilentlyContinue)) {
        throw "$ToolName is required but was not found in PATH."
    }
}

function Assert-WixToolset {
    if ((Get-Command candle.exe -ErrorAction SilentlyContinue) -or (Get-Command wix.exe -ErrorAction SilentlyContinue)) {
        return
    }

    throw "WiX Toolset is required for installer builds. Install WiX 3+ and add it to PATH."
}

function Resolve-FatJar {
    param([string]$TargetDirectory)

    $jar = Get-ChildItem -Path $TargetDirectory -Filter "*-all.jar" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($jar) {
        return $jar.FullName
    }

    return $null
}

function Get-RuntimeModules {
    param([string]$JarPath)

    $fallback = @(
        "java.base",
        "java.desktop",
        "java.logging",
        "java.naming",
        "java.sql",
        "java.xml",
        "jdk.crypto.ec",
        "jdk.unsupported"
    )

    $jdepsOutput = & jdeps --ignore-missing-deps --multi-release 21 --print-module-deps $JarPath 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($jdepsOutput)) {
        return ($fallback -join ",")
    }

    $modules = $jdepsOutput.Trim().Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    $modules += $fallback

    return (($modules | Select-Object -Unique) -join ",")
}

function Copy-ConfigFiles {
    param(
        [string]$ProjectRoot,
        [string]$ApplicationRoot
    )

    $configDirectory = Join-Path $ApplicationRoot "config"
    $exampleConfig = Join-Path $ProjectRoot "trustvault.properties.example"
    if (-not (Test-Path $exampleConfig)) {
        return
    }

    New-Item -ItemType Directory -Force -Path $configDirectory | Out-Null
    Copy-Item $exampleConfig (Join-Path $configDirectory "trustvault.properties.example") -Force
    Copy-Item $exampleConfig (Join-Path $configDirectory "trustvault.properties") -Force
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pomPath = Join-Path $projectRoot "pom.xml"
$targetDir = Join-Path $projectRoot "target"
$distDir = Join-Path $projectRoot "dist"
$appImageDir = Join-Path $distDir "app-image"
$installerDir = Join-Path $distDir "installer"
$runtimeDir = Join-Path $distDir "runtime"
$resourceDir = Join-Path $projectRoot "installer\jpackage"
$iconScript = Join-Path $PSScriptRoot "New-TrustVaultIcon.ps1"
$upgradeUuid = "d6c6f2d5-7308-4f35-9a85-89ce6d93db1f"

Assert-Tool java
Assert-Tool jdeps
Assert-Tool jpackage

if (-not (Test-Path $pomPath)) {
    throw "pom.xml was not found in $projectRoot"
}

if (-not $AppVersion) {
    $AppVersion = Get-ProjectVersion -PomPath $pomPath
}

$iconPath = & $iconScript -OutputPath (Join-Path $resourceDir "TrustVault.ico")

Remove-Item -Recurse -Force $distDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $appImageDir, $installerDir, $runtimeDir, $resourceDir | Out-Null

if (-not $SkipBuild) {
    $maven = Resolve-MavenCommand -ProjectRoot $projectRoot -ExplicitCommand $MavenCommand
    & $maven -B -DskipTests clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }
}

$fatJarPath = Resolve-FatJar -TargetDirectory $targetDir
if (-not (Test-Path $fatJarPath)) {
    throw "Expected fat JAR matching '*-all.jar' not found in $targetDir. Run 'mvn clean package' first."
}

$fatJarName = Split-Path -Leaf $fatJarPath

$runtimeModules = Get-RuntimeModules -JarPath $fatJarPath

$appImageArgs = @(
    "--type", "app-image",
    "--dest", $appImageDir,
    "--name", "TrustVault",
    "--input", $targetDir,
    "--main-jar", $fatJarName,
    "--main-class", "Bootstrap",
    "--vendor", $Vendor,
    "--app-version", $AppVersion,
    "--description", "TrustVault banking operations desktop application",
    "--icon", $iconPath,
    "--resource-dir", $resourceDir,
    "--add-modules", $runtimeModules,
    "--java-options", "-Dfile.encoding=UTF-8"
)

& jpackage @appImageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage app-image creation failed."
}

$generatedAppImage = Join-Path $appImageDir "TrustVault"
if (-not (Test-Path $generatedAppImage)) {
    throw "Expected app image not found: $generatedAppImage"
}

Copy-ConfigFiles -ProjectRoot $projectRoot -ApplicationRoot $generatedAppImage

$runtimeSource = Join-Path $generatedAppImage "runtime"
$runtimeTarget = Join-Path $runtimeDir "TrustVault-runtime"
if (Test-Path $runtimeSource) {
    Copy-Item -Recurse -Force $runtimeSource $runtimeTarget
}

$portableZip = Join-Path $installerDir ("TrustVault-portable-{0}.zip" -f $AppVersion)
Compress-Archive -Path (Join-Path $generatedAppImage "*") -DestinationPath $portableZip -Force

if ($PackageType -ne "app-image") {
    Assert-WixToolset

    $installerArgs = @(
        "--type", $PackageType,
        "--dest", $installerDir,
        "--name", "TrustVault",
        "--app-image", $generatedAppImage,
        "--vendor", $Vendor,
        "--app-version", $AppVersion,
        "--icon", $iconPath,
        "--resource-dir", $resourceDir,
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", "TrustVault",
        "--win-shortcut",
        "--win-per-user-install",
        "--win-upgrade-uuid", $upgradeUuid
    )

    & jpackage @installerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage installer creation failed."
    }
}

Write-Host "Fat JAR: $fatJarPath"
Write-Host "App image: $generatedAppImage"
Write-Host "Runtime image: $runtimeTarget"
Write-Host "Portable ZIP: $portableZip"

if ($PackageType -eq "app-image") {
    Write-Host "Installer output: skipped (PackageType=app-image)"
} else {
    $installerFile = Get-ChildItem -Path $installerDir -File | Where-Object {
        $_.Name -match ("^TrustVault.*\.{0}$" -f [regex]::Escape($PackageType))
    } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($installerFile) {
        Write-Host "Installer output: $($installerFile.FullName)"
    }
}
