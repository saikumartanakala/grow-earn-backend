-- ===============================
-- USERS TABLE (Update existing)
-- ===============================

-- ALTER TABLE users ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255) UNIQUE;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS last_ip VARCHAR(100);
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT 'ACTIVE';
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_until TIMESTAMP NULL;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
-- ===============================
-- DEVICE REGISTRY TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS `device_registry` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `device_fingerprint` VARCHAR(255) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `first_ip` VARCHAR(100),
  `last_ip` VARCHAR(100),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_seen_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_device_registry_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===============================
-- LOGIN AUDIT TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS `login_audit` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT,
  `ip_address` VARCHAR(100),
  `device_fingerprint` VARCHAR(255),
  `user_agent` VARCHAR(512),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_login_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Profile fields (run these ALTER statements):
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS channel_name VARCHAR(100);
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_pic_url VARCHAR(500);
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);

-- ===============================
-- USER VIOLATIONS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS `user_violations` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `violation_type` VARCHAR(50) NOT NULL,
  `description` TEXT,
  `strike_count` INT DEFAULT 0,
  `action_taken` VARCHAR(50),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  INDEX `idx_user_violations_user_id` (`user_id`),
  INDEX `idx_user_violations_type` (`violation_type`),
  CONSTRAINT `fk_user_violations_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===============================
-- REVOKED TOKENS TABLE
-- ===============================
-- Drop and recreate to use token_hash for indexing
DROP TABLE IF EXISTS `revoked_tokens`;
CREATE TABLE IF NOT EXISTS `revoked_tokens` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `token_hash` VARCHAR(64) NOT NULL UNIQUE,
  `token` TEXT NOT NULL,
  `revoked_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `revoked_by` BIGINT,
  `reason` VARCHAR(100),
  INDEX `idx_revoked_tokens_user_id` (`user_id`),
  INDEX `idx_revoked_tokens_token_hash` (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===============================
-- TASKS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS `tasks` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `campaign_id` BIGINT NOT NULL,
  `task_type` VARCHAR(32) NOT NULL,
  `target_link` TEXT,
  `earning` DOUBLE DEFAULT 0,
  `status` VARCHAR(32) DEFAULT 'OPEN',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_tasks_campaign_id` (`campaign_id`),
  INDEX `idx_tasks_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `viewer_tasks` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` BIGINT NOT NULL,
  `viewer_id` BIGINT NOT NULL,
  `status` VARCHAR(32) DEFAULT 'ASSIGNED',
  `proof` TEXT,
  `assigned_at` TIMESTAMP NULL,
  `submitted_at` TIMESTAMP NULL,
  `completed_at` TIMESTAMP NULL,
  `approved_by` BIGINT NULL,
  `rejection_reason` TEXT NULL,
  INDEX `idx_viewer_tasks_viewer_id` (`viewer_id`),
  INDEX `idx_viewer_tasks_status` (`status`),
  INDEX `idx_viewer_tasks_viewer_status` (`viewer_id`, `status`),
  INDEX `idx_viewer_tasks_task_id` (`task_id`),
  CONSTRAINT `fk_viewer_tasks_task` FOREIGN KEY (`task_id`) REFERENCES `tasks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wallet` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `viewer_id` BIGINT NOT NULL UNIQUE,
  `balance` DOUBLE DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `earnings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `viewer_id` BIGINT NOT NULL,
  `task_id` BIGINT,
  `amount` DOUBLE DEFAULT 0,
  `type` VARCHAR(50) DEFAULT 'TASK_COMPLETION',
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create default admin user (password: admin123 - should be changed in production)
-- INSERT IGNORE INTO users (email, password, role) VALUES ('admin@growearn.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.H8BjZL1GJoI1n1ADmO', 'ADMIN');
