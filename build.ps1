# Builds the mod. Output: build\libs\birds-in-every-biome-1.1.0.jar
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"
& "$PSScriptRoot\gradlew.bat" build @args
