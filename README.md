# TrustVault

TrustVault is a JavaFX desktop banking operations application backed by MySQL. It supports customer onboarding, account creation, deposits and withdrawals, transfers, statements, audit logs, loans, and fixed deposits.

## Stack

- Java 21
- JavaFX
- MySQL
- JDBC
- Maven
- PowerShell packaging with `jpackage` and optional WiX Toolset

## Features

- Admin login with password hashing
- Customer and account management
- Cash transactions and transfer workflows
- Statement export
- Audit log viewer
- Loan and fixed deposit management
- Windows desktop packaging

## Local Run

Prerequisites:

- JDK 21
- Maven 3.9+
- MySQL server with a `trustvault` database

Create a local `trustvault.properties` file:

```properties
db.url=jdbc:mysql://localhost:3306/trustvault
db.user=root
db.password=your-password
```

Build the runnable fat JAR:

```powershell
mvn clean package
```

Run the app directly from Maven:

```powershell
mvn javafx:run
```

Run the packaged fat JAR locally:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

## Package Locally

Portable Windows app image:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType app-image
```

Windows `.exe` installer:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType exe
```

Windows `.msi` installer:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType msi
```

Installer packaging requires WiX Toolset on the machine. Final outputs are written to:

```text
dist/app-image/TrustVault/
dist/installer/
dist/runtime/TrustVault-runtime/
```

More details are in [DEPLOYMENT.md](DEPLOYMENT.md).

## GitHub Actions

The workflow at `.github/workflows/windows-package.yml` does the following:

- on pushes and pull requests to `main`, builds the Windows installer package and uploads artifacts
- on version tags like `v1.0.0`, builds release assets and publishes them to GitHub Releases

The workflow at `.github/workflows/deploy-pages.yml` publishes the public project page to the `gh-pages` branch.

Public preview URL:

```text
https://rawcdn.githack.com/dimpuDhanush/TrustVault/gh-pages/index.html
```

## Typical GitHub Setup

After creating a GitHub repository, run:

```powershell
git add .
git commit -m "Initial TrustVault setup"
git remote add origin https://github.com/<your-user>/<your-repo>.git
git push -u origin main
```

To publish a release:

```powershell
git tag v1.0.0
git push origin v1.0.0
```
