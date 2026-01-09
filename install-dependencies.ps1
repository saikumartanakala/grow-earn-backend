#Requires -RunAsAdministrator

<#
.SYNOPSIS
    Grow Earn Backend - Complete Installation Script for Windows
.DESCRIPTION
    This script installs all required dependencies for the Grow Earn backend project on Windows
.NOTES
    Requires administrative privileges
#>

param(
    [switch]$SkipVerification
)

Write-Host "🚀 Grow Earn Backend - Windows PowerShell Installation Script" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Green

# Check for administrator privileges
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "❌ This script requires administrative privileges." -ForegroundColor Red
    Write-Host "Please run PowerShell as Administrator and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host "📋 Prerequisites Check:" -ForegroundColor Cyan
Write-Host "======================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will install:"
Write-Host "1. Java 17 JDK"
Write-Host "2. Maven 3.6+"
Write-Host "3. MySQL 8.0+"
Write-Host ""

$continue = Read-Host "Do you want to proceed with installation? (y/n)"
if ($continue -ne 'y' -and $continue -ne 'Y') {
    Write-Host "Installation cancelled by user." -ForegroundColor Yellow
    exit 0
}

# Check for Chocolatey
Write-Host ""
Write-Host "🔍 Checking for Chocolatey (Windows Package Manager)..." -ForegroundColor Cyan

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "📦 Installing Chocolatey..." -ForegroundColor Yellow
    try {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
        # Refresh environment
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    } catch {
        Write-Host "❌ Failed to install Chocolatey: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ Chocolatey is already installed" -ForegroundColor Green
}

# Install Java 17
Write-Host ""
Write-Host "☕ Installing Java 17..." -ForegroundColor Cyan
try {
    choco install openjdk17 -y
    # Refresh environment
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
} catch {
    Write-Host "❌ Failed to install Java 17: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Install Maven
Write-Host ""
Write-Host "🔧 Installing Maven..." -ForegroundColor Cyan
try {
    choco install maven -y
    # Refresh environment
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
} catch {
    Write-Host "❌ Failed to install Maven: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Install MySQL
Write-Host ""
Write-Host "🗄️ Installing MySQL 8.0..." -ForegroundColor Cyan
try {
    choco install mysql -y
    # Refresh environment
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
} catch {
    Write-Host "❌ Failed to install MySQL: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verification
if (-not $SkipVerification) {
    Write-Host ""
    Write-Host "🔍 Verification:" -ForegroundColor Cyan
    Write-Host "=" * 15 -ForegroundColor Cyan
    Write-Host ""

    # Check Java
    Write-Host "Java version:" -ForegroundColor Yellow
    try {
        $javaVersion = java -version 2>&1
        Write-Host $javaVersion -ForegroundColor Green
    } catch {
        Write-Host "❌ Java installation failed or not found in PATH" -ForegroundColor Red
        Write-Host "Please restart your terminal and try again." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Maven version:" -ForegroundColor Yellow
    try {
        $mvnVersion = mvn -version 2>&1
        Write-Host $mvnVersion -ForegroundColor Green
    } catch {
        Write-Host "❌ Maven installation failed or not found in PATH" -ForegroundColor Red
        Write-Host "Please restart your terminal and try again." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "MySQL version:" -ForegroundColor Yellow
    try {
        $mysqlVersion = mysql --version 2>&1
        Write-Host $mysqlVersion -ForegroundColor Green
    } catch {
        Write-Host "❌ MySQL installation failed or not found in PATH" -ForegroundColor Red
        Write-Host "Please restart your terminal and try again." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "✅ All dependencies installed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "=" * 12 -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Configure MySQL:" -ForegroundColor White
Write-Host "   - Run MySQL Installer (if not auto-configured)" -ForegroundColor Gray
Write-Host "   - Set root password" -ForegroundColor Gray
Write-Host "   - Create database user" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Setup Database:" -ForegroundColor White
Write-Host "   mysql -u root -p" -ForegroundColor Gray
Write-Host "   CREATE DATABASE IF NOT EXISTS grow_earn;" -ForegroundColor Gray
Write-Host "   CREATE USER 'growearn_user'@'localhost' IDENTIFIED BY 'secure_password_123';" -ForegroundColor Gray
Write-Host "   GRANT ALL PRIVILEGES ON grow_earn.* TO 'growearn_user'@'localhost';" -ForegroundColor Gray
Write-Host "   FLUSH PRIVILEGES;" -ForegroundColor Gray
Write-Host "   EXIT;" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Import Schema:" -ForegroundColor White
Write-Host "   cd grow-earn-backend-java\grow-earn-backend-java" -ForegroundColor Gray
Write-Host "   mysql -u growearn_user -p grow_earn < grow-earn-backend\db-schema.sql" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Configure Application:" -ForegroundColor White
Write-Host "   Edit src\main\resources\application.properties" -ForegroundColor Gray
Write-Host "   Update database credentials and JWT secret" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Run Application:" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "6. Test Installation:" -ForegroundColor White
Write-Host "   curl http://localhost:5000/api/test" -ForegroundColor Gray
Write-Host ""
Write-Host "🎉 Ready to deploy Grow Earn Backend!" -ForegroundColor Green
Write-Host ""
Write-Host "Note: You may need to restart your terminal/command prompt" -ForegroundColor Yellow
Write-Host "for the new PATH variables to take effect." -ForegroundColor Yellow