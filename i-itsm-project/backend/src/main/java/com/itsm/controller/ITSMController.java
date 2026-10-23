package com.itsm.controller;
import com.itsm.model.*;
import com.itsm.repository.*;
import com.itsm.strategy.*;
import com.itsm.lifecycle.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ITSMController {

    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private SystemConfigurationRepository configRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setName("System Admin");
            admin.setPassword("admin123");
            admin.setRole(User.UserRole.ADMINISTRATOR);
            admin.setApproved(true);
            userRepository.save(admin);

            User tech = new User();
            tech.setEmail("tech.alice@gmail.com");
            tech.setName("Alice Tech");
            tech.setPassword("tech123");
            tech.setRole(User.UserRole.TECHNICIAN);
            tech.setSkills("APPLICATION,DATABASE,NETWORK,SECURITY,HARDWARE");
            tech.setApproved(true);
            userRepository.save(tech);

            User tech2 = new User();
            tech2.setEmail("tech.bob@gmail.com");
            tech2.setName("Bob Loadbalancer");
            tech2.setPassword("tech123");
            tech2.setRole(User.UserRole.TECHNICIAN);
            tech2.setSkills("SOFTWARE,CLOUD,STORAGE");
            tech2.setApproved(true);
            userRepository.save(tech2);

            User emp = new User();
            emp.setEmail("employee@gmail.com");
            emp.setName("John Employee");
            emp.setPassword("emp123");
            emp.setRole(User.UserRole.EMPLOYEE);
            emp.setApproved(true);
            userRepository.save(emp);
        }

        if (categoryRepository.count() == 0) {
            String[] defaultCategories = {"Hardware", "Software", "Network", "Database", "Cloud", "Security", "Authentication", "Application", "Storage"};
            for (String cat : defaultCategories) {
                if (!categoryRepository.existsByName(cat)) {
                    IncidentCategory c = new IncidentCategory();
                    c.setName(cat);
                    c.setDescription("Standard ITSM classification category");
                    categoryRepository.save(c);
                }
            }
        }

        if (serviceRepository.count() == 0) {
            ItService s1 = new ItService();
            s1.setServiceName("Payment Application");
            s1.setBusinessCriticality("HIGH");
            s1.setDescription("Core transaction processing");
            serviceRepository.save(s1);

            ItService s2 = new ItService();
            s2.setServiceName("Authentication Service");
            s2.setBusinessCriticality("HIGH");
            s2.setDescription("OAuth & SSO provider");
            serviceRepository.save(s2);

            ItService s3 = new ItService();
            s3.setServiceName("Internal HR Portal");
            s3.setBusinessCriticality("LOW");
            s3.setDescription("HR records portal");
            serviceRepository.save(s3);
        }

        if (slaPolicyRepository.count() == 0) {
            createSla("P1", 5, 30);
            createSla("P2", 15, 120);
            createSla("P3", 60, 480);
            createSla("P4", 240, 1440);
        }

        if (configRepository.count() == 0) {
            SystemConfiguration cfg = new SystemConfiguration();
            cfg.setConfigKey("ASSIGNMENT_STRATEGY");
            cfg.setConfigValue("SKILL_BASED");
            configRepository.save(cfg);
        }
    }

    private void createSla(String priority, int resp, int res) {
        SlaPolicy p = new SlaPolicy();
        p.setPriority(priority);
        p.setResponseMinutes(resp);
        p.setResolutionMinutes(res);
        slaPolicyRepository.save(p);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found. Please check email."));
        }

        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid password."));
        }
        if (!user.isApproved()) {
            return ResponseEntity.status(403).body(Map.of("error", "Your account is pending administrator approval."));
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            return ResponseEntity.status(400).body(Map.of("error", "Email already registered."));
        }
        newUser.setRole(User.UserRole.EMPLOYEE);
        newUser.setApproved(false);
        if (newUser.getPassword() == null) newUser.setPassword("password123");
        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Pending administrator approval."));
    }

    @GetMapping("/services")
    public List<ItService> getServicesPublic() {
        return serviceRepository.findAll();
    }

    @GetMapping("/categories")
    public List<IncidentCategory> getCategoriesPublic() {
        return categoryRepository.findAll();
    }

    @GetMapping("/admin/categories")
    public List<IncidentCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/admin/categories")
    public IncidentCategory saveCategory(@RequestBody IncidentCategory c) {
        return categoryRepository.save(c);
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/users")
    public List<User> getAllUsers() { return userRepository.findAll(); }

    @PutMapping("/admin/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setApproved(true);
        userRepository.save(u);
        return ResponseEntity.ok(u);
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/services")
    public List<ItService> getAllServices() {
        return serviceRepository.findAll();
    }

    @PostMapping("/admin/services")
    public ItService saveService(@RequestBody ItService s) {
        return serviceRepository.save(s);
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/incidents")
    public List<Incident> getAllIncidents(@RequestParam(required = false) String email, @RequestParam(required = false) String role) {
        if ("EMPLOYEE".equals(role)) {
            User u = userRepository.findByEmail(email).orElse(null);
            if (u != null) return incidentRepository.findByReporterId(u.getId());
        } else if ("TECHNICIAN".equals(role)) {
            User u = userRepository.findByEmail(email).orElse(null);
            if (u != null) return incidentRepository.findByAssignedTechnicianId(u.getId());
        }
        return incidentRepository.findAll();
    }

    @PostMapping("/incidents")
    public ResponseEntity<?> reportIncident(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            User reporter = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found: " + email));
            
            Incident inc = new Incident();
            inc.setReporter(reporter);
            String rawCat = (String) payload.get("category");
            inc.setCategory(rawCat != null ? rawCat.toUpperCase() : "HARDWARE");
            inc.setDescription((String) payload.get("description"));
            
            Object serviceIdObj = payload.get("serviceId");
            if (serviceIdObj != null && !serviceIdObj.toString().isEmpty()) {
                Long serviceId = Long.valueOf(serviceIdObj.toString());
                ItService service = serviceRepository.findById(serviceId).orElse(null);
                inc.setAffectedService(service);
            }
            
            String impact = (String) payload.get("impact");
            String urgency = (String) payload.get("urgency");
            inc.setImpact(impact != null ? impact : "MEDIUM");
            inc.setUrgency(urgency != null ? urgency : "MEDIUM");

            String priority = "P3";
            if ("HIGH".equals(inc.getImpact()) && "HIGH".equals(inc.getUrgency())) priority = "P1";
            else if ("HIGH".equals(inc.getImpact()) || "HIGH".equals(inc.getUrgency())) priority = "P2";
            else if ("LOW".equals(inc.getImpact()) && "LOW".equals(inc.getUrgency())) priority = "P4";
            inc.setPriority(priority);
            inc.setStatus("NEW");
            inc.setStatus("CLASSIFIED");

            SlaPolicy sla = slaPolicyRepository.findByPriority(priority).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            if (sla != null) {
                inc.setSlaResponseDeadline(now.plusMinutes(sla.getResponseMinutes()));
                inc.setSlaResolutionDeadline(now.plusMinutes(sla.getResolutionMinutes()));
            } else {
                inc.setSlaResponseDeadline(now.plusHours(1));
                inc.setSlaResolutionDeadline(now.plusHours(8));
            }

            List<Incident> activeIncidents = incidentRepository.findByStatusNot("CLOSED");
            for (Incident active : activeIncidents) {
                if (active.getDescription() != null && inc.getDescription() != null && inc.getDescription().length() >= 3) {
                    int len = Math.min(inc.getDescription().length(), 5);
                    String sub = inc.getDescription().toLowerCase().substring(0, len);
                    if (active.getDescription().toLowerCase().contains(sub)) {
                        inc.setDuplicate(true);
                        inc.setDuplicateOfIncidentId(active.getId());
                        break;
                    }
                }
            }

            String strategyType = configRepository.findByConfigKey("ASSIGNMENT_STRATEGY")
                .map(SystemConfiguration::getConfigValue).orElse("SKILL_BASED");
            
            List<User> techs = userRepository.findByRole(User.UserRole.TECHNICIAN);
            User assignedTech = null;
            
            AssignmentStrategy strategy;
            if ("LOAD_BASED".equals(strategyType)) {
                strategy = new LoadBasedAssignment();
            } else {
                strategy = new SkillBasedAssignment();
            }
            assignedTech = strategy.assignTechnician(inc, techs);

            if (assignedTech != null) {
                inc.setAssignedTechnician(assignedTech);
                inc.setStatus("ASSIGNED");
                assignedTech.setCurrentLoad(assignedTech.getCurrentLoad() + 1);
                userRepository.save(assignedTech);
            }

            Incident saved = incidentRepository.save(inc);

            Notification n = new Notification();
            n.setRecipientEmail(reporter.getEmail());
            n.setMessage("Incident #" + saved.getId() + " created (" + priority + ")");
            notificationRepository.save(n);

            if (assignedTech != null) {
                Notification tn = new Notification();
                tn.setRecipientEmail(assignedTech.getEmail());
                tn.setMessage("Assigned Incident #" + saved.getId() + " (" + priority + ")");
                notificationRepository.save(tn);
            }

            AuditLog audit = new AuditLog();
            audit.setIncidentId(saved.getId());
            audit.setUserEmail(reporter.getEmail());
            audit.setActionPerformed("INCIDENT_CREATED");
            audit.setPreviousState("NEW");
            audit.setNewState(saved.getStatus());
            audit.setDescription("Created incident with priority " + priority);
            auditRepository.save(audit);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/incidents/{id}/status")
    public ResponseEntity<?> updateIncidentStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Incident inc = incidentRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        String oldStatus = inc.getStatus();
        String newStatus = (String) payload.get("status");
        String userEmail = (String) payload.get("email");

        if (!IncidentLifecycleManager.isValidTransition(oldStatus, newStatus)) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid state transition from " + oldStatus + " to " + newStatus));
        }

        inc.setStatus(newStatus);
        if (payload.containsKey("investigationNotes")) {
            inc.setInvestigationNotes((String) payload.get("investigationNotes"));
        }
        if (payload.containsKey("resolutionDetails")) {
            inc.setResolutionDetails((String) payload.get("resolutionDetails"));
            if ("RESOLVED".equals(newStatus) && inc.getAssignedTechnician() != null) {
                User t = inc.getAssignedTechnician();
                t.setCurrentLoad(Math.max(0, t.getCurrentLoad() - 1));
                userRepository.save(t);
            }
        }
        if (payload.containsKey("additionalInfo")) {
            inc.setAdditionalInfo((String) payload.get("additionalInfo"));
        }
        if (payload.containsKey("techRequestInfo")) {
            inc.setTechRequestInfo((String) payload.get("techRequestInfo"));
        }
        if (payload.containsKey("feedbackRating")) {
            inc.setFeedbackRating(Integer.valueOf(payload.get("feedbackRating").toString()));
            inc.setFeedbackComments((String) payload.get("feedbackComments"));
        }

        Incident updated = incidentRepository.save(inc);

        AuditLog audit = new AuditLog();
        audit.setIncidentId(updated.getId());
        audit.setUserEmail(userEmail);
        audit.setActionPerformed("STATUS_UPDATE");
        audit.setPreviousState(oldStatus);
        audit.setNewState(newStatus);
        audit.setDescription("Updated status to " + newStatus);
        auditRepository.save(audit);

        if (inc.getReporter() != null) {
            Notification n = new Notification();
            n.setRecipientEmail(inc.getReporter().getEmail());
            n.setMessage("Incident #" + inc.getId() + " status: " + newStatus);
            notificationRepository.save(n);
        }

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/admin/sla")
    public List<SlaPolicy> getAllSla() { return slaPolicyRepository.findAll(); }

    @PostMapping("/admin/sla")
    public SlaPolicy saveSla(@RequestBody SlaPolicy sla) { return slaPolicyRepository.save(sla); }

    @GetMapping("/admin/config")
    public List<SystemConfiguration> getAllConfig() { return configRepository.findAll(); }

    @PostMapping("/admin/config")
    public SystemConfiguration saveConfig(@RequestBody SystemConfiguration cfg) {
        SystemConfiguration existing = configRepository.findByConfigKey(cfg.getConfigKey()).orElse(new SystemConfiguration());
        existing.setConfigKey(cfg.getConfigKey());
        existing.setConfigValue(cfg.getConfigValue());
        return configRepository.save(existing);
    }

    @GetMapping("/admin/sla-violations")
    public List<Incident> getSlaViolations() {
        LocalDateTime now = LocalDateTime.now();
        return incidentRepository.findAll().stream()
            .filter(i -> !i.getStatus().equals("CLOSED") && !i.getStatus().equals("RESOLVED"))
            .filter(i -> i.getSlaResolutionDeadline() != null && now.isAfter(i.getSlaResolutionDeadline()))
            .collect(Collectors.toList());
    }

    @GetMapping("/notifications")
    public List<Notification> getNotifications(@RequestParam String email) {
        return notificationRepository.findByRecipientEmail(email);
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit/{incidentId}")
    public List<AuditLog> getAuditLogs(@PathVariable Long incidentId) {
        return auditRepository.findByIncidentId(incidentId);
    }

    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics() {
        List<Incident> all = incidentRepository.findAll();
        long total = all.size();
        long open = all.stream().filter(i -> !i.getStatus().equals("CLOSED")).count();
        long critical = all.stream().filter(i -> "P1".equals(i.getPriority())).count();
        long resolved = all.stream().filter(i -> i.getStatus().equals("RESOLVED") || i.getStatus().equals("VERIFIED") || i.getStatus().equals("CLOSED")).count();
        long slaViolations = getSlaViolations().size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncidents", total);
        stats.put("openIncidents", open);
        stats.put("criticalIncidents", critical);
        stats.put("resolvedIncidents", resolved);
        stats.put("slaViolations", slaViolations);

        List<Map<String, Object>> techWorkload = userRepository.findByRole(User.UserRole.TECHNICIAN).stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", t.getName());
            m.put("active", incidentRepository.findByAssignedTechnicianId(t.getId()).stream().filter(i -> !i.getStatus().equals("CLOSED") && !i.getStatus().equals("RESOLVED")).count());
            return m;
        }).collect(Collectors.toList());
        stats.put("technicianWorkload", techWorkload);

        List<Map<String, Object>> serviceAnalysis = serviceRepository.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("serviceName", s.getServiceName());
            m.put("count", incidentRepository.findAll().stream().filter(i -> i.getAffectedService() != null && i.getAffectedService().getId().equals(s.getId())).count());
            return m;
        }).collect(Collectors.toList());
        stats.put("serviceAnalysis", serviceAnalysis);
        
        return stats;
    }

    @GetMapping("/admin/sla-risks")
    public List<Map<String, Object>> getSlaRisks() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> openIncidents = incidentRepository.findAll().stream()
            .filter(i -> !i.getStatus().equals("CLOSED") && !i.getStatus().equals("RESOLVED"))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> risks = new ArrayList<>();
        for (Incident inc : openIncidents) {
            if (inc.getSlaResolutionDeadline() != null) {
                long minutesRemaining = java.time.Duration.between(now, inc.getSlaResolutionDeadline()).toMinutes();
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
        }
        return risks;
    }

    @GetMapping("/incidents/suggested-resolution")
    public ResponseEntity<?> getSuggestedResolution(@RequestParam(required = false) String category, @RequestParam(required = false) String description) {
        List<Incident> resolvedIncidents = incidentRepository.findAll().stream()
            .filter(i -> i.getResolutionDetails() != null && !i.getResolutionDetails().isEmpty())
            .collect(Collectors.toList());
        
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Incident inc : resolvedIncidents) {
            boolean matchCategory = category != null && inc.getCategory() != null && inc.getCategory().equalsIgnoreCase(category);
            boolean matchKeyword = false;
            if (description != null && inc.getDescription() != null) {
                String[] words = description.toLowerCase().split("\\s+");
                for (String w : words) {
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
                suggestions.add(m);
            }
        }
        return ResponseEntity.ok(suggestions);
    }
}