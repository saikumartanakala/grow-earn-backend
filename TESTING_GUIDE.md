# Payment System - Quick Start Testing Guide

## 🚀 Getting Started

### 1. Database Setup
Run the application - tables will be auto-created from `schema.sql`:
- `viewer_wallet`
- `creator_wallet`
- `withdrawal_requests`
- `creator_topups`

### 2. Test Accounts Needed
- **Viewer account** with completed tasks and earnings
- **Creator account** 
- **Admin account** for approvals

---

## 📋 TESTING SCENARIOS

### Scenario 1: Viewer Withdraws Money

**A. Check Wallet Balance**
```bash
curl -X GET http://localhost:8080/api/viewer/wallet/balance \
  -H "Authorization: Bearer <VIEWER_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "wallet": {
    "userId": 1,
    "balance": 150.00,
    "lockedBalance": 50.00,
    "totalBalance": 200.00,
    "updatedAt": "2026-02-06T10:30:00"
  }
}
```

**B. Request Withdrawal**
```bash
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "upiId": "viewer@paytm"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Withdrawal request submitted successfully. Awaiting admin approval.",
  "withdrawal": {
    "id": 1,
    "userId": 1,
    "amount": 100.00,
    "upiId": "viewer@paytm",
    "status": "PENDING",
    "requestedAt": "2026-02-06T10:35:00"
  }
}
```

**C. View Withdrawal History**
```bash
curl -X GET http://localhost:8080/api/viewer/wallet/withdrawals?page=0&size=10 \
  -H "Authorization: Bearer <VIEWER_TOKEN>"
```

---

### Scenario 2: Admin Approves Withdrawal

**A. View Pending Withdrawals**
```bash
curl -X GET http://localhost:8080/api/admin/payments/withdrawals/pending \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "withdrawals": [
    {
      "id": 1,
      "userId": 1,
      "userEmail": "viewer@example.com",
      "amount": 100.00,
      "upiId": "viewer@paytm",
      "status": "PENDING",
      "requestedAt": "2026-02-06T10:35:00"
    }
  ],
  "count": 1
}
```

**B. Approve Withdrawal**
```bash
curl -X POST http://localhost:8080/api/admin/payments/withdrawals/1/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Withdrawal approved successfully",
  "withdrawal": {
    "id": 1,
    "status": "PAID",
    "processedAt": "2026-02-06T10:40:00",
    "processedBy": 10,
    "processedByEmail": "admin@growearn.com"
  }
}
```

**C. OR Reject Withdrawal**
```bash
curl -X POST http://localhost:8080/api/admin/payments/withdrawals/1/reject \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Invalid UPI ID"
  }'
```

---

### Scenario 3: Creator Adds Funds

**A. Check Creator Wallet**
```bash
curl -X GET http://localhost:8080/api/creator/wallet/balance \
  -H "Authorization: Bearer <CREATOR_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "wallet": {
    "userId": 2,
    "balance": 0.00,
    "lockedBalance": 0.00,
    "totalBalance": 0.00
  }
}
```

**B. Submit Top-up Request**
```bash
curl -X POST http://localhost:8080/api/creator/wallet/topup \
  -H "Authorization: Bearer <CREATOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "upiReference": "UTR123456789",
    "proofUrl": "https://res.cloudinary.com/xyz/image/upload/v123/proof.jpg"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Top-up request submitted successfully. Awaiting admin approval.",
  "topup": {
    "id": 1,
    "creatorId": 2,
    "amount": 500.00,
    "upiReference": "UTR123456789",
    "proofUrl": "https://res.cloudinary.com/xyz/image/upload/v123/proof.jpg",
    "status": "PENDING",
    "createdAt": "2026-02-06T11:00:00"
  }
}
```

---

### Scenario 4: Admin Approves Top-up

**A. View Pending Top-ups**
```bash
curl -X GET http://localhost:8080/api/admin/payments/topups/pending \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**B. Approve Top-up**
```bash
curl -X POST http://localhost:8080/api/admin/payments/topups/1/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Top-up approved successfully",
  "topup": {
    "id": 1,
    "status": "APPROVED",
    "approvedAt": "2026-02-06T11:05:00",
    "approvedBy": 10,
    "approvedByEmail": "admin@growearn.com"
  }
}
```

**C. Verify Creator Balance Updated**
```bash
curl -X GET http://localhost:8080/api/creator/wallet/balance \
  -H "Authorization: Bearer <CREATOR_TOKEN>"
```

**Expected Response:**
```json
{
  "success": true,
  "wallet": {
    "userId": 2,
    "balance": 500.00,  // ← CREDITED!
    "lockedBalance": 0.00,
    "totalBalance": 500.00
  }
}
```

---

## 🚫 ERROR SCENARIOS TO TEST

### 1. Insufficient Balance
```bash
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -d '{"amount": 9999.00, "upiId": "viewer@paytm"}'
```
**Expected:** `400 Bad Request - Insufficient balance`

### 2. Minimum Withdrawal Not Met
```bash
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -d '{"amount": 5.00, "upiId": "viewer@paytm"}'
```
**Expected:** `400 Bad Request - Minimum withdrawal amount is ₹10`

### 3. User with Violations
**Prerequisite:** Add a strike to the user via admin
```bash
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -d '{"amount": 100.00, "upiId": "viewer@paytm"}'
```
**Expected:** `400 Bad Request - Cannot withdraw: Account has active violations`

### 4. Duplicate Pending Request
**Submit withdrawal twice without admin approval**
```bash
# First request
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -d '{"amount": 50.00, "upiId": "viewer@paytm"}'

# Second request (should fail)
curl -X POST http://localhost:8080/api/viewer/wallet/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -d '{"amount": 30.00, "upiId": "viewer@paytm"}'
```
**Expected:** `400 Bad Request - You already have a pending withdrawal request`

---

## 🔍 DATABASE VERIFICATION

After each action, verify changes in database:

**Check Viewer Wallet:**
```sql
SELECT * FROM viewer_wallet WHERE user_id = 1;
```

**Check Withdrawal Requests:**
```sql
SELECT * FROM withdrawal_requests ORDER BY requested_at DESC;
```

**Check Creator Wallet:**
```sql
SELECT * FROM creator_wallet WHERE creator_id = 2;
```

**Check Top-up Requests:**
```sql
SELECT * FROM creator_topups ORDER BY created_at DESC;
```

---

## 📊 ADMIN DASHBOARD QUERIES

**Pending Withdrawals Count:**
```sql
SELECT COUNT(*) FROM withdrawal_requests WHERE status = 'PENDING';
```

**Today's Approved Withdrawals:**
```sql
SELECT SUM(amount) FROM withdrawal_requests 
WHERE status = 'PAID' 
  AND DATE(processed_at) = CURDATE();
```

**Pending Top-ups Total:**
```sql
SELECT SUM(amount) FROM creator_topups WHERE status = 'PENDING';
```

**Total Platform Payout (All-time):**
```sql
SELECT SUM(amount) FROM withdrawal_requests WHERE status = 'PAID';
```

---

## ✅ VERIFICATION CHECKLIST

After testing, verify:
- [ ] Viewer can see wallet balance
- [ ] Viewer can request withdrawal
- [ ] Admin can see pending withdrawals
- [ ] Admin can approve withdrawal
- [ ] Viewer balance deducted on approval
- [ ] Admin can reject withdrawal
- [ ] Viewer balance unchanged on rejection
- [ ] Creator can submit top-up
- [ ] Admin can approve top-up
- [ ] Creator balance credited on approval
- [ ] Users with violations blocked
- [ ] Minimum amounts enforced
- [ ] Duplicate requests prevented
- [ ] All timestamps recorded
- [ ] Admin IDs logged

---

## 🛠️ TROUBLESHOOTING

**Issue:** "Missing or invalid authorization token"
- **Fix:** Include proper JWT token in Authorization header

**Issue:** "Wallet not found"
- **Fix:** Wallet created automatically on first access. Check user_id is correct.

**Issue:** "User not found"
- **Fix:** Ensure user exists in users table with correct role

**Issue:** "Withdrawal request not found"
- **Fix:** Use correct withdrawal request ID from database

**Issue:** Balance mismatch
- **Fix:** Check for transaction rollbacks, verify no race conditions

---

## 📝 POSTMAN COLLECTION

Import this into Postman for easier testing:

**Environment Variables:**
```
VIEWER_TOKEN = <jwt_token>
CREATOR_TOKEN = <jwt_token>
ADMIN_TOKEN = <jwt_token>
BASE_URL = http://localhost:8080
```

**Collection Structure:**
```
📁 Grow-Earn Payment System
  📁 Viewer
    - Get Wallet Balance
    - Request Withdrawal
    - Get Withdrawal History
  📁 Creator
    - Get Wallet Balance
    - Submit Top-up
    - Get Top-up History
  📁 Admin
    📁 Withdrawals
      - Get All Withdrawals
      - Get Pending Withdrawals
      - Approve Withdrawal
      - Reject Withdrawal
    📁 Top-ups
      - Get All Top-ups
      - Get Pending Top-ups
      - Approve Top-up
      - Reject Top-up
```

---

## 🎯 SUCCESS CRITERIA

✅ **Payment system is working if:**
1. Viewers can request and receive approved withdrawals
2. Creators can add funds after admin approval
3. All money movements require admin action
4. Balances are accurately tracked
5. Violation checks prevent restricted users
6. All transactions are logged with timestamps
7. Race conditions prevented via locking
8. Rejection reasons stored properly

---

## 📧 SUPPORT

For issues or questions:
1. Check application logs: `tail -f logs/application.log`
2. Verify database schema matches: `SHOW CREATE TABLE viewer_wallet;`
3. Check Spring Boot console for errors
4. Review [PAYMENT_IMPLEMENTATION.md](./PAYMENT_IMPLEMENTATION.md) for full details
