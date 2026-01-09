# Grow Earn Backend (Java Spring Boot)

A comprehensive backend API for the Grow Earn platform built with Spring Boot 3.2.5, providing authentication, campaign management, and viewer task functionality.

## 🚀 Quick Start

### Prerequisites

Before running this application, ensure you have the following installed on your server:

#### 1. **Java 17** (Required)
```bash
# Check Java version
java -version

# Expected output: Java 17.x.x
```

#### 2. **Maven 3.6+** (Required)
```bash
# Check Maven version
mvn -version

# Expected output: Apache Maven 3.6.x or higher
```

#### 3. **MySQL 8.0+** (Required)
```bash
# Check MySQL version
mysql --version

# Expected output: mysql Ver 8.0.x
```

### 🚀 **One-Command Installation Scripts**

For quick setup, use the provided installation scripts:

#### **Linux/macOS:**
```bash
# Make script executable and run
chmod +x install-dependencies.sh
./install-dependencies.sh
```

#### **Windows (Command Prompt):**
```cmd
install-dependencies.bat
```

#### **Windows (PowerShell):**
```powershell
# Run as Administrator
.\install-dependencies.ps1
```

**What the scripts install:**
- ✅ Java 17 JDK
- ✅ Maven 3.6+
- ✅ MySQL 8.0+ Server
- ✅ Automatic verification
- ✅ Next steps guidance

### Database Setup

#### 1. **Secure MySQL Installation**
```bash
# Run MySQL secure installation
sudo mysql_secure_installation
```

#### 2. **Create Database and User**
```bash
# Login to MySQL as root
mysql -u root -p

# Create database and user (run these commands in MySQL shell)
CREATE DATABASE IF NOT EXISTS grow_earn;
CREATE USER 'growearn_user'@'localhost' IDENTIFIED BY 'secure_password_123';
GRANT ALL PRIVILEGES ON grow_earn.* TO 'growearn_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 3. **Import Database Schema**
```bash
# Navigate to project directory
cd grow-earn-backend-java/grow-earn-backend-java

# Import the database schema
mysql -u growearn_user -p grow_earn < grow-earn-backend/db-schema.sql
```

### Sample Data

The database comes pre-populated with sample data for testing:

#### Users
- **Creator**: `creator@example.com` / `password`
- **Viewer**: `viewer@example.com` / `password`

#### Sample Campaign
- YouTube campaign with subscriber, view, like, and comment goals
- Multiple viewer tasks available for claiming

#### Test the Sample Data
```bash
# Login as viewer
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"viewer@example.com","password":"password"}'

# Use the returned JWT token to access dashboard
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:5000/api/viewer/dashboard
```

### Application Configuration

#### 1. **Update Database Credentials**
Edit `src/main/resources/application.properties`:

```properties
# Update these values according to your MySQL setup
spring.datasource.username=growearn_user
spring.datasource.password=secure_password_123
spring.datasource.url=jdbc:mysql://localhost:3306/grow_earn?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

#### 2. **Update JWT Secret** (Important for Security)
```properties
# Generate a secure random secret key
jwt.secret=YourSuperSecureRandomKeyHere12345678901234567890
```

### Installation Steps

#### 1. **Clone/Download the Project**
```bash
# If using git
git clone <repository-url>
cd grow-earn-backend-java/grow-earn-backend-java

# Or extract the downloaded ZIP file
unzip grow-earn-backend-java.zip
cd grow-earn-backend-java/grow-earn-backend-java
```

#### 2. **Install Dependencies**
```bash
# Maven will automatically download all required dependencies
mvn clean install
```

#### 3. **Run the Application**
```bash
# Development mode
mvn spring-boot:run

# Or using full plugin name
mvn org.springframework.boot:spring-boot-maven-plugin:run

# Production mode (build JAR first)
mvn clean package
java -jar target/grow-earn-backend-java-0.0.1-SNAPSHOT.jar
```

#### 4. **Verify Installation**
```bash
# Test the API endpoint
curl http://localhost:5000/api/test

# Expected response: "Backend is running successfully!"
```

## 📋 Dependencies Overview

### Core Framework
- **Spring Boot 3.2.5** - Main framework
- **Java 17** - Runtime environment
- **Maven 3.6+** - Build tool

### Database
- **MySQL 8.0+** - Primary database
- **MySQL Connector/J** - JDBC driver
- **Spring Data JPA** - ORM framework
- **Hibernate** - JPA implementation

### Security
- **Spring Security** - Authentication & authorization
- **JJWT (JSON Web Tokens)** - JWT token handling
- **BCrypt** - Password hashing

### Web
- **Spring Web** - REST API framework
- **Jackson** - JSON processing
- **Tomcat** - Embedded web server

### Development Tools
- **Spring Boot DevTools** - Development utilities
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework

## 🔧 Configuration Files

### application.properties
```properties
# Server Configuration
server.port=5000
spring.application.name=grow-earn-backend-java

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/grow_earn?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=growearn_user
spring.datasource.password=secure_password_123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# JWT Configuration
jwt.secret=YourSuperSecureRandomKeyHere12345678901234567890
```

## 🌐 API Endpoints

### Authentication
- `POST /api/auth/login` - User login

### Viewer Dashboard
- `GET /api/viewer/dashboard` - Get viewer dashboard data (requires JWT)

### Test Endpoints
- `GET /api/test` - Test endpoint (no authentication required)

### Creator Stats
- `GET /api/creator/stats/{creatorId}` - Get creator statistics

## 🎨 Frontend Integration

### CORS Configuration
The backend is configured to accept requests from frontend applications running on:
- **Port 5173** (Vite development server)
- **Origin**: `http://localhost:5173`

### Sample Frontend Code
```javascript
// Login example
const login = async (email, password) => {
  const response = await fetch('http://localhost:5000/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  return response.json();
};

// Get dashboard data
const getDashboard = async (token) => {
  const response = await fetch('http://localhost:5000/api/viewer/dashboard', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  return response.json();
};
```

## 🔒 Security Features

- **JWT Authentication** - Token-based authentication
- **Password Encryption** - BCrypt hashing
- **CORS Configuration** - Cross-origin resource sharing
- **Role-based Access** - Creator and User roles

## 🚀 Deployment

### Development
```bash
mvn spring-boot:run
```

### Production
```bash
# Build the application
mvn clean package -DskipTests

# Run the JAR file
java -jar target/grow-earn-backend-java-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/grow-earn-backend-java-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 🐛 Troubleshooting

### Common Issues

1. **Port 5000 already in use:**
   ```bash
   # Find process using port 5000
   netstat -ano | findstr :5000
   # Kill the process or change port in application.properties
   ```

2. **Database connection failed:**
   - Verify MySQL is running: `sudo systemctl status mysql`
   - Check credentials in `application.properties`
   - Ensure database exists: `mysql -u growearn_user -p -e "SHOW DATABASES;"`

3. **Java version issues:**
   ```bash
   java -version
   # Should show Java 17
   ```

4. **Maven build fails:**
   ```bash
   mvn clean
   mvn install
   ```

### Logs
Application logs are available in the console output. For production, consider configuring log files.

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Verify all prerequisites are installed
3. Ensure database is properly configured
4. Check application logs for error messages

## 📝 License

This project is licensed under the MIT License.