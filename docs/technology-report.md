# Technology Report

| Area | Technology | Version | Why it was chosen |
|---|---|---|---|
| Frontend | React | 18.2 | Component-based UI, state hooks for role dashboards |
| Frontend build | react-scripts (CRA) | 5.0.1 | Zero-config dev server and build |
| HTTP client | Axios | 1.x | Interceptors for the JWT header and 401 handling |
| Voice input | Web Speech API (browser) | n/a | No library or cost; supports English, Hindi, Telugu |
| Backend | Spring Boot (Web) | 3.2.5 | REST controllers, dependency injection, scheduling, async |
| Language | Java | 17 | OOP features: records, interfaces, enums |
| Persistence | Spring Data JPA / Hibernate | via Boot | Entity mapping, `ddl-auto: update` |
| Database | PostgreSQL | 18 | Reliable relational store, text columns, SQL intervals |
| Validation | Jakarta Bean Validation | via Boot | Declarative DTO and entity validation |
| Password hashing | spring-security-crypto (BCrypt) | via Boot | Hashing without the full Spring Security stack |
| Authentication | jjwt | 0.12.5 | Stateless signed tokens (8 h expiry) |
| AI classification | Google Gemini API (REST, `java.net.http`) | configurable | Understands free text in several languages, returns structured JSON |
| Alerts | Slack / Discord incoming webhook | n/a | P1 alerts reach the on-call channel without polling the app |
| Containers | Docker, Docker Compose | n/a | Same setup on every machine |
| Build tool | Maven | 3.9 | Dependency and build management |
| Hosted DB (shared) | Neon PostgreSQL | n/a | Team-shared cloud database via `.env` |

## Answers to the Technology Selection questions
1. **Language (Java 17).** Strongly typed OOP with interfaces, records and enums, which this course is about. Mature tooling.
2. **Frontend (React).** Each role gets its own dashboard built from small components. Large ecosystem.
3. **Backend (Spring Boot).** Dependency injection, validation, scheduling and JPA come ready-made, so the code stays focused on the domain.
4. **Database (PostgreSQL).** Incidents, users and SLA policies are relational with clear foreign keys. ACID transactions keep technician load and incident status consistent.
5. **Architecture.** Layered: controller, service, repository, domain. Controllers contain no business rules, so each layer can be tested alone.
6. **Alternatives considered.** Node.js + MongoDB (weaker typing and relations), Django (less OOP-focused), microservices (too heavy for the team size and timeline), Server-Sent Events/WebSockets (planned, polling is simpler for now).
7. **Advantages and limitations.** Advantages: type safety, clear layering, easy Docker setup. Limitations: JVM memory use, CRA is no longer actively developed, polling instead of push, in-memory login lockout resets on restart.
8. **Support for OOP.** Strategy (technician assignment), Observer (audit, notification, email, webhook), Factory (incident creation), Adapter (`GeminiClassifier` hides the external API behind `IncidentClassifier`), State-style lifecycle (`IncidentLifecycleManager`), Repository, inheritance in the exception hierarchy, records for immutable DTOs.
9. **Growth.** Move the lockout cache and sessions to Redis; replace polling with WebSockets; run several backend instances behind a load balancer; add database indexes (already on status, priority, reporter, technician) and read replicas; move notifications to a message queue.

## Key Technical Decisions
1. **Stateless JWT instead of sessions.** The server still re-checks the user in the database on each request (cached for 10 s), so deleted or unapproved users lose access quickly.
2. **BCrypt with transparent upgrade.** Old plain-text passwords are accepted once and re-saved as hashes.
3. **Design patterns for extensibility.** A new classifier, assignment rule or notification channel is a new class, not a change to existing ones.
4. **AI with a safe fallback.** Gemini output is validated against our own category and service lists. If Gemini fails, a keyword classifier answers and the form still works.
5. **Centralised error handling.** `GlobalExceptionHandler` returns `{"error": "..."}` for every error.
6. **Background monitoring.** `SlaMonitorService` (30 s) warns and records violations; `EscalationService` (60 s) raises priority.
7. **Multi-factor priority.** Impact, service criticality, affected users and urgency (see `report.md`).

## Limitations and Future Work
- Duplicate detection is an exact case-insensitive description match with the same category, service and priority; text similarity would catch rephrased reports.
- Frontend polls (15 s, Mission Control 5 s); WebSockets would be more efficient.
- Suggested resolutions use keyword matching; embeddings could improve them.
- Voice recognition in Chrome sends audio to Google's servers; the Gemini call sends the incident text to Google.
- Escalation only raises priority; a full Technician → Team Lead → Manager chain is future work.