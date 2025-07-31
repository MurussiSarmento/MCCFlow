# FlowDeconstruct - Compile and Run Script
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

# Create directories
if (!(Test-Path "target\classes")) {
    New-Item -ItemType Directory -Path "target\classes" -Force | Out-Null
}

$libDir = "target\lib"
if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

# Download Jackson dependencies
Write-Host "Downloading dependencies..." -ForegroundColor Yellow

$jacksonVersion = "2.15.2"
$baseUrl = "https://repo1.maven.org/maven2/com/fasterxml/jackson"

$dependencies = @(
    @{name="jackson-core"; url="$baseUrl/core/jackson-core/$jacksonVersion/jackson-core-$jacksonVersion.jar"},
    @{name="jackson-databind"; url="$baseUrl/core/jackson-databind/$jacksonVersion/jackson-databind-$jacksonVersion.jar"},
    @{name="jackson-annotations"; url="$baseUrl/core/jackson-annotations/$jacksonVersion/jackson-annotations-$jacksonVersion.jar"}
)

foreach ($dep in $dependencies) {
    $jarFile = "$libDir\$($dep.name)-$jacksonVersion.jar"
    if (!(Test-Path $jarFile)) {
        Write-Host "Downloading $($dep.name)..." -ForegroundColor Cyan
        try {
            Invoke-WebRequest -Uri $dep.url -OutFile $jarFile
        } catch {
            Write-Host "Failed to download $($dep.name)" -ForegroundColor Red
        }
    }
}

# Build classpath
$jars = Get-ChildItem -Path $libDir -Filter "*.jar" | ForEach-Object { $_.FullName }
$classpath = "target\classes" + ";" + ($jars -join ";")

# Find Java files
$javaFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse
if ($javaFiles.Count -eq 0) {
    Write-Host "No Java files found!" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($javaFiles.Count) Java files" -ForegroundColor Green

# Compile
Write-Host "Compiling..." -ForegroundColor Yellow
$sourceFiles = $javaFiles | ForEach-Object { $_.FullName }

try {
    & javac -cp $classpath -d "target\classes" @sourceFiles
    Write-Host "Compilation successful!" -ForegroundColor Green
} catch {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

# Run application
Write-Host "Starting application..." -ForegroundColor Yellow
try {
    & java -cp $classpath com.sap.flowdeconstruct.FlowDeconstructApp
} catch {
    Write-Host "Failed to start application!" -ForegroundColor Red
}

Write-Host "Done." -ForegroundColor Green