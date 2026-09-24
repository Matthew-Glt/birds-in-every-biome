# Launches a dev Minecraft client with the mod loaded (first run downloads game assets).
# Needs a JDK 25; this finds one (JAVA_HOME first, then the usual install locations).
$ErrorActionPreference = "Stop"

function Get-JdkMajor([string]$dir) {
    $release = Join-Path $dir "release"
    if (-not (Test-Path $release)) { return 0 }
    $match = Select-String -Path $release -Pattern 'JAVA_VERSION="(\d+)' | Select-Object -First 1
    if ($match) { return [int]$match.Matches[0].Groups[1].Value }
    return 0
}

function Find-Jdk25 {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($pattern in @(
            "$env:ProgramFiles\Microsoft\jdk-25*",
            "$env:ProgramFiles\Java\jdk-25*",
            "$env:ProgramFiles\Eclipse Adoptium\jdk-25*",
            "$env:LOCALAPPDATA\Programs\*\jdk-25*")) {
        $candidates += (Get-ChildItem $pattern -Directory -ErrorAction SilentlyContinue |
                ForEach-Object { $_.FullName })
    }
    foreach ($candidate in $candidates) {
        if ((Get-JdkMajor $candidate) -ge 25) { return $candidate }
    }
    throw "No JDK 25 found. Install one (for example: winget install Microsoft.OpenJDK.25) or set JAVA_HOME to a JDK 25."
}

$env:JAVA_HOME = Find-Jdk25
Write-Host "Using JAVA_HOME=$env:JAVA_HOME (Java $(Get-JdkMajor $env:JAVA_HOME))"
& "$PSScriptRoot\gradlew.bat" runClient @args
