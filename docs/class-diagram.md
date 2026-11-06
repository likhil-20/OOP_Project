````markdown
# Class Diagram

```mermaid

classDiagram
    class User {
        Long id
        String name
        String email
        String password
        UserRole role
        String skills
        int currentLoad
        boolean approved
    }
    class Incident {
        Long id
        String category
        String description
        String impact
        String urgency
        Integer affectedUsers
        String priority
        String status
        LocalDateTime slaResponseDeadline
        LocalDateTime slaResolutionDeadline
        Integer feedbackRating
        boolean isDuplicate
        Boolean escalated
    }
    class ItService { Long id; String serviceName; String businessCriticality }
    class IncidentCategory { Long id; String name; String description }
    class SlaPolicy { Long id; String priority; int responseMinutes; int resolutionMinutes }
    class SystemConfiguration { Long id; String configKey; String configValue }
    class AuditLog { Long id; Long incidentId; String actionPerformed; String previousState; String newState }
    class Notification { Long id; String recipientEmail; String message; boolean isRead }

    User "1" --> "*" Incident : reports
    User "1" --> "*" Incident : assigned to
    ItService "1" --> "*" Incident : affects

    class AssignmentStrategy { <<interface>> +assignTechnician(Incident, List~User~) User }
    class SkillBasedAssignment
    class LoadBasedAssignment
    AssignmentStrategy <|.. SkillBasedAssignment
    AssignmentStrategy <|.. LoadBasedAssignment

    class IncidentObserver { <<interface>> +update(IncidentEvent) }
    class AuditObserver
    class NotificationObserver
    class IncidentEventPublisher { +subscribe() +unsubscribe() +publish(IncidentEvent) }
    class IncidentEvent { <<record>> }
    IncidentObserver <|.. AuditObserver
    IncidentObserver <|.. NotificationObserver
    IncidentEventPublisher o--> IncidentObserver
    IncidentEventPublisher ..> IncidentEvent
    AuditObserver ..> AuditLog
    NotificationObserver ..> Notification

    class IncidentFactory { +createIncident(...) Incident$ }
    IncidentFactory ..> Incident

    class IncidentLifecycleManager { +isValidTransition(String, String) boolean$ }

    class ITSMController
    ITSMController ..> AssignmentStrategy
    ITSMController ..> IncidentFactory
    ITSMController ..> IncidentLifecycleManager
    ITSMController ..> IncidentEventPublisher
    ITSMController ..> JwtUtil

    class EscalationService { +checkAndEscalate() }
    EscalationService ..> Incident
    EscalationService ..> Notification
    EscalationService ..> AuditLog

    class AuthInterceptor { +preHandle() boolean }
    class JwtUtil { +generateToken() +parse() }
    AuthInterceptor ..> JwtUtil

    class ApiException { <<abstract>> HttpStatus status }
    ApiException <|-- BadRequestException
    ApiException <|-- UnauthorizedException
    ApiException <|-- ForbiddenException
    ApiException <|-- ConflictException
    ApiException <|-- ResourceNotFoundException
    BadRequestException <|-- InvalidStateTransitionException