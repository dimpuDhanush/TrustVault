# TrustVault Deployment

## Target

This project is a Windows JavaFX desktop application packaged with `jpackage`.

## What Is Included

- `scripts/package-windows.ps1`: builds and packages the app
- `lib/mysql-connector-j-9.6.0.jar`: bundled JDBC dependency used at build/package time
- `trustvault.properties.example`: runtime DB config template

## Prerequisites

- JDK 21 with `javac`, `jar`, and `jpackage`
- JavaFX SDK 21 installed locally
- MySQL server reachable from the target machine

Default JavaFX path expected by the script:

```powershell
C:\JavaFX\javafx-sdk-21.0.10\lib
```

If your JavaFX SDK is elsewhere, set:

```powershell
$env:TV_JAVAFX_LIB="C:\path\to\javafx-sdk-21.0.10\lib"
```

## Runtime Database Config

Create a `trustvault.properties` file based on `trustvault.properties.example`.

Example:

```properties
db.url=jdbc:mysql://localhost:3306/trustvault
db.user=root
db.password=your-password
```

For packaged builds, keep this file next to the generated app launcher.

## Build An App Image

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

Output:

```text
dist\TrustVault\
dist\TrustVault-portable.zip
```

Run:

```powershell
.\dist\TrustVault\TrustVault.exe
```

## Build An Installer

Requires WiX Toolset on the build machine.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType exe
```

Output:

```text
dist\TrustVault-1.0.0.exe
```

## Deployment Notes

- The packaged app includes the Java runtime image, so the target machine does not need Java preinstalled.
- The target machine still needs network access to the MySQL server you configure.
- If login or startup fails, check the generated logs under the local `logs` directory.
- GitHub release automation publishes the portable Windows package.
