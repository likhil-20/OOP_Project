package com.itsm.service;

import com.itsm.model.AuditLog;
import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.model.SlaViolation;
import com.itsm.model.User;
import com.itsm.repository.AuditLogRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class SlaMonitorService {

    private static final Set<String> FINISHED = Set.of("RESOLVED", "VERIFIED", "CLOSED");
    private static final Set<String> NOT_PICKED_UP = Set.of("NEW", "CLASSIFIED", "ASSIGNED");
    private static final int WARNING_PERCENT = 80;

    @Autowired private IncidentRepository incidentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private SlaViolationRepository violationRepository;

    @Scheduled(fixedDelay = 30000, initialDelay = 20000)
    public void monitor() {
        LocalDateTime now = LocalDateTime.now();
        for (Incident inc : incidentRepository.findOpen(FINISHED)) {
            if (inc.getStatus() == null) continue;
            boolean changed = false;

            // ----- Response SLA -----
            boolean pickedUp = inc.getAcknowledgedAt() != null || !NOT_PICKED_UP.contains(inc.getStatus());
            LocalDateTime resp = inc.getSlaResponseDeadline();
            if (!pickedUp && resp != null && !Boolean.TRUE.equals(inc.getResponseBreached())) {
                if (now.isAfter(resp)) {
                    breach(inc, "RESPONSE", resp);
                    inc.setResponseBreached(true);
                    changed = true;
                } else if (!Boolean.TRUE.equals(inc.getResponseWarningSent())
                        && nearDeadline(inc.getCreatedAt(), resp, now)) {
                    warn(inc, "response", resp, now);
                    inc.setResponseWarningSent(true);
                    changed = true;
                }
            }

            // ----- Resolution SLA -----
            LocalDateTime res = inc.getSlaResolutionDeadline();
            if (res != null && !Boolean.TRUE.equals(inc.getResolutionBreached())) {
                if (now.isAfter(res)) {
                    breach(inc, "RESOLUTION", res);
                    inc.setResolutionBreached(true);
                    changed = true;
                } else if (!Boolean.TRUE.equals(inc.getResolutionWarningSent())
                        && nearDeadline(inc.getCreatedAt(), res, now)) {
                    warn(inc, "resolution", res, now);
                    inc.setResolutionWarningSent(true);
                    changed = true;
                }
            }

            if (changed) incidentRepository.save(inc);
        }
    }

    private boolean nearDeadline(LocalDateTime start, LocalDateTime deadline, LocalDateTime now) {
        if (start == null) return false;
        long total = Duration.between(start, deadline).toSeconds();
        if (total <= 0) return false;
        long used = Duration.between(start, now).toSeconds();
        return used * 100 >= total * WARNING_PERCENT;
    }

    private void breach(Incident inc, String type, LocalDateTime deadline) {
        SlaViolation v = new SlaViolation();
        v.setIncidentId(inc.getId());
        v.setType(type);
        v.setPriority(inc.getPriority());
        v.setDeadline(deadline);
        violationRepository.save(v);

        AuditLog audit = new AuditLog();
        audit.setIncidentId(inc.getId());
        audit.setUserEmail("system");
        audit.setActionPerformed("SLA_" + type + "_BREACH");
        audit.setPreviousState(inc.getStatus());
        audit.setNewState(inc.getStatus());
        audit.setDescription(type + " SLA deadline (" + deadline + ") was missed.");
        auditRepository.save(audit);

        String message = "SLA " + type.toLowerCase() + " BREACHED for Incident #" + inc.getId()
            + " (" + inc.getPriority() + ").";
        if (inc.getAssignedTechnician() != null) sendInApp(inc.getAssignedTechnician().getEmail(), message);
        for (User admin : userRepository.findByRole(User.UserRole.ADMINISTRATOR)) {
            sendInApp(admin.getEmail(), message);
        }
        System.out.println("[SLA] " + message);
    }

    private void warn(Incident inc, String type, LocalDateTime deadline, LocalDateTime now) {
        long minutes = Math.max(0, Duration.between(now, deadline).toMinutes());
        String message = "WARNING: Incident #" + inc.getId() + " (" + inc.getPriority() + ") "
            + type + " SLA deadline is in about " + minutes + " minute(s).";
        if (inc.getAssignedTechnician() != null) {
            sendInApp(inc.getAssignedTechnician().getEmail(), message);
        } else {
            for (User admin : userRepository.findByRole(User.UserRole.ADMINISTRATOR)) {
                sendInApp(admin.getEmail(), message);
            }
        }
    }

    private void sendInApp(String email, String message) {
        if (email == null) return;
        Notification n = new Notification();
        n.setRecipientEmail(email);
        n.setMessage(message);
        notificationRepository.save(n);
    }
}
