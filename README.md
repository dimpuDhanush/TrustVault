# TrustVault

TrustVault is a JavaFX desktop banking operations application backed by MySQL. It supports customer onboarding, account creation, deposits and withdrawals, transfers, statements, audit logs, loans, and fixed deposits.

## Stack

- Java 21
- JavaFX
- MySQL
- JDBC
- PowerShell packaging with `jpackage`

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
- JavaFX SDK
- MySQL server with a `trustvault` database

Create a local `trustvault.properties` file:

```properties
db.url=jdbc:mysql://localhost:3306/trustvault
db.user=root
db.password=your-password
```

Compile from IntelliJ or use the packaging script below.

## Package Locally

Portable Windows app image:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

Installer build:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -PackageType exe
```

Installer packaging requires WiX Toolset on the machine.

More details are in [DEPLOYMENT.md](DEPLOYMENT.md).

## GitHub Actions

The workflow at `.github/workflows/windows-package.yml` does the following:

- on pushes and pull requests to `main`, builds the Windows portable package and uploads it as an artifact
- on version tags like `v1.0.0`, builds the portable package and Windows installer, then publishes them to GitHub Releases

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
