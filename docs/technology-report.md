```markdown
# Technology Report

| Area | Technology | Version | Why it was chosen |
|---|---|---|---|
| Frontend | React | 18.2 | Component-based UI, state hooks for role dashboards |
| Frontend build | react-scripts (CRA) | 5.0.1 | Zero-config dev server and build |
| HTTP client | Axios | 1.x | Interceptors for the JWT header and 401 handling |
| Backend | Spring Boot (Web) | 3.2.5 | REST controllers, dependency injection, scheduling |
| Language | Java | 17 | OOP features: records, interfaces, enums |
| Persistence | Spring Data JPA / Hibernate | via Boot 3.2.5 | Entity mapping, `ddl-auto: update` |
| Database | PostgreSQL | 16 | Reliable relational store, text columns, SQL intervals |
| Validation | Spring Validation (Jakarta) | via Boot | Declarative DTO and entity validation |
| Password hashing | spring-security-crypto (BCrypt) | via Boot | Hashing without the full Spring Security stack |
| Authentication | jjwt | 0.12.5 | Stateless signed tokens (8 h expiry) |
| Containers | Docker, Docker Compose | n/a | Same setup on every machine |
| Build tool | Maven | 3.9 | Dependency and build management |
| Hosted DB (shared) | Neon PostgreSQL | n/a | Team-shared cloud database via `.env` |

## Key Technical Decisions
1. **Stateless JWT instead of sessions.** Scales and keeps the frontend simple. The server still re-checks the user in the database on each request, so deleted or unapproved users lose access instantly.
2. **BCrypt with transparent upgrade.** Old plain-text passwords are accepted once and re-saved as hashes.
3. **Design patterns for extensibility.** Strategy (assignment), Observer (audit and notification), Factory (creation).
4. **Centralised error handling.** `GlobalExceptionHandler` returns `{"error": "..."}` for all errors.
5. **Scheduled escalation.** `@Scheduled` job every 60 s handles SLA breaches without user action.
6. **Multi-factor priority.** Priority uses impact, service criticality, affected users and urgency.

## Limitations and Future Work
- Duplicate detection is a simple 5-character substring match; could use text similarity.
- Notifications are in-app only; add email or WebSocket push.
- Frontend polls every 10 s; WebSockets would be more efficient.
- Escalation scans all incidents each minute; use a filtered query for large data.
- Suggested resolutions use keyword matching; could use full-text search.