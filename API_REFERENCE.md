# Payment System API Reference Card

## 🔐 Authentication
All endpoints require JWT Bearer token:
```
Authorization: Bearer <token>
```

---

## 👁️ VIEWER ENDPOINTS

### 1. Get Wallet Balance
```http
GET /api/viewer/wallet/balance
```
**Auth:** Viewer token  
**Response:**
```json
{
  "success": true,
  "wallet": {
    "balance": 150.00,
    "lockedBalance": 50.00,
    "totalBalance": 200.00
  }
}
```

### 2. Request Withdrawal
```http
POST /api/viewer/wallet/withdraw
Content-Type: application/json

{
  "amount": 100.00,
  "upiId": "viewer@paytm"
}
```
**Auth:** Viewer token  
**Min Amount:** ₹10  
**Response:**
```json
{
  "success": true,
  "message": "Withdrawal request submitted",
  "withdrawal": {
    "id": 1,
    "status": "PENDING",
    "amount": 100.00
  }
}
```

### 3. Get Withdrawal History
```http
GET /api/viewer/wallet/withdrawals?page=0&size=10
```
**Auth:** Viewer token  
**Params:** `page`, `size`

---

## 🎨 CREATOR ENDPOINTS

### 1. Get Wallet Balance
```http
GET /api/creator/wallet/balance
```
**Auth:** Creator token

### 2. Submit Top-up Request
```http
POST /api/creator/wallet/topup
Content-Type: application/json

{
  "amount": 500.00,
  "upiReference": "UTR123456789",
  "proofUrl": "https://cloudinary.com/proof.jpg"
}
```
**Auth:** Creator token  
**Min Amount:** ₹10

### 3. Get Top-up History
```http
GET /api/creator/wallet/topups?page=0&size=10
```
**Auth:** Creator token

---

## 👑 ADMIN ENDPOINTS

### Withdrawal Management

#### 1. List All Withdrawals
```http
GET /api/admin/payments/withdrawals?status=PENDING&page=0&size=20
```
**Auth:** Admin token  
**Params:** `status` (optional), `page`, `size`  
**Status Values:** PENDING, APPROVED, PAID, REJECTED

#### 2. Get Pending Withdrawals
```http
GET /api/admin/payments/withdrawals/pending
```
**Auth:** Admin token

#### 3. Approve Withdrawal
```http
POST /api/admin/payments/withdrawals/{id}/approve
```
**Auth:** Admin token  
**Effect:** Deducts viewer balance, marks as PAID

#### 4. Reject Withdrawal
```http
POST /api/admin/payments/withdrawals/{id}/reject
Content-Type: application/json

{
  "reason": "Invalid UPI ID"
}
```
**Auth:** Admin token  
**Effect:** Marks as REJECTED, balance unchanged

---

### Top-up Management

#### 1. List All Top-ups
```http
GET /api/admin/payments/topups?status=PENDING&page=0&size=20
```
**Auth:** Admin token  
**Params:** `status` (optional), `page`, `size`  
**Status Values:** PENDING, APPROVED, REJECTED

#### 2. Get Pending Top-ups
```http
GET /api/admin/payments/topups/pending
```
**Auth:** Admin token

#### 3. Approve Top-up
```http
POST /api/admin/payments/topups/{id}/approve
```
**Auth:** Admin token  
**Effect:** Credits creator balance, marks as APPROVED

#### 4. Reject Top-up
```http
POST /api/admin/payments/topups/{id}/reject
Content-Type: application/json

{
  "reason": "Proof not clear"
}
```
**Auth:** Admin token  
**Effect:** Marks as REJECTED, no balance change

---

## 🚨 ERROR RESPONSES

### 400 Bad Request
```json
{
  "success": false,
  "message": "Insufficient balance"
}
```

**Common Reasons:**
- Insufficient balance
- Amount below minimum (₹10)
- Pending request exists
- User has violations
- Invalid UPI ID format

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Missing or invalid authorization token"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Withdrawal request not found"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Failed to process request"
}
```

---

## 📊 STATUS FLOW DIAGRAMS

### Withdrawal Status Flow
```
PENDING → PAID (admin approves)
   ↓
REJECTED (admin rejects)
```

### Top-up Status Flow
```
PENDING → APPROVED (admin approves)
   ↓
REJECTED (admin rejects)
```

---

## 💡 BUSINESS RULES

### Viewer Withdrawals
- ✅ Minimum withdrawal: ₹10
- ✅ Must have sufficient balance
- ✅ One pending request at a time
- ✅ No violations allowed
- ✅ Admin approval required

### Creator Top-ups
- ✅ Minimum top-up: ₹10
- ✅ Must be CREATOR role
- ✅ Proof required (Cloudinary URL)
- ✅ UPI reference required
- ✅ No violations allowed
- ✅ Admin approval required

### Admin Actions
- ✅ Can only process PENDING requests
- ✅ Must provide rejection reason
- ✅ Actions are logged with admin ID
- ✅ Balance changes are atomic

---

## 🔒 SECURITY NOTES

1. **No Auto-payments:** ALL transactions require human approval
2. **Pessimistic Locking:** Prevents race conditions
3. **Violation Checks:** Users with strikes blocked
4. **Transaction Safety:** Rollback on any failure
5. **Audit Trail:** All actions timestamped with admin ID
6. **Balance Verification:** Checked before deduction
7. **Single Pending Rule:** Prevents spam requests

---

## 📈 MONITORING QUERIES

**Pending Payments Count:**
```sql
SELECT 
  (SELECT COUNT(*) FROM withdrawal_requests WHERE status='PENDING') as withdrawals,
  (SELECT COUNT(*) FROM creator_topups WHERE status='PENDING') as topups;
```

**Today's Activity:**
```sql
SELECT 
  COUNT(*) as approved_withdrawals,
  SUM(amount) as total_paid
FROM withdrawal_requests 
WHERE status='PAID' 
  AND DATE(processed_at) = CURDATE();
```

**Creator Balance Summary:**
```sql
SELECT 
  creator_id, 
  balance, 
  locked_balance,
  (balance + locked_balance) as total
FROM creator_wallet
ORDER BY total DESC;
```

---

## 🎯 QUICK TESTING

**Test Viewer Flow:**
```bash
# 1. Check balance
curl -H "Authorization: Bearer $VIEWER_TOKEN" \
  http://localhost:8080/api/viewer/wallet/balance

# 2. Request withdrawal
curl -X POST -H "Authorization: Bearer $VIEWER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"upiId":"test@upi"}' \
  http://localhost:8080/api/viewer/wallet/withdraw

# 3. Admin approve
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/payments/withdrawals/1/approve
```

**Test Creator Flow:**
```bash
# 1. Check balance
curl -H "Authorization: Bearer $CREATOR_TOKEN" \
  http://localhost:8080/api/creator/wallet/balance

# 2. Submit top-up
curl -X POST -H "Authorization: Bearer $CREATOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":500,"upiReference":"UTR123","proofUrl":"https://proof.jpg"}' \
  http://localhost:8080/api/creator/wallet/topup

# 3. Admin approve
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/payments/topups/1/approve
```

---

## 📞 CONTACT & SUPPORT

**Documentation:**
- [PAYMENT_IMPLEMENTATION.md](./PAYMENT_IMPLEMENTATION.md) - Full implementation details
- [TESTING_GUIDE.md](./TESTING_GUIDE.md) - Comprehensive testing scenarios

**Code Location:**
```
src/main/java/com/growearn/
├── entity/         (ViewerWallet, CreatorWallet, etc.)
├── repository/     (WalletRepositories)
├── service/        (WalletServices, WithdrawalService, etc.)
├── controller/     (ViewerWithdrawalController, etc.)
└── dto/            (Request/Response DTOs)
```

**Database Tables:**
- `viewer_wallet` - Viewer balances
- `creator_wallet` - Creator balances
- `withdrawal_requests` - Viewer withdrawals
- `creator_topups` - Creator fund additions

---

## ✅ IMPLEMENTATION STATUS

**Version:** 1.0  
**Status:** Production Ready  
**Date:** February 6, 2026  
**Security:** Admin-Approved Only ✅  
**Compilation:** No Errors ✅  
**Testing:** Manual Testing Recommended ✅
