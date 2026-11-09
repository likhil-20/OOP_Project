# I-ITSM Architecture

## 1. System Architecture

````mermaid
flowchart LR
    U[Browser: React 18 SPA\nAxios + JWT] -->|HTTPS/JSON :3000 to :8080| API

    subgraph Backend [Spring Boot 3.2 backend]
        direction TB
        API[ITSMController\nREST /api/**]
        SEC[AuthInterceptor + JwtUtil\nBCrypt]
        EXC[GlobalExceptionHandler]
        STR[Strategy:\nSkillBased / LoadBased]
        FAC[IncidentFactory]
        LIF[IncidentLifecycleManager]
        PUB[IncidentEventPublisher]
        OBS[AuditObserver\nNotificationObserver]
        ESC[EscalationService\n@Scheduled 60s]
        REPO[Spring Data JPA Repositories]
        API --> SEC
        API --> STR
        API --> FAC
        API --> LIF
        API --> PUB --> OBS
        API --> REPO
        OBS --> REPO
        ESC --> REPO
        API --> EXC
    end

    REPO -->|JDBC| DB[(PostgreSQL 16)]
````

## 2. Layers
| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `frontend/src` | Role-based dashboards (Employee, Technician, Administrator) |
| API | `controller` | REST endpoints, business orchestration |
| Security | `security`, `config` | JWT check, role check, BCrypt bean |
| Domain / patterns | `strategy`, `factory`, `observer`, `lifecycle` | Assignment, creation, events, state rules |
| Services | `service` | Scheduled SLA escalation |
| Persistence | `repository`, `model` | JPA entities and repositories |
| Cross-cutting | `exception`, `dto` | Error handling, validated request/response objects |

## 3. Deployment

````mermaid
flowchart LR
    subgraph Docker Compose
        F[frontend: node:22 :3000]
        B[backend: temurin-17 :8080]
        D[(postgres:16 :5432)]
    end
    F --> B --> D
````

## 4. Create-Incident Sequence

````mermaid
sequenceDiagram
    actor E as Employee
    participant UI as React
    participant AI as AuthInterceptor
    participant C as ITSMController
    participant S as AssignmentStrategy
    participant DB as PostgreSQL
    participant P as EventPublisher
    E->>UI: Submit form
    UI->>AI: POST /api/incidents + Bearer token
    AI->>AI: Verify JWT, load user
    AI->>C: authUser attribute
    C->>C: Validate category, calculate priority, set SLA, detect duplicate
    C->>S: assignTechnician(incident, technicians)
    S-->>C: technician
    C->>DB: save incident, update technician load
    C->>P: publish INCIDENT_CREATED
    P->>DB: AuditObserver saves audit log
    P->>DB: NotificationObserver saves notifications
    C-->>UI: 200 incident JSON
````

## 5. Incident Lifecycle

````mermaid
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