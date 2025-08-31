# FlowDeconstruct - Compile and Run Script (Maven-based)
Write-Host "FlowDeconstruct - Compile and Run" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Set JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = 'C:\Program Files\sapjvm\sapjvm_8'
    $env:PATH = $env:PATH + ';C:\Program Files\sapjvm\sapjvm_8\bin'
}

# Verify Java
Write-Host "Java version:" -ForegroundColor Yellow
java -version

# Ensure Maven is available
$mvnVersion = (& mvn -v 2>&1)
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven not found on PATH. Please install Apache Maven to build and run the project." -ForegroundColor Red
    exit 1
}

# Build with Maven (fetches all dependencies declared in pom.xml)
Write-Host "Building with Maven (skipping tests)..." -ForegroundColor Yellow
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# Run the shaded JAR produced by the Maven Shade plugin
$jarPath = "target\FlowDeconstruct.jar"
if (!(Test-Path $jarPath)) {
    Write-Host "Shaded JAR not found at $jarPath" -ForegroundColor Red
    exit 1
}

Write-Host "Starting application..." -ForegroundColor Yellow
try {
    & java -jar $jarPath
} catch {
    Write-Host "Failed to start application!" -ForegroundColor Red
}

Write-Host "Done." -ForegroundColor Green