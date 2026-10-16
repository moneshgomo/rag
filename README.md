# 🏗️ Full System Architecture

## ✅ What I Understand From the Codebase + Your Requirements

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

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 4.1.1 (Monolithic) |
| **RAG / AI** | Spring AI 2.0.1 + OpenRouter (nvidia/nemotron) |
| **Bill Analysis** | Groq API (`openai/gpt-oss-20b`) |
| **PDF Parsing** | PDF.co API (3rd party) |
| **Payment** | Razorpay Java SDK (Test Mode) |
| **Email** | Spring Boot Email (JavaMailSender / SMTP) |
| **Database** | SQL (to be added — JPA/Hibernate) |
| **Security** | JWT (to be added) |
| **Frontend** | React (to be added) |
| **Vector Store** | In-memory / Spring AI Vector Store |

---

## 📦 Current Codebase State

### ✅ Already Exists
| File | Purpose |
|---|---|
| `RagController` + `RagService` + `IngestService` | RAG pipeline — ingest leave-policy.txt, search, ask |
| `VectorStoreConfig` | Spring AI vector store config |
| `ReimbursementController` + `ReimbursementService` | Reimbursement flow (process PDF bill + reason) |
| `PdfExtractionService` | PDF.co API integration for extracting bill text |
| `AiBillAnalysisService` | Groq API — analyzes extracted bill text |
| `PaymentController` + `PaymentService` | Razorpay — create order + verify payment signature |
| `Reimbursement.java` (model) | POJO with amount, currency, billContext, reason, tlName, employeeName, teamName |
| `ReimbursementRequest.java` (DTO) | Input DTO with MultipartFile bill + all fields |
| `leave-policy.txt` | Policy document ingested into vector store |

### ❌ Missing / To Be Built
- SQL Database (JPA entities + repositories)
- JWT Security layer
- User Management (SuperAdmin, Manager, Employee, Finance roles)
- Email Service (Spring Mail)
- Leave Request module
- React Frontend
- Razorpay payment trigger from Finance team UI
- DB storage for reclaim reasons (for future RAG tuning)
- Email deep-links for approval/rejection

---

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

## 🗄️ Database Schema (To Be Added)

### Tables Needed

```sql
-- Users Table
users (id, name, email, password_hash, role [SUPER_ADMIN/MANAGER/EMPLOYEE/FINANCE], team_id, created_at)

-- Teams Table
teams (id, name, manager_id, created_at)

-- Leave Requests Table
leave_requests (id, employee_id, manager_id, start_date, end_date, days, reason, 
                status [PENDING/APPROVED/REJECTED], rag_decision, created_at, updated_at)

-- Leave Balance Table
leave_balance (id, employee_id, total_days, used_days, remaining_days, year)

-- Reimbursement Requests Table
reimbursement_requests (id, employee_id, manager_id, amount, currency, reason, 
                        bill_url, rag_analysis, status [PENDING/MANAGER_REVIEW/FINANCE_REVIEW/PAID/REJECTED],
                        razorpay_order_id, razorpay_payment_id, created_at, updated_at)

-- Payment Records Table
payment_records (id, reimbursement_id, razorpay_order_id, razorpay_payment_id, 
                 razorpay_signature, amount, currency, paid_at, paid_by_user_id)

-- Email Tokens Table (for secure deep links)
email_action_tokens (id, token [UUID], request_type [LEAVE/REIMBURSEMENT], request_id, 
                     action [APPROVE/REJECT/REVIEW], expires_at, used)
```

---

## 🔐 JWT Security Layer (To Be Added)

```
POST /auth/login        → Returns JWT token
POST /auth/register     → Super Admin registers org

All other endpoints     → Require Bearer JWT in Authorization header
Role-based access:
  - SUPER_ADMIN  → Full access
  - MANAGER      → Own team requests only
  - EMPLOYEE     → Own requests only
  - FINANCE      → Reimbursement approvals only
```

---

## 📧 Email Service (Spring Boot Mail — To Be Added)

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

## 🧠 RAG System (Partially Exists — Needs Enhancement)

### Current State
- ✅ Ingests `leave-policy.txt` into vector store
- ✅ `/api/ask?q=` → answers leave policy questions
- ✅ `AiBillAnalysisService` → analyzes bill text via Groq

### What Needs to Change
- Add **reclaim policy document** to vector store
- RAG validates reimbursement: `bill content + employee reason vs reclaim policy`
- Store reclaim reasons in DB for **future RAG fine-tuning**
- RAG decision stored in `reimbursement_requests.rag_analysis` column

---

## ⚛️ React Frontend Structure (To Be Built)

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

## 🔄 What We Build Next (Suggested Order)

1. **Add DB** → JPA entities + H2/PostgreSQL + repositories
2. **Add JWT** → Spring Security + JWT filter + role-based access
3. **Add Email** → Spring Mail + HTML templates + token-based deep links
4. **Enhance Razorpay** → tie payment to DB + email trigger
5. **Enhance Reimbursement** → save reason to DB, store RAG result
6. **Add Leave Request** → full CRUD + RAG validation
7. **React Frontend** → all pages + JWT auth + Razorpay UI
