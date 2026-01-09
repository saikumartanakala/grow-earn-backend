#!/bin/bash

# Grow Earn Backend - Complete Installation Script
# This script installs all required dependencies for the Grow Earn backend project
# Supports Ubuntu/Debian, CentOS/RHEL, and provides Windows instructions

set -e  # Exit on any error

echo "🚀 Grow Earn Backend - Complete Installation Script"
echo "=================================================="

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if command -v apt-get >/dev/null 2>&1; then
        # Ubuntu/Debian
        echo "📦 Detected Ubuntu/Debian system"
        echo "🔄 Updating package lists..."
        sudo apt update

        echo "☕ Installing Java 17..."
        sudo apt install -y openjdk-17-jdk

        echo "🔧 Installing Maven..."
        sudo apt install -y maven

        echo "🗄️ Installing MySQL 8.0..."
        sudo apt install -y mysql-server-8.0
        sudo systemctl start mysql
        sudo systemctl enable mysql

        echo "🔒 Running MySQL secure installation..."
        sudo mysql_secure_installation

    elif command -v yum >/dev/null 2>&1; then
        # CentOS/RHEL
        echo "📦 Detected CentOS/RHEL system"
        echo "🔄 Updating package lists..."
        sudo yum update -y

        echo "☕ Installing Java 17..."
        sudo yum install -y java-17-openjdk-devel

        echo "🔧 Installing Maven..."
        sudo yum install -y maven

        echo "🗄️ Installing MySQL 8.0..."
        sudo yum install -y mysql-server
        sudo systemctl start mysqld
        sudo systemctl enable mysqld

        echo "🔒 Running MySQL secure installation..."
        sudo mysql_secure_installation

    else
        echo "❌ Unsupported Linux distribution"
        echo "Please install manually: Java 17, Maven 3.6+, MySQL 8.0+"
        exit 1
    fi

elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "📦 Detected macOS system"

    if ! command -v brew >/dev/null 2>&1; then
        echo "🍺 Installing Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi

    echo "☕ Installing Java 17..."
    brew install openjdk@17

    echo "🔧 Installing Maven..."
    brew install maven

    echo "🗄️ Installing MySQL 8.0..."
    brew install mysql
    brew services start mysql

    echo "🔒 Please run: mysql_secure_installation"

else
    echo "❌ Unsupported operating system: $OSTYPE"
    echo ""
    echo "📋 Manual Installation Instructions:"
    echo "===================================="
    echo ""
    echo "1. Install Java 17:"
    echo "   - Download from: https://adoptium.net/temurin/releases/"
    echo "   - Choose JDK 17 (LTS)"
    echo "   - Add to PATH environment variable"
    echo ""
    echo "2. Install Maven 3.6+:"
    echo "   - Download from: https://maven.apache.org/download.cgi"
    echo "   - Add to PATH environment variable"
    echo ""
    echo "3. Install MySQL 8.0+:"
    echo "   - Download MySQL Installer from: https://dev.mysql.com/downloads/installer/"
    echo "   - Run installer and select MySQL Server 8.0+"
    echo ""
    echo "4. Verify installations:"
    echo "   java -version    # Should show Java 17"
    echo "   mvn -version     # Should show Maven 3.6+"
    echo "   mysql --version  # Should show MySQL 8.0+"
    exit 1
fi

echo ""
echo "✅ Installation completed successfully!"
echo ""
echo "🔍 Verification:"
echo "java -version"
java -version
echo ""
echo "mvn -version"
mvn -version
echo ""
echo "mysql --version"
mysql --version

echo ""
echo "📋 Next Steps:"
echo "=============="
echo ""
echo "1. Setup Database:"
echo "   sudo mysql -u root -p"
echo "   CREATE DATABASE IF NOT EXISTS grow_earn;"
echo "   CREATE USER 'growearn_user'@'localhost' IDENTIFIED BY 'secure_password_123';"
echo "   GRANT ALL PRIVILEGES ON grow_earn.* TO 'growearn_user'@'localhost';"
echo "   FLUSH PRIVILEGES;"
echo "   EXIT;"
echo ""
echo "2. Import Schema:"
echo "   cd grow-earn-backend-java/grow-earn-backend-java"
echo "   mysql -u growearn_user -p grow_earn < grow-earn-backend/db-schema.sql"
echo ""
echo "3. Configure Application:"
echo "   Edit src/main/resources/application.properties"
echo "   Update database credentials and JWT secret"
echo ""
echo "4. Run Application:"
echo "   mvn spring-boot:run"
echo ""
echo "5. Test Installation:"
echo "   curl http://localhost:5000/api/test"
echo ""
echo "🎉 Ready to deploy Grow Earn Backend!"