# jpackage Resource Directory

The Windows packaging script points `jpackage` at this directory with `--resource-dir`.

You can drop any of these files here to customize the installer:

- `TrustVault.ico`
- `WinInstaller.properties`
- `main.wxs`
- `overrides.wxi`

`scripts/New-TrustVaultIcon.ps1` generates `TrustVault.ico` here automatically when needed.
