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

    @Autowired private IncidentRepository incidentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;

    private static boolean isOpen(Incident i) {
        return !i.getStatus().equals("CLOSED") && !i.getStatus().equals("RESOLVED");
    }

    public List<Incident> getSlaViolations() {
        LocalDateTime now = LocalDateTime.now();
        return incidentRepository.findAll().stream()
            .filter(AnalyticsService::isOpen)
            .filter(i -> i.getSlaResolutionDeadline() != null && now.isAfter(i.getSlaResolutionDeadline()))
            .collect(Collectors.toList());
    }

    public Map<String, Object> getAnalytics() {
        List<Incident> all = incidentRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncidents", (long) all.size());
        stats.put("openIncidents", all.stream().filter(i -> !i.getStatus().equals("CLOSED")).count());
        stats.put("criticalIncidents", all.stream().filter(i -> "P1".equals(i.getPriority())).count());
        stats.put("resolvedIncidents", all.stream().filter(i ->
            i.getStatus().equals("RESOLVED") || i.getStatus().equals("VERIFIED") || i.getStatus().equals("CLOSED")).count());
        stats.put("slaViolations", (long) getSlaViolations().size());

        List<Map<String, Object>> techWorkload = userRepository.findByRole(User.UserRole.TECHNICIAN).stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", t.getName());
            m.put("active", incidentRepository.findByAssignedTechnicianId(t.getId()).stream()
                .filter(AnalyticsService::isOpen).count());
            return m;
        }).collect(Collectors.toList());
        stats.put("technicianWorkload", techWorkload);

        List<Map<String, Object>> serviceAnalysis = serviceRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("serviceName", s.getServiceName());
            m.put("count", all.stream().filter(i ->
                i.getAffectedService() != null && i.getAffectedService().getId().equals(s.getId())).count());
            return m;
        }).collect(Collectors.toList());
        stats.put("serviceAnalysis", serviceAnalysis);

        return stats;
    }

    public List<Map<String, Object>> getSlaRisks() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> risks = new ArrayList<>();

        for (Incident inc : incidentRepository.findAll()) {
            if (!isOpen(inc) || inc.getSlaResolutionDeadline() == null) continue;

            long minutesRemaining = Duration.between(now, inc.getSlaResolutionDeadline()).toMinutes();
            if (minutesRemaining < 120) {
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
        }
        return risks;
    }
}