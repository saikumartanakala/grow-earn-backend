package com.growearn.config;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Production-ready Admin Seeder
 * 
 * Automatically creates admin users on application startup if they don't exist.
 * Configure admin accounts in application.properties:
 * 
 * app.admin.accounts[0].email=admin@gmail.com
 * app.admin.accounts[0].password=securePassword123
 * app.admin.accounts[1].email=superadmin@gmail.com
 * app.admin.accounts[1].password=anotherSecurePass
 * 
 * For production, use environment variables:
 * APP_ADMIN_ACCOUNTS_0_EMAIL=admin@gmail.com
 * APP_ADMIN_ACCOUNTS_0_PASSWORD=securePassword123
 */
@Configuration
public class AdminSeeder {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    @Bean
    CommandLineRunner seedAdminUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminAccountsConfig adminAccountsConfig
    ) {
        return args -> {
            List<AdminAccountsConfig.AdminAccount> accounts = adminAccountsConfig.getAccounts();
            
            if (accounts == null || accounts.isEmpty()) {
                logger.info("No admin accounts configured. Skipping admin seeding.");
                return;
            }

            for (AdminAccountsConfig.AdminAccount account : accounts) {
                String email = account.getEmail();
                String password = account.getPassword();

                if (email == null || email.isBlank() || password == null || password.isBlank()) {
                    logger.warn("Skipping invalid admin account configuration (empty email or password)");
                    continue;
                }

                // Check if admin already exists
                if (userRepository.findByEmailAndRole(email, Role.ADMIN).isPresent()) {
                    logger.info("Admin user already exists: {}", email);
                    continue;
                }

                // Check if email exists with different role
                if (userRepository.findByEmail(email).isPresent()) {
                    logger.warn("Email {} already exists with a different role. Skipping admin creation.", email);
                    continue;
                }

                // Create new admin user
                User adminUser = new User(
                        email,
                        passwordEncoder.encode(password),
                        Role.ADMIN
                );

                userRepository.save(adminUser);
                logger.info("✅ Admin user created successfully: {}", email);
            }
        };
    }
}
