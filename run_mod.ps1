$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Error 'Java is not installed or not on PATH. Install JDK 17 and retry.'
    exit 1
}

if (-not $env:JAVA_HOME) {
    $javaBin = (Get-Command java).Source
    $env:JAVA_HOME = Split-Path -Parent $javaBin
}

Write-Host 'Project root: ' $projectRoot
Write-Host 'JAVA_HOME: ' $env:JAVA_HOME

Write-Host 'Stopping stale Gradle daemons...'
& "$projectRoot\gradlew.bat" --stop --console=plain --no-daemon

Write-Host 'Running clean compile...'
& "$projectRoot\gradlew.bat" clean compileJava --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error 'Compile failed. Fix compile errors before launching the game.'
    exit $LASTEXITCODE
}

Write-Host 'Launching the Forge client...'
& "$projectRoot\gradlew.bat" runClient --console=plain --no-daemon
exit $LASTEXITCODE
