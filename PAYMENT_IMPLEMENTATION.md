# Payment System Implementation - Grow-Earn Backend

## ✅ IMPLEMENTATION COMPLETE

Safe, admin-approved payment flow for **VIEWERS** and **CREATORS** has been successfully implemented.

---

## 🔒 SECURITY PRINCIPLES ENFORCED

- ✅ **NO instant payments** - ALL transactions require admin approval
- ✅ **NO automated UPI** - Manual verification only
- ✅ **Admin-gated** - Money movement requires human approval
- ✅ **Transaction-safe** - Uses pessimistic locking
- ✅ **Strike-protected** - Users with violations cannot transact
- ✅ **Audit-logged** - All actions tracked with timestamps and admin IDs

---

## 📊 DATABASE SCHEMA ADDITIONS

### 1. `viewer_wallet`
- Tracks viewer earnings and withdrawable balance
- **Fields:**
  - `user_id` (PK, FK to users)
  - `balance` - Available for withdrawal
  - `locked_balance` - Funds in hold period
  - `updated_at`

### 2. `creator_wallet`
- Tracks creator campaign funds
- **Fields:**
  - `creator_id` (PK, FK to users)
  - `balance` - Available for campaigns
  - `locked_balance` - Funds locked in active campaigns
  - `updated_at`

### 3. `withdrawal_requests`
- Viewer withdrawal requests requiring admin approval
- **Fields:**
  - `id` (PK)
  - `user_id` (FK to users)
  - `amount`
  - `upi_id`
  - `status` (PENDING, APPROVED, PAID, REJECTED)
  - `requested_at`
  - `processed_at`
  - `processed_by` (admin ID)
  - `rejection_reason`

### 4. `creator_topups`
- Creator fund additions requiring admin approval
- **Fields:**
  - `id` (PK)
  - `creator_id` (FK to users)
  - `amount`
  - `upi_reference`
  - `proof_url` (Cloudinary image)
  - `status` (PENDING, APPROVED, REJECTED)
  - `created_at`
  - `approved_at`
  - `approved_by` (admin ID)
  - `rejection_reason`

---

## 💰 VIEWER PAYMENT FLOW

### Step 1: Earn from Tasks
After task verification and hold period:
```
viewer_tasks.hold_end_time expires
  ↓
Add to viewer_wallet.locked_balance
  ↓
Release to viewer_wallet.balance
```

### Step 2: Request Withdrawal
**API:** `POST /api/viewer/wallet/withdraw`

**Request:**
```json
{
  "amount": 100.00,
  "upiId": "viewer@upi"
}
```

**Validations:**
- ✅ Minimum withdrawal: ₹10
- ✅ Sufficient balance
- ✅ No active violations
- ✅ No pending withdrawal requests

**Result:** Creates `withdrawal_requests` with status = PENDING

### Step 3: Admin Approval
**Admin APIs:**
- `GET /api/admin/payments/withdrawals` - List all
- `GET /api/admin/payments/withdrawals/pending` - Pending only
- `POST /api/admin/payments/withdrawals/{id}/approve` - Approve
- `POST /api/admin/payments/withdrawals/{id}/reject` - Reject

**On APPROVE:**
```
1. Deduct from viewer_wallet.balance
2. Set status = PAID
3. Record processed_by (admin ID)
4. Set processed_at timestamp
```

**On REJECT:**
```
1. Set status = REJECTED
2. Record rejection_reason
3. Balance remains unchanged
```

---

## 🎨 CREATOR PAYMENT FLOW

### Step 1: Submit Top-up Request
**API:** `POST /api/creator/wallet/topup`

**Request:**
```json
{
  "amount": 500.00,
  "upiReference": "UTR123456789",
  "proofUrl": "https://cloudinary.com/proof.jpg"
}
```

**Validations:**
- ✅ User is a CREATOR
- ✅ Minimum top-up: ₹10
- ✅ No active violations
- ✅ Valid UPI reference

**Result:** Creates `creator_topups` with status = PENDING

### Step 2: Admin Approval
**Admin APIs:**
- `GET /api/admin/payments/topups` - List all
- `GET /api/admin/payments/topups/pending` - Pending only
- `POST /api/admin/payments/topups/{id}/approve` - Approve
- `POST /api/admin/payments/topups/{id}/reject` - Reject

**On APPROVE:**
```
1. Credit creator_wallet.balance
2. Set status = APPROVED
3. Record approved_by (admin ID)
4. Set approved_at timestamp
```

**On REJECT:**
```
1. Set status = REJECTED
2. Record rejection_reason
3. No wallet change
```

### Step 3: Campaign Usage
When creator creates a campaign:
```
1. Deduct from creator_wallet.balance
2. Move to creator_wallet.locked_balance
```

When tasks are paid:
```
1. Deduct from creator_wallet.locked_balance (permanent)
2. Credit viewer_wallet.locked_balance
```

When campaign ends with unused budget:
```
1. Release from locked_balance → balance
```

---

## 🔧 IMPLEMENTED COMPONENTS

### Entities (6 new files)
- ✅ `ViewerWallet.java` - Viewer wallet entity
- ✅ `CreatorWallet.java` - Creator wallet entity
- ✅ `WithdrawalRequest.java` - Withdrawal request entity
- ✅ `CreatorTopup.java` - Top-up request entity
- ✅ `WithdrawalStatus.java` - Enum (PENDING, APPROVED, PAID, REJECTED)
- ✅ `TopupStatus.java` - Enum (PENDING, APPROVED, REJECTED)

### Repositories (4 new files)
- ✅ `ViewerWalletRepository.java` - With pessimistic locking
- ✅ `CreatorWalletRepository.java` - With pessimistic locking
- ✅ `WithdrawalRequestRepository.java` - Query methods
- ✅ `CreatorTopupRepository.java` - Query methods

### Services (4 new files)
- ✅ `ViewerWalletService.java` - Wallet operations
- ✅ `CreatorWalletService.java` - Wallet operations
- ✅ `WithdrawalService.java` - Withdrawal flow
- ✅ `CreatorTopupService.java` - Top-up flow
- ✅ **Modified:** `UserViolationService.java` - Added `hasActiveViolations()` method

### DTOs (6 new files)
- ✅ `WithdrawalRequestDTO.java` - Create withdrawal
- ✅ `WithdrawalResponseDTO.java` - Withdrawal response
- ✅ `CreatorTopupRequestDTO.java` - Create top-up
- ✅ `CreatorTopupResponseDTO.java` - Top-up response
- ✅ `WalletDTO.java` - Wallet balance response
- ✅ `ApprovalRequestDTO.java` - Admin approval/rejection

### Controllers (3 new files)
- ✅ `ViewerWithdrawalController.java` - Viewer wallet & withdrawals
- ✅ `CreatorTopupController.java` - Creator wallet & top-ups
- ✅ `AdminPaymentController.java` - Admin approval endpoints

---

## 🌐 API ENDPOINTS SUMMARY

### Viewer APIs (Authentication Required - VIEWER role)
```
GET  /api/viewer/wallet/balance         - Get wallet balance
POST /api/viewer/wallet/withdraw        - Request withdrawal
GET  /api/viewer/wallet/withdrawals     - Withdrawal history
```

### Creator APIs (Authentication Required - CREATOR role)
```
GET  /api/creator/wallet/balance        - Get wallet balance
POST /api/creator/wallet/topup          - Submit top-up request
GET  /api/creator/wallet/topups         - Top-up history
```

### Admin APIs (Authentication Required - ADMIN role)
```
GET  /api/admin/payments/withdrawals              - All withdrawals
GET  /api/admin/payments/withdrawals/pending      - Pending withdrawals
POST /api/admin/payments/withdrawals/{id}/approve - Approve withdrawal
POST /api/admin/payments/withdrawals/{id}/reject  - Reject withdrawal

GET  /api/admin/payments/topups                   - All top-ups
GET  /api/admin/payments/topups/pending           - Pending top-ups
POST /api/admin/payments/topups/{id}/approve      - Approve top-up
POST /api/admin/payments/topups/{id}/reject       - Reject top-up
```

---

## 🔐 TRANSACTION SAFETY

### Pessimistic Locking
Both wallet repositories use `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent:
- Race conditions
- Double withdrawals
- Balance manipulation

### Atomic Operations
All wallet modifications are `@Transactional` ensuring:
- All-or-nothing operations
- Rollback on failure
- Consistent state

### Violation Checks
Before any financial transaction:
```java
if (userViolationService.hasActiveViolations(userId)) {
    throw new RuntimeException("Cannot transact: Account has violations");
}
```

---

## 📝 USAGE EXAMPLES

### Viewer Withdraws ₹100

**1. Viewer requests:**
```bash
POST /api/viewer/wallet/withdraw
Authorization: Bearer <viewer_token>

{
  "amount": 100.00,
  "upiId": "viewer@paytm"
}
```

**2. Admin approves:**
```bash
POST /api/admin/payments/withdrawals/1/approve
Authorization: Bearer <admin_token>
```

**3. Result:**
- ₹100 deducted from viewer_wallet.balance
- Status changed to PAID
- Admin ID and timestamp recorded

---

### Creator Adds ₹500

**1. Creator submits:**
```bash
POST /api/creator/wallet/topup
Authorization: Bearer <creator_token>

{
  "amount": 500.00,
  "upiReference": "UTR987654321",
  "proofUrl": "https://cloudinary.com/payment-proof.jpg"
}
```

**2. Admin approves:**
```bash
POST /api/admin/payments/topups/1/approve
Authorization: Bearer <admin_token>
```

**3. Result:**
- ₹500 credited to creator_wallet.balance
- Status changed to APPROVED
- Creator can now fund campaigns

---

## ✅ SECURITY CHECKLIST

- ✅ All money movements require admin approval
- ✅ No instant payments or automated transfers
- ✅ Users with violations blocked from transactions
- ✅ Minimum withdrawal/top-up amounts enforced
- ✅ Pessimistic locking prevents race conditions
- ✅ All actions logged with admin IDs and timestamps
- ✅ Balance validations before deductions
- ✅ Rejection reasons stored for audit
- ✅ One pending request per user at a time
- ✅ Transaction-safe with rollback support

---

## 🚀 NEXT STEPS (OPTIONAL ENHANCEMENTS)

1. **Email Notifications**
   - Notify viewers when withdrawal approved/rejected
   - Notify creators when top-up approved/rejected
   - Notify admins when new requests arrive

2. **Withdrawal Limits**
   - Daily/weekly withdrawal limits per user
   - Maximum single withdrawal amount

3. **UPI Validation**
   - Validate UPI ID format
   - Store verified UPI IDs per user

4. **Dashboard Widgets**
   - Admin dashboard showing pending requests count
   - Financial transaction reports
   - Month-wise payout summaries

5. **Batch Approvals**
   - Select multiple withdrawals and approve at once
   - Bulk rejection with common reason

---

## 📦 FILES CREATED/MODIFIED

### Created (23 files):
```
src/main/java/com/growearn/entity/
  - ViewerWallet.java
  - CreatorWallet.java
  - WithdrawalRequest.java
  - CreatorTopup.java
  - WithdrawalStatus.java
  - TopupStatus.java

src/main/java/com/growearn/repository/
  - ViewerWalletRepository.java
  - CreatorWalletRepository.java
  - WithdrawalRequestRepository.java
  - CreatorTopupRepository.java

src/main/java/com/growearn/service/
  - ViewerWalletService.java
  - CreatorWalletService.java
  - WithdrawalService.java
  - CreatorTopupService.java

src/main/java/com/growearn/dto/
  - WithdrawalRequestDTO.java
  - WithdrawalResponseDTO.java
  - CreatorTopupRequestDTO.java
  - CreatorTopupResponseDTO.java
  - WalletDTO.java
  - ApprovalRequestDTO.java

src/main/java/com/growearn/controller/
  - ViewerWithdrawalController.java
  - CreatorTopupController.java
  - AdminPaymentController.java
```

### Modified (2 files):
```
src/main/resources/
  - schema.sql (added 4 new tables)

src/main/java/com/growearn/service/
  - UserViolationService.java (added hasActiveViolations method)
```

---

## 🎯 IMPLEMENTATION STATUS: ✅ COMPLETE

All requirements met:
- ✅ Safe, admin-approved payment flow
- ✅ NO instant payments
- ✅ NO automated UPI
- ✅ Separate flows for viewers and creators
- ✅ Database schema extended
- ✅ Transaction-safe operations
- ✅ Complete API endpoints
- ✅ Violation checks integrated
- ✅ Audit logging implemented

**Total Lines of Code:** ~2,500 lines
**Compilation Status:** ✅ No errors
**Test Strategy:** Manual testing via Postman/curl
