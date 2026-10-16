# FinFlow — Complete API Reference & Architecture

> **Base URL**: `http://localhost:8080`  
> **Authentication**: All secured endpoints require `Authorization: Bearer <JWT>`  
> **Role Hierarchy**: `SUPER_ADMIN` > `MANAGER` > `FINANCE` / `EMPLOYEE`

---

## 1. Authentication APIs

### 1.1 Register User
- **Method**: `POST`
- **Path**: `/api/auth/register`
- **Public**: Yes
- **Request Body**:
```json
{
  "name": "Monesh gomo",
  "email": "dmonesh198@gmail.com",
  "password": "admin@1234",
  "contactNumber": "+91 9876543210",
  "role": "SUPER_ADMIN",
  "teamId": null
}
```
- **Response** (`200 OK`):
```json
{
    "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJkbW9uZXNoMTk4QGdtYWlsLmNvbSIsInJvbGUiOiJTVVBFUl9BRE1JTiIsImlhdCI6MTc5MjA4MzYxNCwiZXhwIjoxNzkyMTcwMDE0fQ.zPJ9a4ND0PM9EK9Bszm0IPe-Tm8MA4PAOAfzuxzaAxhfPVzC3mI9eCv5AQLv4hwh",
    "id": 1,
    "name": "Monesh gomo",
    "email": "dmonesh198@gmail.com",
    "role": "SUPER_ADMIN",
    "contactNumber": "+91 9876543210",
    "teamId": null,
    "teamName": null
}
```

### 1.2 Login
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Public**: Yes
- **Request Body**:
```json
{
  "email": "dmonesh198@gmail.com",
  "password": "admin@1234"
}
```
```json
{
  "email": "23th0026@rgcet.edu.in",
  "password": "nethra@1234"
}
```

- **Response** (`200 OK`):
```json
{
    "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJkbW9uZXNoMTlAZ21haWwuY29tIiwicm9sZSI6IlNVUEVSX0FETUlOIiwiaWF0IjoxNzkyMDc5NjM4LCJleHAiOjE3OTIxNjYwMzh9.0czxoTGXQGHfB8uDtG-oL7lfdfQHoBiBLNH_ImXEfuh5vnFuseqdsgPX7Kr9Ed6C",
    "id": 2,
    "name": "Monash Gomo",
    "email": "dmonesh19@gmail.com",
    "role": "SUPER_ADMIN",
    "contactNumber": "+91 9363636990",
    "teamId": null,
    "teamName": null
}
```

### 1.3 Current User Profile
- **Method**: `GET`
- **Path**: `/api/auth/me`
- **Header**: `Authorization: Bearer <token>`
- **Response** (`200 OK`):
```json
{
    "id": 2,
    "name": "Monash Gomo",
    "email": "dmonesh19@gmail.com",
    "contactNumber": "+91 9363636990",
    "role": "SUPER_ADMIN",
    "teamId": null,
    "teamName": null,
    "active": true,
    "createdAt": "2026-10-14T01:38:56.017768"
}
```

---

## 2. Super Admin APIs

### 2.1 Add Manager
- **Method**: `POST`
- **Path**: `/api/admin/managers`
- **Access**: `SUPER_ADMIN`
- **Request Body**:
```json
{
  "name": " Dev Manager",
  "email": "[EMAIL_ADDRESS]",
  "password": "Manager@1234",
  "contactNumber": "+91 9876543211",
  "teamId": null
}
```

### 2.2 List Managers
- **Method**: `GET`
- **Path**: `/api/admin/managers`
- **Access**: `SUPER_ADMIN`
- **Response** (`200 OK`):
```json
[
  {
    "name": "Dev Manager",
    "email": "manager@finflow.com",
    "contactNumber": "+91 9876543211",
    "role": "MANAGER",
    "teamName": "Dev Team"
  }
]
```

### 2.3 List Employees
- **Method**: `GET`
- **Path**: `/api/admin/employees`
- **Access**: `SUPER_ADMIN`
- **Response** (`200 OK`):
```json
[
  {
    "name": "Aravind Senior Dev",
    "email": "aravind@finflow.com",
    "contactNumber": "+91 9123456789",
    "role": "EMPLOYEE",
    "teamName": "Dev Team"
  }
]
```

### 2.4 Create User (Employee / Finance)
- **Method**: `POST`
- **Path**: `/api/admin/users`
- **Access**: `SUPER_ADMIN`
- **Request Body**:
```json
{
  "name": "Finance Head",
  "email": "finance@finflow.com",
  "password": "Finance@1234",
  "contactNumber": "+91 9876543212",
  "role": "FINANCE",
  "teamId": null
}
```

### 2.5 List Users
- **Method**: `GET`
- **Path**: `/api/admin/users?role=EMPLOYEE`
- **Access**: `SUPER_ADMIN`

### 2.6 Toggle User Status
- **Method**: `PATCH`
- **Path**: `/api/admin/users/{id}/toggle-status`
- **Access**: `SUPER_ADMIN`

---

## 3. Team APIs

### 3.1 Create Team
- **Method**: `POST`
- **Path**: `/api/teams`
- **Access**: `SUPER_ADMIN`
- **Request Body**:
```json
{
  "name": "Dev Team",
  "description": "Backend & Frontend Engineers",
  "managerId": 2
}
```

### 3.2 List Teams
- **Method**: `GET`
- **Path**: `/api/teams`

### 3.3 Add Member to Team (Dev, Intern, etc.)
- **Method**: `POST`
- **Path**: `/api/teams/{id}/members`
- **Access**: `SUPER_ADMIN`, `MANAGER`
- **Request Body**:
```json
{
  "name": "Aravind Senior Dev",
  "email": "aravind@finflow.com",
  "password": "Emp@1234",
  "contactNumber": "+91 9123456789"
}
```

### 3.4 List Team Members
- **Method**: `GET`
- **Path**: `/api/teams/{id}/members`

---

## 4. Leave APIs (RAG & Email Tokens)

### 4.1 Submit Leave Request
- **Method**: `POST`
- **Path**: `/api/leaves`
- **Access**: `EMPLOYEE`, `MANAGER`
- **Request Body**:
```json
{
  "leaveType": "SICK",
  "startDate": "2026-11-01",
  "endDate": "2026-11-03",
  "reason": "Viral fever, prescribed medical rest for 3 days."
}
```
- **Response** (`200 OK`):
```json
{
  "id": 1,
  "employeeId": 3,
  "employeeName": "Aravind Senior Dev",
  "employeeEmail": "aravind@finflow.com",
  "employeeContact": "+91 9123456789",
  "managerId": 2,
  "managerName": "Karthik Dev Manager",
  "leaveType": "SICK",
  "startDate": "2026-11-01",
  "endDate": "2026-11-03",
  "daysCount": 3,
  "reason": "Viral fever, prescribed medical rest for 3 days.",
  "ragDecision": "Leave request complies with the standard sick leave policy quota.",
  "status": "PENDING",
  "managerComment": null,
  "createdAt": "2026-10-13T22:30:00"
}
```

### 4.2 Leave Balance
- **Method**: `GET`
- **Path**: `/api/leaves/balance`
- **Response**:
```json
{
  "employeeId": 3,
  "employeeName": "Aravind Senior Dev",
  "year": 2026,
  "totalDays": 20,
  "usedDays": 0,
  "remainingDays": 20
}
```

### 4.3 Pending Leaves (Manager Review)
- **Method**: `GET`
- **Path**: `/api/leaves/pending`
- **Access**: `MANAGER`, `SUPER_ADMIN`

### 4.4 Approve Leave
- **Method**: `POST`
- **Path**: `/api/leaves/{id}/approve`
- **Access**: `MANAGER`, `SUPER_ADMIN`
- **Request Body**:
```json
{
  "comment": "Approved. Get well soon."
}
```

### 4.5 Reject Leave
- **Method**: `POST`
- **Path**: `/api/leaves/{id}/reject`
- **Access**: `MANAGER`, `SUPER_ADMIN`

---

## 5. Reimbursement Flow APIs

### 5.1 Submit Claim
- **Method**: `POST`
- **Path**: `/api/reimbursements`
- **Content-Type**: `multipart/form-data`
- **Parts**:
  - `bill`: PDF file
  - `amount`: `999.00`
  - `currency`: `INR`
  - `reason`: `Internet broadband bill for remote work`

### 5.2 My Reimbursements
- **Method**: `GET`
- **Path**: `/api/reimbursements/my`

### 5.3 Manager Pending Reclaims
- **Method**: `GET`
- **Path**: `/api/reimbursements/pending`
- **Access**: `MANAGER`, `SUPER_ADMIN`

### 5.4 Manager Forward to Finance
- **Method**: `POST`
- **Path**: `/api/reimbursements/{id}/forward`
- **Access**: `MANAGER`, `SUPER_ADMIN`

### 5.5 Reject Claim
- **Method**: `POST`
- **Path**: `/api/reimbursements/{id}/reject`
- **Access**: `MANAGER`, `FINANCE`, `SUPER_ADMIN`

---

## 6. Payment APIs — End-to-End Tracking

> **Guaranteed Tracking**: Whenever verified by finance admin or manager, the system shows and records:
> - Exact amount needed to pay
> - Employee ID
> - Employee Name
> - Employee Contact Number

### 6.1 View Payable Claims (Finance Dashboard)
- **Method**: `GET`
- **Path**: `/api/payments/pending`
- **Access**: `FINANCE`, `SUPER_ADMIN`, `MANAGER`
- **Response** (`200 OK`):
```json
[
  {
    "reimbursementId": 1,
    "employeeId": 3,
    "employeeName": "Aravind Senior Dev",
    "employeeEmail": "aravind@finflow.com",
    "employeeContactNumber": "+91 9123456789",
    "amount": 999.00,
    "currency": "INR",
    "reason": "Monthly fiber broadband internet bill for remote work",
    "status": "FINANCE_REVIEW",
    "managerName": "Karthik Dev Manager",
    "ragAnalysis": "Verified broadband bill of Rs. 999 against remote employee reimbursement policy.",
    "razorpayOrderId": null,
    "createdAt": "2026-10-13T21:00:00"
  }
]
```

### 6.2 Create Razorpay Order
- **Method**: `POST`
- **Path**: `/api/payments/order/{reimbursementId}`
- **Access**: `FINANCE`, `SUPER_ADMIN`, `MANAGER`
- **Response** (`200 OK`):
```json
{
  "razorpayOrderId": "order_QlKx3mAabc1234",
  "reimbursementId": 1,
  "amount": 999.00,
  "currency": "INR",
  "employeeId": 3,
  "employeeName": "Aravind Senior Dev",
  "employeeContactNumber": "+91 9123456789",
  "employeeEmail": "aravind@finflow.com",
  "razorpayKeyId": "rzp_test_TjVo10RBI3W8go"
}
```

### 6.3 Verify Payment & Immutably Record
- **Method**: `POST`
- **Path**: `/api/payments/verify`
- **Access**: `FINANCE`, `SUPER_ADMIN`, `MANAGER`
- **Request Body**:
```json
{
  "reimbursementId": 1,
  "razorpayOrderId": "order_QlKx3mAabc1234",
  "razorpayPaymentId": "pay_QlKy8mBbcd5678",
  "razorpaySignature": "simulated_signature_hash"
}
```
- **Response** (`200 OK`):
```json
{
  "id": 1,
  "reimbursementId": 1,
  "razorpayOrderId": "order_QlKx3mAabc1234",
  "razorpayPaymentId": "pay_QlKy8mBbcd5678",
  "amount": 999.00,
  "currency": "INR",
  "employeeId": 3,
  "employeeName": "Aravind Senior Dev",
  "employeeContact": "+91 9123456789",
  "paidByEmail": "finance@finflow.com",
  "paidAt": "2026-10-13T22:45:00"
}
```

### 6.4 Payment Audit History
- **Method**: `GET`
- **Path**: `/api/payments/history`
- **Access**: `FINANCE`, `SUPER_ADMIN`, `MANAGER`

---

## 7. Email Deep Link Action API

- **Method**: `GET` or `POST`
- **Path**: `/api/token/{token}/action`
- **Public**: Yes (Secured via single-use unguessable UUID tokens with expiration)
- **Description**: Clicked directly from email by managers to approve/reject leaves and reclaims in 1-click without requiring prior login.