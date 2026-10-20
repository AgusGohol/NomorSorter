# Membangun NomorSorter.jar dan ZIP rilis di folder dist\.
# Pakai: powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$version = (Get-Content (Join-Path $root 'VERSION') -Raw).Trim()
$source = Get-Content (Join-Path $root 'src\NomorSorter.java') -Raw
if ($source -notmatch "APP_VERSION = `"$([regex]::Escape($version))`"") {
    throw "APP_VERSION di src\NomorSorter.java tidak sama dengan VERSION ($version)."
}

# Kompilasi di folder sementara berjalur pendek agar tidak terkena batas panjang jalur Windows.
$classes = Join-Path ([IO.Path]::GetTempPath()) ("ns-build-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Force $classes | Out-Null
try {
    & javac --release 17 -encoding UTF-8 -d $classes (Join-Path $root 'src\NomorSorter.java')
    if ($LASTEXITCODE -ne 0) { throw 'Kompilasi gagal.' }
    $jar = Join-Path $root 'NomorSorter.jar'
    if (Test-Path $jar) { Remove-Item -Force $jar }
    & jar --create --file $jar --main-class NomorSorter -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Pembuatan JAR gagal.' }
} finally { Remove-Item -Recurse -Force $classes -ErrorAction SilentlyContinue }

# Entri ZIP harus memakai garis miring "/" dan berada di folder NomorSorter/ agar dikenali updater.
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
$zip = Join-Path $dist "NomorSorter_$version.zip"
if (Test-Path $zip) { Remove-Item -Force $zip }
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open($zip, 'Create')
try {
    foreach ($file in 'NomorSorter.jar', 'NomorSorterUpdater.jar', 'jalankan.bat', 'jalankan-updater.bat', 'README.md', 'VERSION', 'src/NomorSorter.java') {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $root $file), "NomorSorter/$file") | Out-Null
    }
} finally { $archive.Dispose() }
Write-Output "Selesai: $zip"
