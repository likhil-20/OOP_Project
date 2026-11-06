# Use Case Diagram

```mermaid
flowchart LR
    EMP([Employee])
    TECH([Technician])
    ADM([Administrator])
    SYS([System Scheduler])

    subgraph I-ITSM
        UC1(Register / Login)
        UC2(Report Incident)
        UC3(View Own Incidents)
        UC4(Respond to Info Request)
        UC5(Verify and Rate Resolution)
        UC6(Acknowledge / Start Progress)
        UC7(Add Investigation Notes)
        UC8(Request Info from Employee)
        UC9(Resolve Incident)
        UC10(View Audit Trail)
        UC11(View Notifications)
        UC12(Manage Users - Approve/Delete)
        UC13(Manage Categories and Services)
        UC14(Configure SLA and Assignment Strategy)
        UC15(View Analytics and Reports)
        UC16(Monitor SLA Violations and Risks)
        UC17(Close Incident)
        UC18(Auto-Escalate Breached Incident)
        UC19(Get Suggested Resolutions)
    end

    EMP --- UC1 & UC2 & UC3 & UC4 & UC5 & UC10 & UC11 & UC19
    TECH --- UC1 & UC6 & UC7 & UC8 & UC9 & UC10 & UC11
    ADM --- UC1 & UC12 & UC13 & UC14 & UC15 & UC16 & UC17 & UC10 & UC11
    SYS --- UC18