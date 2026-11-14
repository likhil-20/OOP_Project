# I-ITSM Architecture

## 1. System Architecture

```mermaid
flowchart LR
    U["Browser: React 18 SPA<br/>Axios + JWT<br/>Web Speech API"] -->|"JSON :3000 to :8080"| API

    subgraph Backend ["Spring Boot 3.2 backend"]
        direction TB
        API["ITSMController (thin)<br/>REST /api/**"]
        SEC["AuthInterceptor + JwtUtil<br/>BCrypt, LoginAttemptService"]
        EXC["GlobalExceptionHandler"]
        SVC["Services: Auth, Incident,<br/>Admin, Analytics, Triage,<br/>Notification, Email"]
        STR["Strategy: SkillBased / LoadBased"]
        FAC["IncidentFactory"]
        LIF["IncidentLifecycleManager"]
        AI["AI: IncidentClassifier<br/>GeminiClassifier / KeywordClassifier"]
        PUB["IncidentEventPublisher"]
        OBS["Observers: Audit, Notification,<br/>Email, Webhook"]
        SCH["Schedulers: EscalationService 60s,<br/>SlaMonitorService 30s"]
        REPO["Spring Data JPA Repositories"]
        API --> SEC
        API --> SVC
        SVC --> STR
        SVC --> FAC
        SVC --> LIF
        SVC --> AI
        SVC --> PUB --> OBS
        SVC --> REPO
        OBS --> REPO
        SCH --> REPO
        API --> EXC
    end

    AI -->|HTTPS| GEM["Google Gemini API"]
    OBS -->|HTTPS| HOOK["Slack / Discord webhook"]
    REPO -->|JDBC| DB[("PostgreSQL 18")]
```

## 2. Layers
| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `frontend/src` | Role-based dashboards, AI triage form with voice input |
| API | `controller` | Receives requests, calls services, returns JSON |
| Security | `security`, `config` | JWT check, role check, login lockout, BCrypt bean |
| Business logic | `service` | Incident rules, priority, SLA, escalation, admin, analytics, triage |
| Domain / patterns | `strategy`, `factory`, `observer`, `lifecycle`, `ai` | Assignment, creation, events, state rules, classification |
| Persistence | `repository`, `model` | JPA entities and repositories |
| Cross-cutting | `exception`, `dto` | Error handling, validated request/response objects |

## 3. Deployment

```mermaid
flowchart LR
    subgraph Docker Compose
        F["frontend: nginx serving the React build :3000"]
        B["backend: temurin-17 JRE :8080"]
        D[("postgres:18 :5432 (local profile)")]
    end
    F --> B --> D
    B -.-> N[("Neon PostgreSQL (shared)")]
    B -.-> G["Gemini API"]
```

## 4. Create-Incident Sequence

```mermaid
sequenceDiagram
    actor E as Employee
    participant UI as React
    participant AI as AuthInterceptor
    participant C as ITSMController
    participant T as TriageService
    participant S as IncidentService
    participant DB as PostgreSQL
    participant P as EventPublisher
    E->>UI: Types or speaks the problem
    UI->>C: POST /api/incidents/triage
    C->>T: triage(description)
    T-->>UI: category, impact, urgency, service, confidence
    E->>UI: Reviews and submits
    UI->>AI: POST /api/incidents + Bearer token
    AI->>AI: Verify JWT, load user, check role
    AI->>C: authUser attribute
    C->>S: reportIncident(request, user)
    S->>S: Validate, calculate priority, set SLA, detect duplicate
    S->>S: AssignmentStrategy.assignTechnician()
    S->>DB: save incident, update technician load
    S->>P: publish INCIDENT_CREATED
    P->>DB: Audit, in-app notification, email log
    P-->>P: Webhook (P1 only)
    C-->>UI: 200 incident JSON
```

## 5. Incident Lifecycle

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> CLASSIFIED
    CLASSIFIED --> ASSIGNED
    ASSIGNED --> ACKNOWLEDGED
    ASSIGNED --> RESOLVED
    ACKNOWLEDGED --> IN_PROGRESS
    ACKNOWLEDGED --> RESOLVED
    IN_PROGRESS --> RESOLVED
    RESOLVED --> VERIFIED
    RESOLVED --> CLOSED
    VERIFIED --> CLOSED
    CLOSED --> [*]
```