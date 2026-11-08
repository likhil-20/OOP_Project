package com.itsm.service;

import com.itsm.model.Incident;
import com.itsm.model.User;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final List<String> FINISHED = List.of("RESOLVED", "VERIFIED", "CLOSED");

    @Autowired private IncidentRepository incidentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return m;
    }

    public List<Incident> getSlaViolations() {
        return incidentRepository.findOpenWithDeadlineBefore(FINISHED, LocalDateTime.now());
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncidents", incidentRepository.count());
        stats.put("openIncidents", incidentRepository.countByStatusNot("CLOSED"));
        stats.put("criticalIncidents", incidentRepository.countByPriority("P1"));
        stats.put("resolvedIncidents", incidentRepository.countByStatusIn(FINISHED));
        stats.put("slaViolations", incidentRepository.countOpenWithDeadlineBefore(FINISHED, LocalDateTime.now()));

        Map<Long, Long> activeByTech = toMap(incidentRepository.countOpenByTechnician(FINISHED));
        List<Map<String, Object>> techWorkload = userRepository.findByRole(User.UserRole.TECHNICIAN).stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", t.getName());
            m.put("active", activeByTech.getOrDefault(t.getId(), 0L));
            return m;
        }).collect(Collectors.toList());
        stats.put("technicianWorkload", techWorkload);

        Map<Long, Long> perService = toMap(incidentRepository.countByService());
        List<Map<String, Object>> serviceAnalysis = serviceRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("serviceName", s.getServiceName());
            m.put("count", perService.getOrDefault(s.getId(), 0L));
            return m;
        }).collect(Collectors.toList());
        stats.put("serviceAnalysis", serviceAnalysis);

        return stats;
    }

    public List<Map<String, Object>> getSlaRisks() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> risks = new ArrayList<>();

        for (Incident inc : incidentRepository.findOpenWithDeadlineBefore(FINISHED, now.plusMinutes(120))) {
            long minutesRemaining = Duration.between(now, inc.getSlaResolutionDeadline()).toMinutes();
            Map<String, Object> m = new HashMap<>();
            m.put("id", inc.getId());
            m.put("description", inc.getDescription());
            m.put("priority", inc.getPriority());
            m.put("status", inc.getStatus());
            m.put("deadline", inc.getSlaResolutionDeadline().toString());
            m.put("minutesRemaining", minutesRemaining);
            m.put("riskLevel", minutesRemaining < 0 ? "VIOLATED" : (minutesRemaining < 30 ? "CRITICAL" : "HIGH_RISK"));
            risks.add(m);
        }
        return risks;
    }
}