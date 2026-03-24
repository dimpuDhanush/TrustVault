# TrustVault Deployment

## Target

This project is a Windows JavaFX desktop application packaged with `jpackage`.

## What Is Included

- `pom.xml`: Maven build for a runnable fat JAR
- `scripts/package-windows.ps1`: builds the fat JAR and packages the Windows app
- `scripts/run-local.ps1`: runs the shaded JAR locally
- `scripts/New-TrustVaultIcon.ps1`: generates a Windows `.ico` from the app logo
- `trustvault.properties.example`: runtime DB config template

## Prerequisites

- JDK 21 with `java`, `jdeps`, and `jpackage`
- Maven 3.9+
- MySQL server reachable from the target machine
- WiX Toolset 3+ for `.exe` or `.msi` installers

## Runtime Database Config

Create a `trustvault.properties` file based on `trustvault.properties.example`.

Example:

```properties
db.url=jdbc:mysql://localhost:3306/trustvault
db.user=root
db.password=your-password
```

The application looks for config in these places:

- `.\trustvault.properties`
- `.\config\trustvault.properties`
- next to the packaged launcher
- next to the launcher under `config\trustvault.properties`
- `%APPDATA%\TrustVault\trustvault.properties`
- `%ProgramData%\TrustVault\trustvault.properties`
- a custom file passed with `-Dtrustvault.config.file=...` or `TV_CONFIG_FILE`

For packaged builds, the script copies a starter config into `config\trustvault.properties`.

## Build The Fat JAR

```powershell
mvn clean package
```

Output:

```text
target\<artifactId>-<version>-all.jar
```

Run:

```powershell
java -jar .\target\trustvault-1.0.0-all.jar
```

## Build An App Image

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType app-image
```

Output:

```text
dist\app-image\TrustVault\
dist\installer\TrustVault-portable-<version>.zip
dist\runtime\TrustVault-runtime\
```

Run:

```powershell
.\dist\app-image\TrustVault\TrustVault.exe
```

## Build An Installer

Requires WiX Toolset on the build machine.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType exe
```

Output:

```text
dist\installer\TrustVault-<version>.exe
```

MSI output:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType msi
```

```text
dist\installer\TrustVault-<version>.msi
```

## Deployment Notes

- The packaged app includes the Java runtime image, so the target machine does not need Java preinstalled.
- The target machine still needs network access to the MySQL server you configure.
- If login or startup fails, check the generated logs under the local `logs` directory.
- GitHub Actions builds the installer and uploads the `dist/` outputs as artifacts.

## WiX Notes

`jpackage` uses WiX on Windows for native installer generation. For advanced customization, place these files in `installer\jpackage\`:

- `main.wxs`
- `overrides.wxi`
- `WinInstaller.properties`

Those overrides are picked up automatically because the packaging script passes that folder as `--resource-dir`.
