# FlowDeconstruct - Run JAR Script
Write-Host "Starting FlowDeconstruct from JAR..." -ForegroundColor Green

# Set JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = 'C:\Program Files\sapjvm\sapjvm_8'
    $env:PATH = $env:PATH + ';C:\Program Files\sapjvm\sapjvm_8\bin'
}

# Check if JAR exists
if (!(Test-Path "target\FlowDeconstruct.jar")) {
    Write-Host "JAR file not found! Please run build-jar.ps1 first." -ForegroundColor Red
    exit 1
}

# Run the JAR
Write-Host "Launching FlowDeconstruct..." -ForegroundColor Yellow
Write-Host "The application will start minimized in the system tray." -ForegroundColor Cyan
Write-Host "Use Ctrl+Shift+F to show/hide the main window." -ForegroundColor Cyan

try {
    & java -jar "target\FlowDeconstruct.jar"
} catch {
    Write-Host "Failed to start FlowDeconstruct!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "FlowDeconstruct has exited." -ForegroundColor Yellow