# Launches a dev Minecraft client with the mod loaded (first run downloads game assets).
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"
& "$PSScriptRoot\gradlew.bat" runClient @args
