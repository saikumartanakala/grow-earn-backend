
package com.growearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class to hold admin accounts from application.properties
 * 
 * Usage in application.properties:
 * app.admin.accounts[0].email=admin@gmail.com
 * app.admin.accounts[0].password=yourSecurePassword
 * app.admin.accounts[1].email=superadmin@gmail.com
 * app.admin.accounts[1].password=anotherPassword
 * 
 * For production, use environment variables instead:
 * APP_ADMIN_ACCOUNTS_0_EMAIL=admin@gmail.com
 * APP_ADMIN_ACCOUNTS_0_PASSWORD=yourSecurePassword
 */
@Configuration
@ConfigurationProperties(prefix = "app.admin")
public class AdminAccountsConfig {

    private List<AdminAccount> accounts = new ArrayList<>();

    public List<AdminAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AdminAccount> accounts) {
        this.accounts = accounts;
    }

    public static class AdminAccount {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
