#!/bin/bash

# Update package lists
sudo apt update

# Install Java (OpenJDK 17)
sudo apt install -y openjdk-17-jdk

# Install Maven
sudo apt install -y maven

# Install MySQL Server
sudo apt install -y mysql-server

# Start MySQL Service
sudo systemctl start mysql
sudo systemctl enable mysql

# Install Git
sudo apt install -y git

# Clone the repository
git clone https://github.com/your-repo/grow-earn-backend-java.git

# Navigate to the project directory
cd grow-earn-backend-java

# Run Maven to install dependencies
./mvnw clean install

#check one

# Print completion message
echo "All dependencies installed successfully!"
