# 🏗️ Full System Architecture


---

## 🧑‍💼 Organization Hierarchy

```
Super Admin (Org Level)
    ├── Manager 1 → Dev Team    (Senior Dev, Junior Dev, Intern)
    ├── Manager 2 → Sales Team
    └── Manager 3 → Testing Team
```

- Super Admin creates the org account and manages managers
- Each Manager adds their own team members
- Finance Team is a special role (separate from managers/employees)

## 🔁 Module 1 — Leave Policy Flow

```
Employee                  Manager                   RAG System               DB
   │                         │                           │                    │
   │──── Submit Leave Req ──▶│                           │                    │
   │                         │──── Email (action link) ──▶ Employee's inbox   │
   │                         │                           │                    │
   │                    [Manager opens link]             │                    │
   │                         │──── Fetch emp leave data ─────────────────────▶│
   │                         │◀─── Leave balance returned ────────────────────│
   │                         │                           │                    │
   │                         │──── RAG: Validate leave ─▶│                    │
   │                         │◀─── Approved / Rejected ──│                    │
   │                         │                           │                    │
   │◀──── Email (result) ────│                           │                    │
   │                         │──── Update leave in DB ───────────────────────▶│
```

### Email Triggers (Leave)
| Event | Recipient | Content |
|---|---|---|
| Employee submits leave | **Manager** | Link to review request |
| Manager approves | **Employee** | Approval + leave balance update |
| Manager rejects | **Employee** | Rejection + reason |

---

## 🔁 Module 2 — Money Reclaim Flow

```
Employee        Manager         RAG System       Finance Team      Razorpay         DB
   │               │                │                 │               │              │
   │─ Submit req ─▶│                │                 │               │              │
   │  (msg+PDF)    │                │                 │               │              │
   │               │─ Store reason ─────────────────────────────────────────────────▶│
   │               │                │                 │               │              │
   │               │─ Email ───────────────────────────────────────▶ Manager inbox   │
   │               │                │                 │               │              │
   │         [Manager reviews]      │                 │               │              │
   │               │                │                 │               │              │
   │               │─ RAG: Analyze ▶│                 │               │              │
   │               │  (PDF bill vs  │                 │               │              │
   │               │   reason vs    │                 │               │              │
   │               │   policy)      │                 │               │              │
   │               │◀─ RAG Report ──│                 │               │              │
   │               │                │                 │               │              │
   │               │─ Forward to Finance + RAG Report ▶│              │              │
   │               │                │                 │               │              │
   │               │                │          [Finance reviews]      │              │
   │               │                │         [Single-click Pay] ────▶│              │
   │               │                │                 │◀── Order ID ──│              │
   │               │                │                 │─ Verify Sig ──▶              │
   │               │                │                 │                              │
   │               │                │                 │─ Store payment record ──────▶│
   │               │                │                 │                              │
   │◀─ Email (Paid) ────────────────────────────────────────────────────────────────│
   │               │◀─ Email (Paid) ─────────────────────────────────────────────── │
```

### Email Triggers (Money Reclaim)
| Event | Recipient | Content |
|---|---|---|
| Employee submits reclaim | **Manager** | Link to review bill + reason |
| Manager forwards to Finance | **Finance Team** | Link to RAG-analyzed report + pay button |
| Finance approves + pays | **Employee + Manager** | Payment confirmation + amount |
| Finance rejects | **Employee + Manager** | Rejection notice + reason |

---


## 📧 Email Service (Spring Boot Mail  To Be Added)

- **Provider**: Gmail SMTP / SendGrid (via `spring-boot-starter-mail`)
- **Templates**: HTML email templates (in `/resources/templates/`)
- **Deep Links**: `https://app.com/review/{token}` — JWT-signed, one-time use
- **Events**: All approve/reject/forward actions send emails

---

## 💳 Razorpay Integration (Already Exists — Needs Enhancement)

### Current State
- ✅ `createOrder(amount, currency)` → creates Razorpay order
- ✅ `verifyPayment(paymentId, orderId, signature)` → verifies payment

### What Needs to Change
- ✅ Finance team triggers `createOrder` from the UI with **single click**
- ✅ After payment verified → **store in `payment_records` DB table**
- ✅ After payment stored → **send email to Employee + Manager**
- ✅ Update `reimbursement_requests.status = PAID`

---

## 🧠 RAG System 

### Current State
- ✅ Ingests `leave-policy.txt` into vector store
- ✅ `/api/ask?q=` → answers leave policy questions


---
## ⚛️ React Frontend Structure

```
src/
├── pages/
│   ├── Login.jsx
│   ├── Dashboard.jsx          (role-aware)
│   ├── LeaveRequest.jsx       (Employee)
│   ├── LeaveReview.jsx        (Manager)
│   ├── ReimburseRequest.jsx   (Employee)
│   ├── ReimburseReview.jsx    (Manager)
│   ├── FinanceReview.jsx      (Finance — with Razorpay pay button)
│   └── AdminPanel.jsx         (Super Admin)
├── components/
│   ├── Navbar.jsx
│   ├── RoleGuard.jsx          (JWT role-based route guard)
│   ├── EmailLinkReview.jsx    (opened from email deep link)
│   └── RazorpayButton.jsx     (single-click pay)
└── services/
    ├── api.js                 (Axios with JWT interceptor)
    └── auth.js
```

---
