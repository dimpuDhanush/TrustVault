param(
    [string]$OutputPath = $(Join-Path $PSScriptRoot "..\installer\jpackage\TrustVault.ico"),
    [string]$SourcePngPath = $(Join-Path $PSScriptRoot "..\assets\trustvault-logo.png")
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function New-PlaceholderBitmap {
    param(
        [int]$Width = 256,
        [int]$Height = 256
    )

    $bitmap = New-Object System.Drawing.Bitmap $Width, $Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

        $background = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            (New-Object System.Drawing.Rectangle 0, 0, $Width, $Height),
            [System.Drawing.Color]::FromArgb(14, 16, 19),
            [System.Drawing.Color]::FromArgb(124, 93, 58),
            45
        )
        $graphics.FillRectangle($background, 0, 0, $Width, $Height)

        $ringBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(218, 180, 123))
        $graphics.FillEllipse($ringBrush, 24, 24, $Width - 48, $Height - 48)

        $innerBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(8, 10, 13))
        $graphics.FillEllipse($innerBrush, 40, 40, $Width - 80, $Height - 80)

        $font = New-Object System.Drawing.Font("Georgia", 86, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $textBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(241, 237, 230))
        $textSize = $graphics.MeasureString("TV", $font)
        $x = ($Width - $textSize.Width) / 2
        $y = ($Height - $textSize.Height) / 2 - 6
        $graphics.DrawString("TV", $font, $textBrush, $x, $y)
    } finally {
        if ($background) {
            $background.Dispose()
        }
        if ($ringBrush) {
            $ringBrush.Dispose()
        }
        if ($innerBrush) {
            $innerBrush.Dispose()
        }
        if ($font) {
            $font.Dispose()
        }
        if ($textBrush) {
            $textBrush.Dispose()
        }
        $graphics.Dispose()
    }

    return $bitmap
}

function New-ResizedBitmap {
    param(
        [string]$Path,
        [int]$Size = 256
    )

    $source = [System.Drawing.Image]::FromFile($Path)
    $bitmap = New-Object System.Drawing.Bitmap $Size, $Size
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.DrawImage($source, 0, 0, $Size, $Size)
    } finally {
        $graphics.Dispose()
        $source.Dispose()
    }

    return $bitmap
}

function Save-AsIcon {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )

    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $pngStream = New-Object System.IO.MemoryStream
    try {
        $Bitmap.Save($pngStream, [System.Drawing.Imaging.ImageFormat]::Png)
        $pngBytes = $pngStream.ToArray()

        $fileStream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        $writer = New-Object System.IO.BinaryWriter $fileStream
        try {
            $writer.Write([UInt16]0)
            $writer.Write([UInt16]1)
            $writer.Write([UInt16]1)
            $writer.Write([byte]0)
            $writer.Write([byte]0)
            $writer.Write([byte]0)
            $writer.Write([byte]0)
            $writer.Write([UInt16]1)
            $writer.Write([UInt16]32)
            $writer.Write([UInt32]$pngBytes.Length)
            $writer.Write([UInt32]22)
            $writer.Write($pngBytes)
        } finally {
            $writer.Dispose()
            $fileStream.Dispose()
        }
    } finally {
        $pngStream.Dispose()
        $Bitmap.Dispose()
    }
}

$bitmap = if (Test-Path $SourcePngPath) {
    New-ResizedBitmap -Path $SourcePngPath
} else {
    New-PlaceholderBitmap
}

Save-AsIcon -Bitmap $bitmap -Path $OutputPath
Write-Output (Resolve-Path $OutputPath).Path
