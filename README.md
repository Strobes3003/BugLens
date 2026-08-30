# 🐞 BugLens

### Intelligent Bug Tracking & Issue Prioritization Platform

**BugLens** is a modern bug-tracking platform built for **CloneFest 2.0**, inspired by traditional issue trackers such as Bugzilla but designed around a more practical question:

> **“What should we fix next?”**

BugLens combines core issue tracking with dependency analysis and an intelligence layer that helps teams understand **impact, component health, release risk, and issue priority**.

---

## 🚀 Overview

Traditional bug trackers are good at storing and managing issues, but developers often still have to manually determine which issue deserves attention first.

BugLens extends conventional issue tracking with an intelligence layer:

```text
Bug Tracking
     ↓
Dependency Analysis
     ↓
Impact Score
     ↓
Fix Next
     ↓
Component Health
     ↓
Release Risk
```

The goal is to move from simply **tracking bugs** to helping development teams **make better fixing decisions**.

---

## ✨ Key Features

### 📋 Issue Management

* Create and edit issues
* Issue explorer
* Search issues
* Filter by:

  * Status
  * Severity
  * Priority
  * Assignee
* Sort issues
* Pagination
* Issue detail view
* Assignment
* Component and release association

### 🔄 Workflow Management

* Issue status management
* Workflow transitions
* Transition validation
* Status visualization
* Backend-controlled workflow rules

### 💬 Comments & Activity

* Add comments to issues
* Edit and delete comments
* Public/internal comment visibility
* Issue activity timeline
* Track important issue events

### 🔗 Issue Dependencies

* Add and remove issue dependencies
* View dependency relationships
* Dependency graph visualization
* Dependency analysis
* Cycle detection
* Blast-radius analysis

### 🧠 Intelligence Layer

BugLens provides an intelligence layer on top of traditional issue tracking.

#### Impact Score

Helps identify issues with greater potential impact based on issue and dependency information.

#### Fix Next

Ranks issues to help answer:

> **Which issue should the team fix next?**

#### Component Health

Provides visibility into the health and risk associated with project components.

#### Release Risk

Helps identify issues that may increase the risk of a release.

---

## 🏗️ Architecture

BugLens uses a **modular monolith architecture**.

It is intentionally **not implemented as microservices**. All backend modules run inside a single Spring Boot application while maintaining clear module boundaries.

```text
                    ┌─────────────────────┐
                    │    React Frontend   │
                    │       + Vite        │
                    └──────────┬──────────┘
                               │
                              HTTPS
                               │
                    ┌──────────▼──────────┐
                    │   Spring Boot API   │
                    │   Modular Monolith  │
                    └──────────┬──────────┘
                               │
                         JPA / JDBC
                               │
                    ┌──────────▼──────────┐
                    │     PostgreSQL      │
                    └─────────────────────┘
```

### Core Design Principle

> **Bug tracking is the foundation. Intelligence is the differentiator.**

The architecture keeps the system modular, simple, maintainable, and suitable for a hackathon project.

---

## 🛠️ Technology Stack

### Frontend

* React
* Vite
* JavaScript / JSX
* REST API integration
* Responsive UI

### Backend

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* JWT Authentication
* Bean Validation
* Lombok

### Database

* PostgreSQL
* Flyway database migrations

### Deployment

* Vercel — Frontend
* Render — Backend
* PostgreSQL — Production database

---

## 📁 Project Structure

```text
buglens/
│
├── frontend/
│   └── src/
│       ├── api/
│       ├── components/
│       ├── features/
│       ├── pages/
│       ├── hooks/
│       └── utils/
│
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/com/buglens/
│       │   │   ├── auth/
│       │   │   ├── workspace/
│       │   │   ├── project/
│       │   │   ├── component/
│       │   │   ├── issue/
│       │   │   ├── workflow/
│       │   │   ├── dependency/
│       │   │   ├── comment/
│       │   │   ├── activity/
│       │   │   ├── release/
│       │   │   ├── intelligence/
│       │   │   │   ├── scoring/
│       │   │   │   ├── graph/
│       │   │   │   ├── health/
│       │   │   │   └── risk/
│       │   │   ├── dashboard/
│       │   │   └── common/
│       │   │
│       │   └── resources/
│       │       └── db/
│       │           └── migration/
│       │
│       └── pom.xml
│
├── database/
│   └── seed/
│
├── docs/
│
└── README.md
```

---

## 🧩 Backend Modules

The backend follows a consistent modular structure:

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/
```

### Modules

```text
auth/
workspace/
project/
component/
issue/
workflow/
dependency/
comment/
activity/
release/
intelligence/
```

The intelligence module contains:

```text
intelligence/
├── scoring/
├── graph/
├── health/
└── risk/
```

This separation keeps business logic organized while retaining the simplicity of a single Spring Boot application.

---

## 🔐 Backend as the Source of Truth

BugLens follows an important architectural rule:

> **The backend is the source of truth for business rules.**

The backend controls:

* Authentication
* Authorization
* Permissions
* Workflow transition validation
* Dependency validation
* Cycle detection
* Impact Score calculation
* Fix Next ranking
* Component Health calculation
* Release Risk calculation

The frontend is responsible for:

* Displaying data
* Sending user actions
* Displaying backend validation/errors
* Visualizing backend results

This keeps business logic out of the UI and prevents inconsistent behavior between clients.

---

## 🔌 API Architecture

The React frontend communicates with the Spring Boot backend through REST APIs.

Example:

```http
GET /api/issues/{issueId}
```

Example response:

```json
{
  "issueKey": "BL-142",
  "title": "Login failure",
  "status": "IN_PROGRESS",
  "severity": "CRITICAL",
  "priority": "HIGH",
  "assignee": {
    "id": 12,
    "name": "Rahul"
  },
  "component": "Authentication",
  "release": "v2.4"
}
```

The frontend consumes backend APIs and does not duplicate backend business rules.

---

## 🗄️ Database & Migrations

BugLens uses PostgreSQL for persistent storage.

Database schema changes are managed using **Flyway migrations**.

Migrations are stored under:

```text
backend/src/main/resources/db/migration/
```

Example:

```text
V1__create_users.sql
V2__create_workspaces.sql
V3__create_projects.sql
V4__create_components.sql
V5__create_releases.sql
V6__create_issues.sql
V7__create_workflows.sql
V8__create_comments.sql
V9__create_dependencies.sql
```

### Migration Rule

Previously committed migrations should not be modified.

Instead, create a new migration:

```text
V10__alter_issue_table.sql
```

The root `database/` directory is reserved for seed/demo data and database documentation.

---

## 👥 Team & Work Division

BugLens was developed by a **4-member team**.

### Member 1 — Frontend Foundation

Responsible for:

* Authentication UI
* Workspace UI
* Projects
* Components
* Releases
* Routing
* Shared UI components
* Layout and navigation

### Member 2 — Frontend Core

Responsible for:

* Issues
* Issue Explorer
* Search/filter/sort
* Issue details
* Workflow UI
* Comments
* Activity
* Dependencies
* Dependency visualization
* Intelligence UI
* Fix Next UI
* Component Health UI
* Release Risk UI

### Member 3 — Backend Foundation

Responsible for:

* Authentication
* Security
* JWT
* Workspace
* Projects
* Components
* Releases
* Permissions
* Related database modules

### Member 4 — Backend Core

Responsible for:

* Issues
* Workflow
* Comments
* Activity
* Dependencies
* Intelligence
* Impact Score
* Fix Next
* Component Health
* Release Risk
* Related database modules

---

## 🌿 Git Branch Structure

The project uses separate branches for the major areas of development.

```text
main
│
├── frontend-foundation
├── frontend-issues
├── backend-foundation
└── backend-core
```

### Ownership

| Branch                | Responsibility                                                      |
| --------------------- | ------------------------------------------------------------------- |
| `frontend-foundation` | Frontend foundation, authentication, workspace, projects, shared UI |
| `frontend-issues`     | Issues, workflow, comments, dependencies, intelligence UI           |
| `backend-foundation`  | Authentication, security, workspace, projects, components, releases |
| `backend-core`        | Issues, workflow, comments, dependencies, intelligence              |

The `main` branch contains the integrated project.

---

## 💻 Local Development

### Prerequisites

Make sure the following are installed:

* Java
* Maven
* Node.js
* npm
* PostgreSQL
* Git

---

### 1. Clone the repository

```bash
git clone https://github.com/Strobes3003/BugLens.git
cd BugLens
```

---

### 2. Start the Backend

```bash
cd backend
```

Configure the required environment variables for PostgreSQL and application configuration.

Then run:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

---

### 3. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
```

Configure:

```text
VITE_API_BASE_URL
```

For local development, point it to the local backend API:

```text
http://localhost:8080/api
```

Then start Vite:

```bash
npm run dev
```

The frontend will be available through the Vite development server.

---

## 🌐 Production Deployment

BugLens is deployed using a separate frontend and backend deployment.

### Frontend

**Vercel**

Production frontend:

`buglens-beige.vercel.app`

### Backend

**Render**

Production backend:

`buglens-backend-a72h.onrender.com`

The production frontend uses:

```text
VITE_API_BASE_URL=https://buglens-backend-a72h.onrender.com/api
```

---

## 🔑 Demo Account

A test account is available for demonstration.

```text
Email: test@buglens.local
Password: [shared separately for demonstration]
```

> For security, production credentials, database credentials, JWT secrets, and other sensitive environment variables are intentionally not stored in this repository.

---

## 🧪 Testing & Verification

The project was verified across the major application areas, including:

* Authentication
* Issue APIs
* Issue listing
* Issue details
* Workflow operations
* Comments
* Activity
* Dependencies
* Intelligence-related endpoints
* Frontend integration
* Production backend deployment
* Frontend production deployment

Frontend production builds were also verified successfully.

---

## 🎯 Project Goal

BugLens is designed around a simple progression:

```text
Store Issues
     ↓
Understand Relationships
     ↓
Measure Impact
     ↓
Prioritize Work
     ↓
Understand Component Health
     ↓
Assess Release Risk
```

Instead of treating a bug tracker as just a database of issues, BugLens aims to turn issue data into actionable engineering intelligence.

---

## 🧠 Why BugLens?

Most issue trackers answer:

> **“What issues do we have?”**

BugLens aims to answer:

> **“Which issue should we fix next, and why?”**

By combining:

* Severity
* Priority
* Dependencies
* Issue activity
* Component health
* Release information
* Dependency impact

BugLens provides a foundation for smarter engineering prioritization.

---

## 🏆 CloneFest 2.0

**Project:** BugLens
**Event:** CloneFest 2.0
**Category:** Developer Tool Reconstruction / Bug Tracking

BugLens takes inspiration from established bug-tracking workflows while introducing a modern interface and an intelligence-oriented approach to issue prioritization.

---

## 📌 Core Architectural Principle

```text
React
  ↓
Spring Boot Modular Monolith
  ↓
PostgreSQL
```

The project intentionally avoids unnecessary microservice complexity.

The focus is on:

* Clear module boundaries
* Maintainable code
* Strong API contracts
* Backend-driven business rules
* Practical issue tracking
* Actionable engineering intelligence

---

## 📄 License

This project was created as part of CloneFest 2.0.
