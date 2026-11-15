# I-ITSM Test Cases

Run all tests: `cd i-itsm-project/backend` then `mvn test`

| ID | Class | What is tested | Expected result |
|----|-------|----------------|-----------------|
| TC-01 | IncidentLifecycleManagerTest | Full lifecycle NEW -> CLOSED, one step at a time | Every step allowed |
| TC-02 | IncidentLifecycleManagerTest | Skipping/going backwards (NEW -> RESOLVED etc.) | Rejected |
| TC-03 | IncidentLifecycleManagerTest | Moving a CLOSED incident anywhere | Rejected |
| TC-04 | IncidentLifecycleManagerTest | Null / unknown status; lowercase input | Null/unknown rejected, lowercase accepted |
| TC-05 | LoginAttemptServiceTest | Failures below the limit | Not locked |
| TC-06 | LoginAttemptServiceTest | Failures reach the limit | Locked (429) |
| TC-07 | LoginAttemptServiceTest | Successful login after some failures | Counter reset, not locked |
| TC-08 | LoginAttemptServiceTest | Lock duration has passed | Lock removed |
| TC-09 | LoginAttemptServiceTest | Two different emails | Locking one does not lock the other |
| TC-10 | AuthServiceTest | 3 wrong passwords then the correct one | Locked, correct password also refused |
| TC-11 | AuthServiceTest | Correct credentials | Token and role returned |
| TC-12 | AuthServiceTest | Unknown email | Generic "Invalid email or password." |
| TC-13 | AssignmentStrategyTest | Load-based assignment | Technician with lowest load chosen |
| TC-14 | AssignmentStrategyTest | Skill-based assignment | Technician with matching skill chosen |
| TC-15 | AssignmentStrategyTest | Skill-based, no matching skill | First technician chosen |
| TC-16 | DtoValidationTest | Login email over 150 characters | Validation error |
| TC-17 | DtoValidationTest | Login password over 100 characters | Validation error |
| TC-18 | DtoValidationTest | Incident description over 2000 characters | Validation error |
| TC-19 | DtoValidationTest | Correct incident request | No validation errors |
| TC-20 | IncidentServiceTest | HIGH impact/urgency, critical service, 200 users | Priority P1 |
| TC-21 | IncidentServiceTest | LOW impact/urgency, 1 user | Priority P4 |
| TC-22 | IncidentServiceTest | Same description/category/service/priority as open incident | Marked duplicate of that incident |
| TC-23 | IncidentServiceTest | Employee sets IN_PROGRESS | Forbidden (403) |
| TC-24 | IncidentServiceTest | Resolve without resolution details | Bad request (400) |
| TC-25 | IncidentServiceTest | Resolve with details | Status RESOLVED, technician load minus 1 |
| TC-26 | AuthServiceTest | Account locked for one IP, login from a different IP | Other IP can still log in |
| TC-27 | DtoValidationTest | Affected users 10001 vs 10000 | 10001 rejected, 10000 accepted |
| TC-28 | SlaMonitorServiceTest | Unacknowledged incident past response deadline, monitor runs twice | One violation saved |
| TC-29 | SlaMonitorServiceTest | Incident at 80% of response window | Warning sent, no violation |
| TC-30 | TriageTest | Wi-Fi outage text, keyword classifier | Network, HIGH impact |
| TC-31 | TriageTest | Meaningless text, keyword classifier | Others, low confidence |
| TC-32 | TriageTest | Gemini throws an error | Keyword fallback used |
| TC-33 | TriageTest | Same text analysed twice | Gemini called once (cache) |
| TC-34 | AuthInterceptorTest | Request without token | 401 |
| TC-35 | AuthInterceptorTest | Employee calls /api/admin/users | 403 |
| TC-36 | AuthInterceptorTest | Administrator calls /api/admin/users | Allowed |
| TC-37 | AssignmentStrategyTest | Deadline-aware assignment, two skilled technicians | Technician with the furthest nearest-deadline chosen |