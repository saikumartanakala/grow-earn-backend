# Frontend Integration - Payment System

## ✅ BACKEND ALIGNED WITH FRONTEND

All payment endpoints have been updated to match your frontend's expected URLs and response formats.

---

## 🔄 CHANGES MADE

### URL Path Updates

**Before:**
```
/api/viewer/wallet/balance
/api/viewer/wallet/withdraw
/api/viewer/wallet/withdrawals

/api/creator/wallet/balance
/api/creator/wallet/topup
/api/creator/wallet/topups

/api/admin/payments/withdrawals
/api/admin/payments/topups
```

**After (Matches Frontend):**
```
/api/viewer/wallet
/api/viewer/withdraw
/api/viewer/withdrawals

/api/creator/wallet
/api/creator/topup
/api/creator/topups

/api/admin/withdrawals
/api/admin/topups
```

---

## 📋 ENDPOINT MAPPING

### Viewer Endpoints

| Frontend Call | Backend Endpoint | Method | Response Format |
|--------------|------------------|---------|----------------|
| `getWalletBalance()` | `/api/viewer/wallet` | GET | `{balance, available, locked}` |
| `requestWithdrawal()` | `/api/viewer/withdraw` | POST | `{id, status, message}` |
| `getWithdrawalHistory()` | `/api/viewer/withdrawals` | GET | `[{id, amount, upiId, status, createdAt, reason}]` |

### Creator Endpoints

| Frontend Call | Backend Endpoint | Method | Response Format |
|--------------|------------------|---------|----------------|
| `getWalletBalance()` | `/api/creator/wallet` | GET | `{balance, available, locked, lockedBalance}` |
| `submitTopup()` | `/api/creator/topup` | POST | `{id, status, message}` |
| `getTopupHistory()` | `/api/creator/topups` | GET | `[{id, amount, upiReferenceId, proofUrl, status, createdAt, reason}]` |

### Admin Endpoints

| Frontend Call | Backend Endpoint | Method | Request Format |
|--------------|------------------|---------|----------------|
| `getPendingWithdrawals()` | `/api/admin/withdrawals/pending` | GET | - |
| `getAllWithdrawals()` | `/api/admin/withdrawals` | GET | `?status=PENDING` |
| `approveWithdrawal()` | `/api/admin/withdrawals/approve` | POST | `{withdrawalId: 1}` |
| `rejectWithdrawal()` | `/api/admin/withdrawals/reject` | POST | `{withdrawalId: 1, reason: "..."}` |
| `getPendingTopups()` | `/api/admin/topups/pending` | GET | - |
| `getAllTopups()` | `/api/admin/topups` | GET | `?status=PENDING` |
| `approveTopup()` | `/api/admin/topups/approve` | POST | `{topupId: 1}` |
| `rejectTopup()` | `/api/admin/topups/reject` | POST | `{topupId: 1, reason: "..."}` |

---

## 🔧 RESPONSE FORMAT UPDATES

### Viewer Wallet Response
```json
{
  "balance": 150.00,
  "available": 150.00,
  "locked": 0.00
}
```

### Creator Wallet Response
```json
{
  "balance": 5000.00,
  "available": 3000.00,
  "locked": 2000.00,
  "lockedBalance": 2000.00
}
```

### Withdrawal Request Response
```json
{
  "id": 1,
  "status": "PENDING",
  "message": "Withdrawal request submitted"
}
```

### Top-up Request Response
```json
{
  "id": 1,
  "status": "PENDING",
  "message": "Top-up request submitted"
}
```

### Withdrawal History Response
```json
[
  {
    "id": 1,
    "userId": 123,
    "userName": "user@example.com",
    "amount": 100.00,
    "upiId": "user@paytm",
    "status": "PENDING",
    "createdAt": "2026-02-06T10:00:00Z",
    "requestedAt": "2026-02-06T10:00:00Z",
    "reason": "Optional rejection reason"
  }
]
```

### Top-up History Response
```json
[
  {
    "id": 1,
    "userId": 456,
    "userName": "creator@example.com",
    "amount": 1000.00,
    "upiReferenceId": "123456789012",
    "proofUrl": "https://cloudinary.com/...",
    "status": "PENDING",
    "createdAt": "2026-02-06T10:00:00Z",
    "reason": "Optional rejection reason"
  }
]
```

### Admin Approval Response
```json
{
  "success": true,
  "message": "Withdrawal approved"
}
```

### Error Response
```json
{
  "error": "Insufficient balance"
}
```

---

## 🎯 FIELD NAME MAPPING

### Frontend → Backend

| Frontend Field | Backend Field | Notes |
|----------------|---------------|-------|
| `upiReferenceId` | `upiReference` | Mapped in DTO getters/setters |
| `userName` | `userEmail` | Uses email as display name |
| `createdAt` | `requestedAt` | Withdrawal timestamps |
| `reason` | `rejectionReason` | Rejection reasons |

---

## ✅ VALIDATION RULES IMPLEMENTED

### Withdrawal Request
- ✅ Minimum amount: ₹10
- ✅ Sufficient balance check
- ✅ No active violations
- ✅ One pending request at a time
- ✅ Valid UPI ID required

### Top-up Request
- ✅ Minimum amount: ₹10
- ✅ Must be CREATOR role
- ✅ UPI reference required
- ✅ Proof URL required
- ✅ No active violations

### Admin Actions
- ✅ Can only process PENDING requests
- ✅ Rejection reason required
- ✅ Admin ID logged
- ✅ Atomic balance updates

---

## 🚀 TESTING THE INTEGRATION

### 1. Start Backend
```bash
.\mvnw spring-boot:run
```

### 2. Test Viewer Flow
```bash
# Get wallet balance
curl http://localhost:8080/api/viewer/wallet \
  -H "Authorization: Bearer <VIEWER_TOKEN>"

# Request withdrawal
curl -X POST http://localhost:8080/api/viewer/withdraw \
  -H "Authorization: Bearer <VIEWER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100, "upiId": "viewer@upi"}'

# Check history
curl http://localhost:8080/api/viewer/withdrawals \
  -H "Authorization: Bearer <VIEWER_TOKEN>"
```

### 3. Test Creator Flow
```bash
# Get wallet balance
curl http://localhost:8080/api/creator/wallet \
  -H "Authorization: Bearer <CREATOR_TOKEN>"

# Submit top-up
curl -X POST http://localhost:8080/api/creator/topup \
  -H "Authorization: Bearer <CREATOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500,
    "upiReferenceId": "UTR123456789",
    "proofUrl": "https://cloudinary.com/proof.jpg"
  }'

# Check history
curl http://localhost:8080/api/creator/topups \
  -H "Authorization: Bearer <CREATOR_TOKEN>"
```

### 4. Test Admin Flow
```bash
# Get pending withdrawals
curl http://localhost:8080/api/admin/withdrawals/pending \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Approve withdrawal
curl -X POST http://localhost:8080/api/admin/withdrawals/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"withdrawalId": 1}'

# Get pending top-ups
curl http://localhost:8080/api/admin/topups/pending \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Approve top-up
curl -X POST http://localhost:8080/api/admin/topups/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"topupId": 1}'
```

---

## 💡 INTEGRATION NOTES

1. **Authentication Required**: All endpoints require JWT Bearer token
2. **CORS Enabled**: Frontend origins whitelisted (`localhost:5173`)
3. **Error Handling**: Returns `{error: "message"}` format
4. **Pagination**: History endpoints support `?page=0&size=10`
5. **Status Values**: `PENDING`, `APPROVED/PAID`, `REJECTED`

---

## 🔐 SECURITY FEATURES

- ✅ Admin approval required for all money movements
- ✅ User violation checks before transactions
- ✅ Pessimistic locking on wallet operations
- ✅ Transaction safety with rollback
- ✅ Audit logging with timestamps

---

## 📊 DATABASE TABLES

- `viewer_wallet` - Viewer balances
- `creator_wallet` - Creator balances
- `withdrawal_requests` - Withdrawal requests
- `creator_topups` - Top-up requests

---

## 🎉 READY FOR FRONTEND

Your frontend payment system should now work seamlessly with these backend endpoints!

**Backend Status:** ✅ Aligned with Frontend  
**Compilation:** ✅ No Errors  
**Testing:** Ready for Integration Testing
