param(
    [ValidateSet("app-image", "exe")]
    [string]$PackageType = "app-image",
    [string]$AppVersion = "1.0.0",
    [string]$JavaFxLibPath = $(if ($env:TV_JAVAFX_LIB) { $env:TV_JAVAFX_LIB } else { "C:\JavaFX\javafx-sdk-21.0.10\lib" }),
    [string]$MySqlJarPath = $(Join-Path $PSScriptRoot "..\lib\mysql-connector-j-9.6.0.jar")
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$srcDir = Join-Path $projectRoot "src"
$assetsDir = Join-Path $projectRoot "assets"
$buildDir = Join-Path $projectRoot "build"
$classesDir = Join-Path $buildDir "classes"
$packageInputDir = Join-Path $buildDir "package-input"
$distDir = Join-Path $projectRoot "dist"
$jarPath = Join-Path $packageInputDir "TrustVault.jar"
$configExamplePath = Join-Path $projectRoot "trustvault.properties.example"

if (-not (Test-Path $JavaFxLibPath)) {
    throw "JavaFX SDK lib directory not found: $JavaFxLibPath"
}

if (-not (Test-Path $MySqlJarPath)) {
    throw "MySQL JDBC driver not found: $MySqlJarPath"
}

if ($PackageType -eq "exe") {
    $wixLight = Get-Command light.exe -ErrorAction SilentlyContinue
    $wixCandle = Get-Command candle.exe -ErrorAction SilentlyContinue
    if (-not $wixLight -or -not $wixCandle) {
        throw "WiX Toolset is required for -PackageType exe. Install WiX and add candle.exe and light.exe to PATH."
    }
}

Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $distDir -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Force $classesDir, $packageInputDir, $distDir | Out-Null

$sourceFiles = Get-ChildItem -Path $srcDir -Filter *.java | ForEach-Object FullName
if (-not $sourceFiles) {
    throw "No Java source files found in $srcDir"
}

& javac `
    --module-path $JavaFxLibPath `
    --add-modules javafx.controls,javafx.fxml `
    -cp $MySqlJarPath `
    -d $classesDir `
    $sourceFiles

if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed."
}

Copy-Item -Recurse $assetsDir (Join-Path $classesDir "assets")

& jar --create --file $jarPath --main-class Main -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "JAR packaging failed."
}

Copy-Item $MySqlJarPath $packageInputDir -Force
if (Test-Path $configExamplePath) {
    Copy-Item $configExamplePath $packageInputDir -Force
}

$jpackageArgs = @(
    "--type", $PackageType,
    "--dest", $distDir,
    "--name", "TrustVault",
    "--input", $packageInputDir,
    "--main-jar", "TrustVault.jar",
    "--main-class", "Main",
    "--vendor", "TrustVault",
    "--app-version", $AppVersion,
    "--module-path", $JavaFxLibPath,
    "--add-modules", "javafx.controls,javafx.fxml,java.sql,java.logging,java.naming,java.xml",
    "--java-options", "-Dfile.encoding=UTF-8"
)

if ($PackageType -eq "exe") {
    $jpackageArgs += @(
        "--win-dir-chooser",
        "--win-menu",
        "--win-shortcut",
        "--win-per-user-install"
    )
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed."
}

$appImageRoot = Join-Path $distDir "TrustVault"
if ((Test-Path $appImageRoot) -and (Test-Path $configExamplePath)) {
    Copy-Item $configExamplePath (Join-Path $appImageRoot "trustvault.properties.example") -Force
    Copy-Item $configExamplePath (Join-Path $appImageRoot "trustvault.properties") -Force
    $portableZip = Join-Path $distDir "TrustVault-portable.zip"
    Remove-Item $portableZip -ErrorAction SilentlyContinue
    Compress-Archive -Path $appImageRoot -DestinationPath $portableZip
}

Write-Host "Package created successfully."
Write-Host "Output directory: $distDir"
