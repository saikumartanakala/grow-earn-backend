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
-- CAMPAIGNS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS `campaigns` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `creator_id` BIGINT NOT NULL,
  `title` VARCHAR(255),
  `description` TEXT,
  `platform` ENUM('YOUTUBE', 'INSTAGRAM', 'FACEBOOK', 'TWITTER') NOT NULL DEFAULT 'YOUTUBE',
  `goal_type` VARCHAR(20),
  `channel_name` VARCHAR(255),
  `channel_link` VARCHAR(500),
  `content_type` VARCHAR(20),
  `video_link` VARCHAR(500),
  `video_duration` VARCHAR(20),
  `subscriber_goal` INT DEFAULT 0,
  `views_goal` INT DEFAULT 0,
  `likes_goal` INT DEFAULT 0,
  `comments_goal` INT DEFAULT 0,
  `current_subscribers` INT DEFAULT 0,
  `current_views` INT DEFAULT 0,
  `current_likes` INT DEFAULT 0,
  `current_comments` INT DEFAULT 0,
  `subscriber_task_count` INT DEFAULT 0,
  `views_task_count` INT DEFAULT 0,
  `likes_task_count` INT DEFAULT 0,
  `comments_task_count` INT DEFAULT 0,
  `total_amount` DOUBLE DEFAULT 0,
  `goal_amount` DOUBLE DEFAULT 0,
  `current_amount` DOUBLE DEFAULT 0,
  `status` VARCHAR(32) DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_campaigns_creator_id` (`creator_id`),
  INDEX `idx_campaigns_platform` (`platform`),
  INDEX `idx_campaigns_status` (`status`)
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
  `platform` ENUM('YOUTUBE', 'INSTAGRAM', 'FACEBOOK', 'TWITTER') NOT NULL DEFAULT 'YOUTUBE',
  `task_type` ENUM('FOLLOW', 'VIEW_LONG', 'VIEW_SHORT', 'LIKE', 'COMMENT') NOT NULL,
  `target_url` VARCHAR(500),
  `status` VARCHAR(32) DEFAULT 'ASSIGNED',
  `proof` TEXT,
  `proof_url` VARCHAR(500),
  `proof_public_id` VARCHAR(255),
  `proof_text` TEXT,
  `proof_hash` VARCHAR(64),
  `risk_score` DOUBLE DEFAULT 0.0,
  `auto_flag` BOOLEAN DEFAULT FALSE,
  `approved_at` TIMESTAMP NULL,
  `hold_start_time` TIMESTAMP NULL,
  `hold_end_time` TIMESTAMP NULL,
  `paid_at` TIMESTAMP NULL,
  `payment_txn_id` VARCHAR(100),
  `device_fingerprint` VARCHAR(255),
  `ip_address` VARCHAR(100),
  `assigned_at` TIMESTAMP NULL,
  `submitted_at` TIMESTAMP NULL,
  `completed_at` TIMESTAMP NULL,
  `approved_by` BIGINT NULL,
  `rejection_reason` TEXT NULL,
  INDEX `idx_viewer_tasks_viewer_id` (`viewer_id`),
  INDEX `idx_viewer_tasks_status` (`status`),
  INDEX `idx_viewer_tasks_viewer_status` (`viewer_id`, `status`),
  INDEX `idx_viewer_tasks_task_id` (`task_id`),
  INDEX `idx_viewer_tasks_platform` (`platform`),
  INDEX `idx_viewer_tasks_task_type` (`task_type`),
  INDEX `idx_viewer_tasks_proof_hash` (`proof_hash`),
  INDEX `idx_viewer_tasks_hold_end` (`hold_end_time`),
  INDEX `idx_viewer_tasks_paid_at` (`paid_at`),
  INDEX `idx_viewer_tasks_risk` (`risk_score`, `auto_flag`),
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

-- Insert test viewer users with default passwords
INSERT IGNORE INTO users (email, password, role, status, is_verified, suspension_until)
VALUES ('viewer1@example.com', 'defaultPassword123', 'VIEWER', 'ACTIVE', true, NULL),
       ('viewer2@example.com', 'defaultPassword123', 'VIEWER', 'ACTIVE', false, NULL);

-- ===============================
-- PAYMENT SYSTEM TABLES
-- ===============================

-- Viewer Wallet: Tracks viewer earnings and balances
CREATE TABLE IF NOT EXISTS `viewer_wallet` (
  `user_id` BIGINT PRIMARY KEY,
  `balance` DECIMAL(10, 2) DEFAULT 0.00,
  `locked_balance` DECIMAL(10, 2) DEFAULT 0.00,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_viewer_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Creator Wallet: Tracks creator campaign funds
CREATE TABLE IF NOT EXISTS `creator_wallet` (
  `creator_id` BIGINT PRIMARY KEY,
  `balance` DECIMAL(10, 2) DEFAULT 0.00,
  `locked_balance` DECIMAL(10, 2) DEFAULT 0.00,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_creator_wallet_user` FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Withdrawal Requests: Viewer withdrawal requests requiring admin approval
CREATE TABLE IF NOT EXISTS `withdrawal_requests` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `upi_id` VARCHAR(100) NOT NULL,
  `status` ENUM('PENDING', 'PROCESSING', 'APPROVED', 'PAID', 'FAILED', 'REJECTED') DEFAULT 'PENDING',
  `requested_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `processed_at` TIMESTAMP NULL,
  `processed_by` BIGINT NULL,
  `rejection_reason` TEXT NULL,
  INDEX `idx_withdrawal_requests_user_id` (`user_id`),
  INDEX `idx_withdrawal_requests_status` (`status`),
  CONSTRAINT `fk_withdrawal_requests_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_withdrawal_requests_processor` FOREIGN KEY (`processed_by`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- If the table already exists with older enum values, run this alter:
-- ALTER TABLE `withdrawal_requests`
--   MODIFY COLUMN `status` ENUM('PENDING', 'PROCESSING', 'APPROVED', 'PAID', 'FAILED', 'REJECTED') DEFAULT 'PENDING';

-- Creator Top-ups: Creator fund additions requiring admin approval
CREATE TABLE IF NOT EXISTS `creator_topups` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `creator_id` BIGINT NOT NULL,
  `amount` DECIMAL(10, 2) NOT NULL,
  `upi_reference` VARCHAR(255) NOT NULL,
  `proof_url` VARCHAR(500),
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `approved_at` TIMESTAMP NULL,
  `approved_by` BIGINT NULL,
  `rejection_reason` TEXT NULL,
  INDEX `idx_creator_topups_creator_id` (`creator_id`),
  INDEX `idx_creator_topups_status` (`status`),
  CONSTRAINT `fk_creator_topups_creator` FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_creator_topups_approver` FOREIGN KEY (`approved_by`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===============================
-- RAZORPAY TRANSACTIONS
-- ===============================

CREATE TABLE IF NOT EXISTS `payment_transactions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `user_role` VARCHAR(30),
  `amount` DECIMAL(12, 2) NOT NULL,
  `currency` VARCHAR(10) DEFAULT 'INR',
  `razorpay_order_id` VARCHAR(100) UNIQUE,
  `razorpay_payment_id` VARCHAR(100),
  `status` VARCHAR(30),
  `signature` VARCHAR(200),
  `credited` BOOLEAN DEFAULT FALSE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `verified_at` TIMESTAMP NULL,
  `credited_at` TIMESTAMP NULL,
  INDEX `idx_payment_transactions_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payout_transactions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `withdrawal_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(12, 2) NOT NULL,
  `currency` VARCHAR(10) DEFAULT 'INR',
  `mode` VARCHAR(20) DEFAULT 'UPI',
  `purpose` VARCHAR(50) DEFAULT 'payout',
  `fund_account` VARCHAR(150),
  `razorpay_payout_id` VARCHAR(100) UNIQUE,
  `status` VARCHAR(30),
  `failure_reason` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `processed_at` TIMESTAMP NULL,
  INDEX `idx_payout_transactions_withdrawal_id` (`withdrawal_id`),
  INDEX `idx_payout_transactions_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
