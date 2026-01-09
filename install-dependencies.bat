@echo off
REM Grow Earn Backend - Windows Installation Script
REM This script installs all required dependencies for Windows

echo 🚀 Grow Earn Backend - Windows Installation Script
echo ==================================================

echo 📋 Prerequisites Check:
echo =======================
echo.
echo This script requires administrative privileges.
echo Please run as Administrator if not already.
echo.
echo Required Software:
echo 1. Java 17 JDK
echo 2. Maven 3.6+
echo 3. MySQL 8.0+
echo.

set /p CONTINUE="Do you want to proceed with installation? (y/n): "
if /i not "%CONTINUE%"=="y" goto :exit

echo.
echo 🔍 Checking for Chocolatey (Windows Package Manager)...
echo.

where choco >nul 2>nul
if %errorlevel% neq 0 (
    echo 📦 Installing Chocolatey...
    powershell -Command "Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))"
) else (
    echo ✅ Chocolatey is already installed
)

echo.
echo ☕ Installing Java 17...
choco install openjdk17 -y

echo.
echo 🔧 Installing Maven...
choco install maven -y

echo.
echo 🗄️ Installing MySQL 8.0...
choco install mysql -y

echo.
echo 🔄 Refreshing environment variables...
call refreshenv

echo.
echo 🔍 Verification:
echo ================
echo.

echo Java version:
java -version
if %errorlevel% neq 0 (
    echo ❌ Java installation failed
    goto :error
)

echo.
echo Maven version:
mvn -version
if %errorlevel% neq 0 (
    echo ❌ Maven installation failed
    goto :error
)

echo.
echo MySQL version:
mysql --version
if %errorlevel% neq 0 (
    echo ❌ MySQL installation failed
    goto :error
)

echo.
echo ✅ All dependencies installed successfully!
echo.
echo 📋 Next Steps:
echo ==============
echo.
echo 1. Configure MySQL:
echo    - Run MySQL Installer (if not auto-configured)
echo    - Set root password
echo    - Create database user
echo.
echo 2. Setup Database:
echo    mysql -u root -p
echo    CREATE DATABASE IF NOT EXISTS grow_earn;
echo    CREATE USER 'growearn_user'@'localhost' IDENTIFIED BY 'secure_password_123';
echo    GRANT ALL PRIVILEGES ON grow_earn.* TO 'growearn_user'@'localhost';
echo    FLUSH PRIVILEGES;
echo    EXIT;
echo.
echo 3. Import Schema:
echo    cd grow-earn-backend-java\grow-earn-backend-java
echo    mysql -u growearn_user -p grow_earn ^< grow-earn-backend\db-schema.sql
echo.
echo 4. Configure Application:
echo    Edit src\main\resources\application.properties
echo    Update database credentials and JWT secret
echo.
echo 5. Run Application:
echo    mvn spring-boot:run
echo.
echo 6. Test Installation:
echo    curl http://localhost:5000/api/test
echo.
echo 🎉 Ready to deploy Grow Earn Backend!
goto :end

:error
echo.
echo ❌ Installation failed. Please check the errors above.
echo You can install manually from:
echo - Java 17: https://adoptium.net/temurin/releases/
echo - Maven: https://maven.apache.org/download.cgi
echo - MySQL: https://dev.mysql.com/downloads/installer/
goto :end

:exit
echo.
echo Installation cancelled by user.

:end
echo.
pause