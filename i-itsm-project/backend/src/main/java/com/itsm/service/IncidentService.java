package com.itsm.service;

import com.itsm.dto.IncidentRequest;
import com.itsm.exception.*;
import com.itsm.factory.IncidentFactory;
import com.itsm.lifecycle.IncidentLifecycleManager;
import com.itsm.model.*;
import com.itsm.observer.IncidentEvent;
import com.itsm.observer.IncidentEventPublisher;
import com.itsm.repository.*;
import com.itsm.strategy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private SystemConfigurationRepository configRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private IncidentEventPublisher eventPublisher;

    // ---------- Who can see what ----------

    // Admin: everything. Employee: only their own tickets. Technician: only tickets assigned to them.
    public boolean canAccess(User me, Incident inc) {
        if (me.getRole() == User.UserRole.ADMINISTRATOR) return true;
        if (me.getRole() == User.UserRole.EMPLOYEE) {
            return inc.getReporter() != null && inc.getReporter().getId().equals(me.getId());
        }
        if (me.getRole() == User.UserRole.TECHNICIAN) {
            return inc.getAssignedTechnician() != null && inc.getAssignedTechnician().getId().equals(me.getId());
        }
        return false;
    }

    public List<Incident> getIncidentsFor(User me) {
        if (me.getRole() == User.UserRole.EMPLOYEE) {
            return incidentRepository.findByReporterId(me.getId());
        } else if (me.getRole() == User.UserRole.TECHNICIAN) {
            return incidentRepository.findByAssignedTechnicianId(me.getId());
        }
        return incidentRepository.findAll();
    }

    public List<AuditLog> getAuditLogs(Long incidentId, User me) {
        Incident inc = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident not found."));
        if (!canAccess(me, inc)) {
            throw new ForbiddenException("You are not allowed to view this incident's history.");
        }
        return auditRepository.findByIncidentId(incidentId);
    }

    // ---------- Create incident ----------

    @Transactional
    public Incident reportIncident(IncidentRequest req, User reporter) {
        String category = req.category().trim();
        if (!"OTHERS".equalsIgnoreCase(category) && !categoryRepository.existsByNameIgnoreCase(category)) {
            throw new BadRequestException("Unknown category: " + category);
        }

        String impact = req.impact() != null ? req.impact() : "MEDIUM";
        String urgency = req.urgency() != null ? req.urgency() : "MEDIUM";
        Incident inc = IncidentFactory.createIncident(
            reporter, category.toUpperCase(), req.description().trim(), impact, urgency);

        if (req.serviceId() != null) {
            ItService service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> new BadRequestException("Selected IT service does not exist."));
            inc.setAffectedService(service);
        }

        inc.setAffectedUsers(req.affectedUsers() != null ? req.affectedUsers() : 1);

        String criticality = inc.getAffectedService() != null
            ? inc.getAffectedService().getBusinessCriticality()
            : "MEDIUM";
        String priority = calculatePriority(inc.getImpact(), inc.getUrgency(), criticality, inc.getAffectedUsers());
        inc.setPriority(priority);

        applySlaDeadlines(inc, priority);
        markDuplicateIfAny(inc);
        assignTechnician(inc);

        Incident saved = incidentRepository.save(inc);

        eventPublisher.publish(new IncidentEvent(saved, "INCIDENT_CREATED", reporter.getEmail(),
            "NEW", saved.getStatus(), "Created incident with priority " + priority));

        return saved;
    }

    private void applySlaDeadlines(Incident inc, String priority) {
        SlaPolicy sla = slaPolicyRepository.findByPriority(priority).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (sla != null) {
            inc.setSlaResponseDeadline(now.plusMinutes(sla.getResponseMinutes()));
            inc.setSlaResolutionDeadline(now.plusMinutes(sla.getResolutionMinutes()));
        } else {
            inc.setSlaResponseDeadline(now.plusHours(1));
            inc.setSlaResolutionDeadline(now.plusHours(8));
        }
    }

    private void markDuplicateIfAny(Incident inc) {
        if (inc.getAffectedService() == null || inc.getPriority() == null) return;
        for (Incident active : incidentRepository.findDuplicateCandidates(
                inc.getCategory(), inc.getPriority(), inc.getAffectedService().getId())) {
            if (isDuplicateOf(inc, active)) {
                inc.setDuplicate(true);
                inc.setDuplicateOfIncidentId(active.getId());
                break;
            }
        }
    }

    private void assignTechnician(Incident inc) {
        String strategyType = configRepository.findByConfigKey("ASSIGNMENT_STRATEGY")
            .map(SystemConfiguration::getConfigValue).orElse("SKILL_BASED");

        AssignmentStrategy strategy = "LOAD_BASED".equals(strategyType)
            ? new LoadBasedAssignment()
            : new SkillBasedAssignment();

        List<User> techs = userRepository.findByRole(User.UserRole.TECHNICIAN);
        User assignedTech = strategy.assignTechnician(inc, techs);

        if (assignedTech != null) {
            inc.setAssignedTechnician(assignedTech);
            inc.setStatus("ASSIGNED");
            assignedTech.setCurrentLoad(assignedTech.getCurrentLoad() + 1);
            userRepository.save(assignedTech);
        }
    }

    // ---------- Priority calculation ----------

    private static int level(String v) {
        if ("HIGH".equals(v)) return 3;
        if ("LOW".equals(v)) return 1;
        return 2;
    }

    private static int usersLevel(Integer users) {
        if (users == null || users < 10) return 1;
        if (users < 100) return 2;
        return 3;
    }

    private static String calculatePriority(String impact, String urgency,
                                            String criticality, Integer affectedUsers) {
        int effectiveImpact = (int) Math.round(
            (level(impact) + level(criticality) + usersLevel(affectedUsers)) / 3.0);
        int urg = level(urgency);

        if (effectiveImpact == 3 && urg == 3) return "P1";
        if (effectiveImpact == 3 || urg == 3) return "P2";
        if (effectiveImpact == 1 && urg == 1) return "P4";
        return "P3";
    }

    // ---------- Duplicate detection ----------

    // Duplicate = same description (ignoring upper/lower case) AND same priority
    private static boolean isDuplicateOf(Incident a, Incident b) {
        if (a.getDescription() == null || b.getDescription() == null) return false;
        if (a.getPriority() == null || b.getPriority() == null) return false;

        boolean sameDescription = a.getDescription().trim().equalsIgnoreCase(b.getDescription().trim());
        boolean samePriority = a.getPriority().equals(b.getPriority());

        if (a.getCategory() == null || !a.getCategory().equalsIgnoreCase(b.getCategory())) return false;
        if (a.getAffectedService() == null || b.getAffectedService() == null
                || !a.getAffectedService().getId().equals(b.getAffectedService().getId())) return false;

        return sameDescription && samePriority;
    }

    // ---------- Suggested resolutions ----------

    public List<Map<String, Object>> getSuggestedResolutions(String category, String description) {
        List<Incident> resolvedIncidents = incidentRepository.findTop200ByResolutionDetailsIsNotNullOrderByIdDesc().stream()
            .filter(i -> i.getResolutionDetails() != null && !i.getResolutionDetails().isEmpty())
            .collect(Collectors.toList());

        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Incident inc : resolvedIncidents) {
            boolean matchCategory = category != null && inc.getCategory() != null
                && inc.getCategory().equalsIgnoreCase(category);
            boolean matchKeyword = false;
            if (description != null && inc.getDescription() != null) {
                for (String w : description.toLowerCase().split("\\s+")) {
                    if (w.length() > 3 && inc.getDescription().toLowerCase().contains(w)) {
                        matchKeyword = true;
                        break;
                    }
                }
            }
            if (matchCategory || matchKeyword) {
                Map<String, Object> m = new HashMap<>();
                m.put("matchedIncidentId", inc.getId());
                m.put("pastDescription", inc.getDescription());
                m.put("category", inc.getCategory());
                m.put("resolutionDetails", inc.getResolutionDetails());
                if (suggestions.size() < 5) suggestions.add(m);
            }
        }
        return suggestions;
    }

        // ---------- Update status ----------

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final List<String> TEXT_FIELDS = List.of(
        "investigationNotes", "resolutionDetails", "additionalInfo", "techRequestInfo", "feedbackComments");

    private static String asText(Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isKnownStatus(String status) {
        try {
            IncidentLifecycleManager.State.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional
    public Incident updateStatus(Long id, Map<String, Object> payload, User me) {
        Incident inc = incidentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Incident not found."));

        if (!canAccess(me, inc)) {
            throw new ForbiddenException("You are not allowed to modify this incident.");
        }

        String newStatus = asText(payload.get("status"));
        if (isBlank(newStatus)) throw new BadRequestException("Status is required.");
        newStatus = newStatus.trim().toUpperCase();
        if (!isKnownStatus(newStatus)) throw new BadRequestException("Unknown status: " + newStatus);

        // Role rules
        boolean sameStatus = newStatus.equals(inc.getStatus());
        if (me.getRole() == User.UserRole.EMPLOYEE && !sameStatus && !"VERIFIED".equals(newStatus)) {
            throw new ForbiddenException("Employees can only verify a resolved incident.");
        }
        if (me.getRole() == User.UserRole.TECHNICIAN && !sameStatus
                && ("VERIFIED".equals(newStatus) || "CLOSED".equals(newStatus))) {
            throw new ForbiddenException("Technicians cannot verify or close incidents.");
        }

        for (String field : TEXT_FIELDS) {
            String value = asText(payload.get(field));
            if (value != null && value.length() > MAX_TEXT_LENGTH) {
                throw new BadRequestException(field + " must be at most " + MAX_TEXT_LENGTH + " characters.");
            }
        }

        if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(inc.getStatus())
                && isBlank(asText(payload.get("resolutionDetails")))) {
            throw new BadRequestException("Resolution details are required to resolve an incident.");
        }

        Integer rating = null;
        if (payload.containsKey("feedbackRating") && payload.get("feedbackRating") != null) {
            try {
                rating = Integer.valueOf(payload.get("feedbackRating").toString().trim());
            } catch (NumberFormatException e) {
                throw new BadRequestException("Feedback rating must be a number from 1 to 5.");
            }
            if (rating < 1 || rating > 5) {
                throw new BadRequestException("Feedback rating must be a number from 1 to 5.");
            }
        }

        String oldStatus = inc.getStatus();
        if (!IncidentLifecycleManager.isValidTransition(oldStatus, newStatus)) {
            throw new InvalidStateTransitionException(oldStatus, newStatus);
        }

        // Apply changes
        inc.setStatus(newStatus);
                if (!newStatus.equals(oldStatus) && inc.getAcknowledgedAt() == null
                && ("ACKNOWLEDGED".equals(newStatus) || "IN_PROGRESS".equals(newStatus) || "RESOLVED".equals(newStatus))) {
            inc.setAcknowledgedAt(LocalDateTime.now());
        }
        if (payload.containsKey("investigationNotes")) {
            inc.setInvestigationNotes(asText(payload.get("investigationNotes")));
        }
        if (payload.containsKey("resolutionDetails")) {
            inc.setResolutionDetails(asText(payload.get("resolutionDetails")));
            if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(oldStatus) && inc.getAssignedTechnician() != null) {
                User t = inc.getAssignedTechnician();
                t.setCurrentLoad(Math.max(0, t.getCurrentLoad() - 1));
                userRepository.save(t);
            }
        }
        if (payload.containsKey("additionalInfo")) {
            inc.setAdditionalInfo(asText(payload.get("additionalInfo")));
        }
        if (payload.containsKey("techRequestInfo")) {
            inc.setTechRequestInfo(asText(payload.get("techRequestInfo")));
        }
        if (rating != null) {
            inc.setFeedbackRating(rating);
            inc.setFeedbackComments(asText(payload.get("feedbackComments")));
        }

        Incident updated = incidentRepository.save(inc);

        if (oldStatus.equals(newStatus)) {
            boolean infoExchange = payload.containsKey("techRequestInfo") || payload.containsKey("additionalInfo");
            String type = infoExchange ? "INFO_UPDATE" : "NOTES_UPDATE";
            String desc = infoExchange
                ? (payload.containsKey("techRequestInfo")
                    ? "Technician requested additional information"
                    : "Employee provided additional information")
                : "Investigation notes updated";
                eventPublisher.publish(new IncidentEvent(updated, type, me.getEmail(), oldStatus, newStatus, desc));
        } 
        else {
            eventPublisher.publish(new IncidentEvent(updated, "STATUS_UPDATE", me.getEmail(),
                oldStatus, newStatus, "Updated status to " + newStatus));
        }

        return updated;
    }
}