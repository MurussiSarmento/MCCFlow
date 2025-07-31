# FlowDeconstruct - Build JAR Script
Write-Host "FlowDeconstruct - Building Executable JAR" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

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

# Create manifest file
Write-Host "Creating manifest..." -ForegroundColor Yellow
$jacksonVersion = "2.15.2"
$classPathEntries = @(
    "lib/jackson-core-$jacksonVersion.jar",
    "lib/jackson-databind-$jacksonVersion.jar",
    "lib/jackson-annotations-$jacksonVersion.jar"
)
$classPath = ($classPathEntries -join " ")

$manifestContent = @"
Manifest-Version: 1.0
Main-Class: com.sap.flowdeconstruct.FlowDeconstructApp
Class-Path: $classPath
"@

$manifestDir = "target\META-INF"
if (!(Test-Path $manifestDir)) {
    New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null
}

$manifestContent | Out-File -FilePath "$manifestDir\MANIFEST.MF" -Encoding ASCII

# Create JAR (without extracting dependencies)
Write-Host "Creating executable JAR..." -ForegroundColor Yellow

# Ensure lib directory exists in target
$libTargetDir = "target\lib"
if (!(Test-Path $libTargetDir)) {
    New-Item -ItemType Directory -Path $libTargetDir -Force | Out-Null
}

# Copy dependencies to target/lib
foreach ($jar in $jars) {
    $jarName = Split-Path $jar -Leaf
    $destPath = "$libTargetDir\$jarName"
    
    # Only copy if source and destination are different
    if ($jar -ne $destPath) {
        Write-Host "Copying $jarName to lib directory..." -ForegroundColor Cyan
        Copy-Item -Path $jar -Destination $destPath -Force
    } else {
        Write-Host "$jarName already exists in target/lib, skipping..." -ForegroundColor Cyan
    }
}

# Create JAR with classes only
Write-Host "Creating final JAR..." -ForegroundColor Yellow
Set-Location "target\classes"
& jar -cfm "..\FlowDeconstruct.jar" "..\META-INF\MANIFEST.MF" .
Set-Location "..\.."

if (Test-Path "target\FlowDeconstruct.jar") {
    Write-Host "Executable JAR created successfully: target\FlowDeconstruct.jar" -ForegroundColor Green
    
    # Test the JAR
    Write-Host "Testing JAR..." -ForegroundColor Yellow
    Start-Process -FilePath "java" -ArgumentList "-jar", "target\FlowDeconstruct.jar" -WindowStyle Hidden
    
    Write-Host "JAR is running in background. Check system tray for FlowDeconstruct icon." -ForegroundColor Green
} else {
    Write-Host "Failed to create JAR!" -ForegroundColor Red
    exit 1
}

Write-Host "Build completed successfully!" -ForegroundColor Green