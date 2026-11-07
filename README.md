# 🌐 FinFlow ( Backend Engineering Focused )

<p align="left">
  <img src="https://img.shields.io/badge/Security-Trivy%20Scanned-1B6AC6?logo=aqua&logoColor=white" alt="Trivy Scanned" />
</p>

<p align="left">
  <img src="https://skillicons.dev/icons?i=java,spring,postgres,docker,githubactions&theme=light" alt="Tech Stack" />
</p>
> An enterprise workforce operations platform combining Retrieval-Augmented Generation (RAG) HR policy advisory, automated quota management, and 1-click secure email approvals.

---

## 👥 Project Scope & Contributions

- **Backend Architecture, RAG Engine, Docker & CI/CD**: Designed and implemented by **[Monesh D](https://github.com/moneshgomo)**.  
  *Core focus: Spring Boot enterprise architecture, Spring AI RAG pipeline, OpenRouter embedding/chat integration, vector store indexing, single-use email action tokens, multi-stage backend Docker optimization, and GitHub Actions CI/CD with Trivy CVE scanning.*
- **Frontend Development**: Implemented by collaborator but not me .  
  *Core focus: React SPA, Vite build tooling, role-aware dashboards (Super Admin, Manager, Employee, Finance), and responsive UI components.*

> **Note on Containerization**: The Docker image, multi-stage build, and CI/CD workflow in this repository are **strictly for the Backend service**. The frontend runs independently as a static React application.

---

## 🎯 Project Overview

### 📍 Problem statement 
Traditional company operations handle employee leaves and expense reimbursements through fragmented email threads, manual spreadsheets, and 50+ page HR policy PDFs. Managers often do not remember complex policy rules (advance notices, consecutive medical limits, doctor note mandates), leading to delayed approvals, out-of-sync leave balances, and administrative friction.

### 🎯 Plan
Architect and build a secure, production-grade backend system that:
1. Automatically verifies employee leave requests against company policy handbooks in real time using AI.
2. Manages organizational department hierarchies and synchronizes annual leave balances transactionally.
3. Allows managers to review and make 1-click decisions directly from their email inbox without logging in.
4. Packages the backend into a hardened, lightweight Docker container deployed via an automated CI/CD pipeline.

### ⚙️  Implementation
- **RAG Policy Engine**: Integrated Spring AI with OpenRouter using `nvidia/nemotron-3-embed-1b:free` (1B parameter compact embedding model) and `openrouter/free` LLM. Document ingestion breaks `leave-policy.txt` into markdown-aware sections with deterministic UUID hashing to prevent duplicate indexing in a persistent `SimpleVectorStore` (`data/vectorstore.json`).
- **Security & Authorization**: Implemented stateless JWT authentication, BCrypt password hashing, and role-based access control (`SUPER_ADMIN`, `MANAGER`, `EMPLOYEE`, `FINANCE`).
- **1-Click Interactive Email Actions**: Built an email service (`spring-boot-starter-mail`) generating single-use cryptographic tokens (UUID, 24-hr TTL). When clicked, the action is executed and the token is permanently burned.
- **Container Optimization (Backend Only)**: Authored a multi-stage `Dockerfile` (`maven:3.9-eclipse-temurin-21` &rarr; `eclipse-temurin:21-jre`) slashing image size by >60%, removing build tools from runtime, and establishing dedicated data paths.
- **CI/CD Pipeline**: Configured GitHub Actions (`.github/workflows/cd.yaml`) with Docker Buildx registry caching (`mode=max`), immutable Git SHA image tags, and Aqua Security Trivy vulnerability scans.

### 🏆 Outcomes
- **Instant Approvals**: Cut leave approval turnaround time from days to seconds with 1-click email action links.
- **Zero Policy Ambiguity**: Provides managers with concise, factual (<100 words) policy compliance advisories with human-in-the-loop decision-making.
- **Accurate Quotas**: Automated real-time deduction ensures leave balances remain synchronized without spreadsheets.
- **Lean & Secure Deliverable**: Production backend Docker image stripped of build tooling and verified clean by automated Trivy vulnerability scanning.

---

## 📊 Feature Status & Roadmap

| Feature / Module | Status | Details |
|---|---|---|
| **User Authentication & RBAC** | ✅ **Completed** | JWT authentication, BCrypt hashing, roles (`SUPER_ADMIN`, `MANAGER`, `EMPLOYEE`, `FINANCE`). |
| **User Onboarding Flow** | ✅ **Completed** | Admin invites user &rarr; temporary credentials email &rarr; 24h password setup token. |
| **Team & Hierarchy Management** | ✅ **Completed** | Super Admin creates teams/managers; managers add team members. |
| **Leave Management Flow** | ✅ **Completed** | Employee submits leave &rarr; balance checked &rarr; RAG advisory generated &rarr; manager notified. |
| **RAG HR Policy Advisor** | ✅ **Completed** | OpenRouter Nemotron-1B embeddings, OpenRouter chat model, persistent vector store. |
| **1-Click Email Decision Tokens** | ✅ **Completed** | Single-use cryptographic action tokens for instant email approval/rejection. |
| **Multi-Stage Docker Image (Backend)** | ✅ **Completed** | Build/runtime separation, Eclipse Temurin 21 JRE base, low image footprint. |
| **CI/CD Pipeline & Vulnerability Scan** | ✅ **Completed** | GitHub Actions build, Docker Hub registry caching, Trivy CVE scanning. |
| **PDF Receipt OCR Extraction** | ⏳ *In Progress* | PDF.co presigned URL upload and optical text extraction. |
| **AI Bill Parsing & Validation** | ⏳ *In Progress* | Groq Cloud API (`openai/gpt-oss-20b`) for invoice data extraction. |
| **Finance Single-Click Razorpay Payout**| ⏳ *In Progress* | Single-click Razorpay payment order creation and signature verification. |
| **Payment Audit & Receipt Generation** | ⏳ *In Progress* | Immutable payment ledger and automated PDF payment receipts. |

---

## 🏛️ System Architecture & Workflow

### Leave Request & AI Policy Flow (Completed)

``` bash
👤 Employee     →  ⚙️ Backend      →  🧠 RAG Engine     →  📩 Manager     →  ✅ Quota Updated
    Request        Check Balance       Evaluate Policy      Email Decision        Notify Status
```
---

## 🧠 RAG Policy Engine Configuration

The RAG pipeline is configured in [`Backend/src/main/resources/application-dev.yml`](rag/Backend/src/main/resources/application.yml):

```yaml
spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENAI_API_KEY}
      timeout: 120s
      embedding:
        model: nvidia/nemotron-3-embed-1b:free   # 1-Billion param compact embedding model
      chat:
        model: openrouter/free                  # Reasoning LLM for policy synthesis
      max-retries: 3
```

- **Vector Store**: Spring AI `SimpleVectorStore` with JSON persistence to `data/vectorstore.json`.
- **Chunking Strategy**: Markdown header-aware splitting (`(?m)(?=^## )`) ensuring zero semantic loss across sections.
- **Deduplication**: Content-derived deterministic hashing (`UUID.nameUUIDFromBytes(content)`).
- **Retrieval**: Cosine similarity search with `topK = 3`.
- **Human-in-the-Loop Guardrail**: The advisor delivers concise recommendations (<100 words); only the human manager can approve or reject.

---

## 🔌 Core Backend REST APIs

### 1. Authentication & Onboarding (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Super Admin registration (initial setup) |
| `POST` | `/api/auth/login` | Public | User login & Bearer JWT issuance |
| `GET` | `/api/auth/me` | Authenticated | Current user profile |
| `GET` | `/api/auth/setup-password/validate` | Public | Validates one-time setup token |
| `POST` | `/api/auth/setup-password` | Public | Sets permanent user password |
| `POST` | `/api/auth/regenerate-setup-link` | Public | Reissues expired onboarding link |

### 2. Admin & Teams (`/api/admin`, `/api/teams`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/admin/managers` | `SUPER_ADMIN` | Creates department managers |
| `GET` | `/api/admin/managers` | `SUPER_ADMIN` | Lists department managers |
| `POST` | `/api/admin/users` | `SUPER_ADMIN` | Adds users (employees/finance) |
| `GET` | `/api/admin/users` | `SUPER_ADMIN` | Lists users (filterable by `?role=`) |
| `PATCH`| `/api/admin/users/{id}/toggle-status` | `SUPER_ADMIN` | Activates/deactivates an account |
| `POST` | `/api/teams` | `SUPER_ADMIN` | Creates a team and assigns a manager |
| `GET` | `/api/teams` | Authenticated | Lists teams |
| `POST` | `/api/teams/{id}/members` | `SUPER_ADMIN`, `MANAGER` | Adds members to a team |
| `GET` | `/api/teams/{id}/members` | Authenticated | Lists team members |

### 3. Leave Management & Policy Advisory (`/api/leaves`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/leaves` | `EMPLOYEE` | Submits leave request; runs quota check & RAG advisory |
| `GET` | `/api/leaves/balance` | `EMPLOYEE` | Returns user's remaining leave balance |
| `GET` | `/api/leaves/my` | `EMPLOYEE` | Returns user's leave history |
| `GET` | `/api/leaves/pending` | `MANAGER` | Lists pending team leave requests |
| `POST` | `/api/leaves/{id}/approve` | `MANAGER` | Approves leave and deducts quota |
| `POST` | `/api/leaves/{id}/reject` | `MANAGER` | Rejects leave with reason |

### 4. RAG Ingestion & Token Actions (`/api/ingest`, `/api/ask`, `/api/token`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/ingest` | Public / Admin | Reads and embeds `docs/leave-policy.txt` into vector store |
| `GET` | `/api/search?q=` | Public / Admin | Semantic search against policy embeddings |
| `GET` | `/api/ask?q=` | Public / Admin | Direct query to HR policy advisor |
| `GET` | `/api/token/{token}/action` | Public (Token-gated) | Executes 1-click email action and burns token |

---

## 🐳 Docker Image Optimization

The backend is packaged using a two-stage build in [`Backend/Dockerfile`](rag/Backend/Dockerfile):

```dockerfile
# Stage 1: Build & Package (Discarded after build)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Why This Docker Setup?
1. **Separation of Concerns**: Maven compiler, JDK utilities, and dependency download caches are kept out of the runtime image.
2. **Lean Runtime**: Using `eclipse-temurin:21-jre` instead of a full JDK cuts the image size by more than **60%**, accelerating container pull and start times.
3. **Data Isolation**: Persistent directory `/app/data` holds the `vectorstore.json` file independently of root system directories.
4. **Hardened Surface**: Zero build tools or source code present in the production container layer.

---

## 🔄 CI/CD Pipeline & Security Scanning

The deployment workflow in [`.github/workflows/cd.yaml`](rag/.github/workflows/cd.yaml) triggers on every push to `main`:

```
Push to main
     │
     ▼
[GitHub Actions Runner (ubuntu-latest)]
     ├── 1. Checkout repository code
     ├── 2. Setup Docker Buildx
     ├── 3. Authenticate with Docker Hub
     ├── 4. Build & Push Backend image:
     │       • Tags: :${GITHUB_SHA} and :dev
     │       • Registry cache: mode=max (build cache re-use)
     └── 5. Scan image with Aqua Security Trivy for CVEs
```

---

## 🚀 How to Run the Project

### Prerequisites
- **Java 21 LTS**
- **Maven 3.9+**
- **PostgreSQL 15+**
- **Docker** *(optional, for containerized run)*
- **Node.js 18+ & npm** *(for frontend)*

---

### Step 1: Database Setup
Create a PostgreSQL database named `finflow`:
```sql
CREATE DATABASE finflow;
```

---

### Step 2: Configure Environment Variables
Create a `.env` file in the `Backend/` directory:
```env
SPRING_APPLICATION_NAME=finflow
PORT=8080

# Database
DB_URL=jdbc:postgresql://localhost:5432/finflow
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# OpenRouter AI (RAG)
OPENAI_BASE_URL=https://openrouter.ai/api/v1
OPENAI_API_KEY=your_openrouter_api_key
OPENAI_EMBEDDING_MODEL=nvidia/nemotron-3-embed-1b:free
OPENAI_CHAT_MODEL=openrouter/free

# Mail Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_app_password

# Security
JWT_SECRET=your_super_secret_jwt_signing_key_at_least_256_bits
JWT_EXPIRATION_MS=86400000

# App URLs
APP_BASE_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:5173
```

---

### Step 3: Run the Backend

#### Option A: Run Locally with Maven
```bash
cd Backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Option B: Run via Docker (Backend Only)
```bash
cd Backend
docker build -t finflow-backend:dev .
docker run -p 8080:8080 --env-file .env finflow-backend:dev
```

---

### Step 4: Ingest the HR Policy Documents (One-Time Setup)
Once the backend starts up on port `8080`, ingest the policy documents into the vector store:
```bash
curl -X POST http://localhost:8080/api/ingest
```
*Expected response: Number of chunks ingested and persisted to `data/vectorstore.json`.*

---

### Step 5: Run the Frontend
In a separate terminal, start the React application:
```bash
cd Frontend
npm install
npm run dev
```
The application interface will be accessible at: `http://localhost:5173`.

---

## 🛡️ Security & Auditing Principles

1. **Human-in-the-Loop**: The AI advisor is strictly non-binding; only authorized human managers approve or reject requests.
2. **Single-Use Action Tokens**: Approval and rejection links sent via email are single-use UUIDs that expire after 24 hours and are immediately invalidated once accessed.
3. **Role Segregation**: Managers can only view and act on team members assigned to them; Super Admin oversight maintains organizational boundary compliance.
