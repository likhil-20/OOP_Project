   package com.itsm.service;

   import com.itsm.model.AuditLog;
   import com.itsm.model.Incident;
   import com.itsm.model.Notification;
   import com.itsm.model.User;
   import com.itsm.repository.AuditLogRepository;
   import com.itsm.repository.IncidentRepository;
   import com.itsm.repository.NotificationRepository;
   import com.itsm.repository.UserRepository;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.scheduling.annotation.Scheduled;
   import org.springframework.stereotype.Service;

   import java.time.LocalDateTime;
   import java.util.Set;

   @Service
   public class EscalationService {

       private static final Set<String> FINISHED = Set.of("RESOLVED", "VERIFIED", "CLOSED");
       private static final Set<String> NOT_PICKED_UP = Set.of("NEW", "CLASSIFIED", "ASSIGNED");

       @Autowired private IncidentRepository incidentRepository;
       @Autowired private UserRepository userRepository;
       @Autowired private NotificationRepository notificationRepository;
       @Autowired private AuditLogRepository auditRepository;

       // Runs by itself every 60 seconds
       @Scheduled(fixedDelay = 60000, initialDelay = 30000)
       public void checkAndEscalate() {
           LocalDateTime now = LocalDateTime.now();
           for (Incident inc : incidentRepository.findAll()) {
               if (inc.getStatus() == null || FINISHED.contains(inc.getStatus())) continue;
               if (Boolean.TRUE.equals(inc.getEscalated())) continue;

               String reason = null;
               if (NOT_PICKED_UP.contains(inc.getStatus())
                       && inc.getSlaResponseDeadline() != null
                       && now.isAfter(inc.getSlaResponseDeadline())) {
                   reason = "Nobody acknowledged it before the response deadline";
               } else if (inc.getSlaResolutionDeadline() != null
                       && now.isAfter(inc.getSlaResolutionDeadline())) {
                   reason = "It was not resolved before the resolution deadline";
               }

               if (reason != null) escalate(inc, reason);
           }
       }

       private void escalate(Incident inc, String reason) {
           String oldPriority = inc.getPriority();
           String newPriority = raise(oldPriority);

           inc.setPriority(newPriority);
           inc.setEscalated(true);
           inc.setEscalatedAt(LocalDateTime.now());
           incidentRepository.save(inc);

           AuditLog audit = new AuditLog();
           audit.setIncidentId(inc.getId());
           audit.setUserEmail("system");
           audit.setActionPerformed("AUTO_ESCALATION");
           audit.setPreviousState(inc.getStatus());
           audit.setNewState(inc.getStatus());
           audit.setDescription("Priority " + oldPriority + " -> " + newPriority + ". " + reason);
           auditRepository.save(audit);

           String message = "Incident #" + inc.getId() + " was ESCALATED to " + newPriority + ". " + reason + ".";
           if (inc.getReporter() != null) sendInApp(inc.getReporter().getEmail(), message);
           if (inc.getAssignedTechnician() != null) sendInApp(inc.getAssignedTechnician().getEmail(), message);
           for (User admin : userRepository.findByRole(User.UserRole.ADMINISTRATOR)) {
               sendInApp(admin.getEmail(), message);
           }

           System.out.println("[ESCALATION] " + message);
       }

       // One step more urgent. P1 stays P1.
       private String raise(String priority) {
           if ("P4".equals(priority)) return "P3";
           if ("P3".equals(priority)) return "P2";
           if ("P2".equals(priority)) return "P1";
           return priority == null ? "P3" : priority;
       }

       private void sendInApp(String email, String message) {
           if (email == null) return;
           Notification n = new Notification();
           n.setRecipientEmail(email);
           n.setMessage(message);
           notificationRepository.save(n);
       }
   }