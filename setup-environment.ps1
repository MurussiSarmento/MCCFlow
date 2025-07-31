# FlowDeconstruct - SAP JVM 8 Environment Setup Script
# This script configures the existing SAP JVM 8 and installs Maven for the FlowDeconstruct project

Write-Host "FlowDeconstruct SAP JVM 8 Environment Setup" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

# Check if running as administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "This script requires Administrator privileges. Please run as Administrator." -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    exit 1
}

# Check if Chocolatey is installed
if (!(Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Chocolatey..." -ForegroundColor Yellow
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    
    # Refresh environment variables
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
} else {
    Write-Host "Chocolatey is already installed" -ForegroundColor Green
}

# Check for existing SAP JVM 8
Write-Host "Checking for SAP JVM 8..." -ForegroundColor Yellow
$sapJvmPath = "C:\Program Files\sapjvm\sapjvm_8"

if (Test-Path $sapJvmPath) {
    Write-Host "✓ SAP JVM 8 found at: $sapJvmPath" -ForegroundColor Green
} else {
    Write-Host "✗ SAP JVM 8 not found at expected location: $sapJvmPath" -ForegroundColor Red
    Write-Host "Please ensure SAP JVM 8 is installed before continuing." -ForegroundColor Yellow
    exit 1
}

# Install Maven
Write-Host "Installing Apache Maven..." -ForegroundColor Yellow
choco install maven -y

# Set JAVA_HOME to SAP JVM 8 installation
$sapJvmHome = "C:\Program Files\sapjvm\sapjvm_8"
if (Test-Path $sapJvmHome) {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $sapJvmHome, "Machine")
    Write-Host "JAVA_HOME set to: $sapJvmHome" -ForegroundColor Green
    
    # Add SAP JVM to PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $sapJvmBinPath = Join-Path $sapJvmHome "bin"
    if ($currentPath -notlike "*$sapJvmBinPath*") {
        $newPath = "$currentPath;$sapJvmBinPath"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
        Write-Host "SAP JVM 8 added to PATH" -ForegroundColor Green
    }
} else {
    Write-Host "Error: SAP JVM 8 installation directory not found" -ForegroundColor Red
    exit 1
}

# Refresh environment variables
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:JAVA_HOME = [System.Environment]::GetEnvironmentVariable("JAVA_HOME","Machine")

Write-Host "" -ForegroundColor White
Write-Host "Installation completed!" -ForegroundColor Green
Write-Host "" -ForegroundColor White

# Verify installations
Write-Host "Verifying installations..." -ForegroundColor Yellow
try {
    $javaExePath = Join-Path $env:JAVA_HOME "bin\java.exe"
    $javaVersion = & $javaExePath -version 2>&1 | Select-String "version"
    Write-Host "Java (SAP JVM): $javaVersion" -ForegroundColor Green
    
    $javaVendor = & $javaExePath -version 2>&1 | Select-String "SAP"
    if ($javaVendor) {
        Write-Host "✓ SAP JVM detected: $javaVendor" -ForegroundColor Green
    }
} catch {
    Write-Host "Java verification failed" -ForegroundColor Red
}

try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    Write-Host "Maven: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "Maven verification failed" -ForegroundColor Red
}

Write-Host "" -ForegroundColor White
Write-Host "SAP JVM 8 Environment Information:" -ForegroundColor Cyan
Write-Host "• Using existing SAP JVM 8 installation" -ForegroundColor White
Write-Host "• SAP JVM 8 is optimized for SAP applications" -ForegroundColor White
Write-Host "• Compatible with Java 8 applications and Maven projects" -ForegroundColor White
Write-Host "• Includes SAP-specific optimizations and enterprise features" -ForegroundColor White
Write-Host "" -ForegroundColor White
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Close and reopen your terminal to refresh environment variables" -ForegroundColor White
Write-Host "2. Run 'mvn clean compile' to build the project" -ForegroundColor White
Write-Host "3. Run 'mvn exec:java' to start the application" -ForegroundColor White
Write-Host "" -ForegroundColor White
Write-Host "Setup completed successfully!" -ForegroundColor Green