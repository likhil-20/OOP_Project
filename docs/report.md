---
title: "I-ITSM: Intelligent IT Service Management System"
author: "Group 9"
date: "November 2026"
---

# 1. Problem Analysis

**Problem.** In a large organisation, IT incidents are reported by phone, chat and email. This leads to delayed response, wrong prioritisation, uneven technician workload, missed SLAs and no history.

**Stakeholders.**

| Stakeholder | Need |
|---|---|
| Employee | Report a problem quickly, see progress, give feedback |
| Technician | A clear queue, enough information, a fair workload |
| Administrator / IT manager | Control, SLA visibility, reports |
| Business | Critical services restored within agreed times |

**Existing challenges.** Delayed response, incorrect prioritisation, inefficient technician allocation, SLA violations, duplicate reports, incomplete history.

**Proposed solution.** I-ITSM is a web application (React, Spring Boot, PostgreSQL) that handles the full lifecycle Report → Classify → Prioritise → Assign → Investigate → Resolve → Verify → Close. It adds automation: AI-assisted triage (typed or spoken), automatic priority, SLA monitoring with warnings, auto-escalation, duplicate detection, suggested resolutions and multi-channel notifications.

**Assumptions.** Self-registered users are employees and need administrator approval. Notifications are simulated email and in-app, plus an optional webhook. Working hours are not modelled in SLAs. One incident has one affected service.

# 2. Requirements
See `requirements.md` (FR1 to FR24, NFR1 to NFR8).

# 3. Priority Algorithm and Rationale

Levels: HIGH = 3, MEDIUM = 2, LOW = 1. Affected users: fewer than 10 = 1, fewer than 100 = 2, otherwise 3.

1. Effective impact = round(average of incident impact, service business criticality, affected-users level).
2. Effective impact HIGH and urgency HIGH gives **P1**.
3. Either one HIGH gives **P2**.
4. Both LOW gives **P5**.
5. Both MEDIUM gives **P3**.
6. One MEDIUM and one LOW gives **P4**.

**Rationale.** It follows the ITIL impact × urgency matrix, but the impact also reflects how important the service is and how many people are affected. The rules are transparent, so users and auditors can see why a ticket got its priority. A rule-based method needs no training data.

# 4. SLA Policy (configurable by the administrator)

| Priority | Response | Resolution |
|---|---|---|
| P1 | 5 min | 30 min |
| P2 | 15 min | 2 h |
| P3 | 1 h | 8 h |
| P4 | 4 h | 24 h |
| P5 | 8 h | 3 days |

A background job warns the technician at 80% of a window and saves every violation. Missing a deadline also escalates the incident one priority level.

# 5. Intelligent Features
1. **AI triage copilot.** The description is analysed by Gemini, which suggests category, impact, urgency and service with a confidence score and a reason. The user can edit everything. If Gemini fails, keyword rules answer.
2. **Voice reporting.** The browser transcribes speech in English, Hindi or Telugu, then triage fills the form.
3. **Automatic escalation, SLA risk detection, duplicate detection, suggested resolutions.**
4. **Smart technician assignment.** Skill-based or load-based, switchable at runtime.

# 6. OOP Design
Encapsulation (private fields, write-only password), abstraction (service layer), inheritance (`ApiException` hierarchy), polymorphism and interfaces (`AssignmentStrategy`, `IncidentClassifier`, `IncidentObserver`), composition (services use repositories), association (User, Incident, ItService).
Patterns: **Strategy**, **Observer**, **Factory**, **Adapter**, **Repository**, State-style **lifecycle manager**. Diagrams are in `class-diagram.md` and `architecture.md`.

# 7. Security Considerations
- Passwords are hashed with BCrypt and never returned by the API.
- JWT (8 h) on every `/api/**` call; the user is re-checked in the database. `/api/admin/**` and `/api/analytics` are administrator-only. Incident access is checked per user.
- Failed logins: 5 attempts per email and IP lock the login for 15 minutes.
- Input is validated on the server (size limits, formats) and the frontend.
- AI: the API key lives only in an environment variable. User text is sent to the model as data; the answer is checked against our own lists, so prompt injection cannot create new categories or services.
- **Known limitations:** the token is stored in `localStorage` (XSS risk); no HTTPS in the Docker setup; the lockout is in memory; the default JWT secret and seeded passwords must be changed in production; Gemini and Chrome speech recognition send data to Google.

# 8. Testing
39 documented test cases in `backend/TEST_CASES.md`: lifecycle, login lockout, assignment strategies, validation, priority, duplicates, role rules, SLA monitoring, AI triage and fallback, and authorization.

# 9. Limitations and Future Work
- Simulated emails for admin-created accounts and password resets contain the temporary password in plain text (acceptable because email is simulated; a real deployment would send a one-time reset link).
- data.sql is skipped by Spring Boot for non-embedded databases; DataInitializer is the real seeder. Lombok is declared but unused.
- Timestamps are stored without a time zone; the backend runs in Asia/Kolkata (TZ set in docker-compose.yml).
- Test coverage does not yet include AdminService, EscalationService, the controller or frontend tests.

See `technology-report.md`.
