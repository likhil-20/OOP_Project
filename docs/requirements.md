# I-ITSM Requirements

## 1. Purpose
Intelligent IT Service Management system: employees report IT incidents, the system prioritises and routes them to technicians, tracks SLAs, and gives administrators control and analytics.

## 2. Actors
- **Employee**: reports incidents, answers info requests, verifies and rates resolutions.
- **Technician**: acknowledges, investigates, requests info, resolves assigned incidents.
- **Administrator**: manages users, categories, services, SLA and assignment strategy, and views analytics.
- **System (scheduler)**: auto-escalates breached incidents.

## 3. Functional Requirements
| ID | Requirement |
|---|---|
| FR1 | Users register; accounts stay pending until an administrator approves them. |
| FR2 | Users log in with email and password and receive a JWT valid for 8 hours. |
| FR3 | Employees create incidents (category, service, impact, urgency, affected users, description). |
| FR4 | The system calculates priority P1 to P4 from impact, service criticality, affected users and urgency. |
| FR5 | The system sets SLA response and resolution deadlines from the SLA policy of that priority. |
| FR6 | The system detects probable duplicate incidents and flags them. |
| FR7 | The system auto-assigns a technician using the configured strategy (skill-based or load-based). |
| FR8 | Incidents follow the lifecycle NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED; invalid transitions are rejected. |
| FR9 | Technicians add investigation notes, request information, and resolve with resolution details. |
| FR10 | Employees reply to info requests and verify and rate resolved incidents (1 to 5). |
| FR11 | Every action is recorded in an audit trail visible per incident. |
| FR12 | Users receive in-app notifications and can mark them read. |
| FR13 | The system auto-escalates incidents that miss the response or resolution deadline (priority raised one level; reporter, technician and admins notified). |
| FR14 | Administrators view analytics (totals, open, P1, resolved, SLA violations, technician workload, service analysis). |
| FR15 | Administrators view SLA violations and SLA risks (less than 2 hours remaining). |
| FR16 | Administrators manage users (approve, delete), categories, IT services, SLA policies and assignment strategy. |
| FR17 | The system suggests resolutions from similar past incidents while an incident is being typed. |
| FR18 | Search and filter incidents by keyword, priority, status, category, technician, service, date and SLA state. |

## 4. Non-Functional Requirements
| ID | Requirement |
|---|---|
| NFR1 | Security: BCrypt password hashing, JWT authentication, role-based access, per-incident ownership checks. |
| NFR2 | Passwords are never returned in API responses. |
| NFR3 | Consistent JSON error format `{"error": "..."}` via a global exception handler. |
| NFR4 | Input validation on all requests (Bean Validation). |
| NFR5 | Maintainability: Strategy, Observer and Factory design patterns; layered structure. |
| NFR6 | Portability: runs with Docker Compose (backend, frontend, PostgreSQL). |
| NFR7 | Dashboard data refreshes automatically every 10 seconds. |
| NFR8 | Strategy can be changed at runtime without downtime. |

## 5. Constraints and Assumptions
- Java 17, Spring Boot 3.2.5, PostgreSQL 16, React 18.
- Notifications are in-app only (no email or SMS).
- New self-registered users get the EMPLOYEE role.