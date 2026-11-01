package com.itsm.controller;
import com.itsm.dto.IncidentRequest;
import com.itsm.dto.LoginRequest;
import com.itsm.dto.LoginResponse;
import com.itsm.dto.RegisterRequest;
import com.itsm.model.*;
import com.itsm.repository.*;
import com.itsm.security.JwtUtil;
import com.itsm.strategy.*;
import com.itsm.lifecycle.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ITSMController {

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final List<String> TEXT_FIELDS = List.of(
        "investigationNotes", "resolutionDetails", "additionalInfo", "techRequestInfo", "feedbackComments");
    private static final Set<String> ASSIGNMENT_STRATEGIES = Set.of("SKILL_BASED", "LOAD_BASED");

    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private SystemConfigurationRepository configRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setName("System Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.UserRole.ADMINISTRATOR);
            admin.setApproved(true);
            userRepository.save(admin);

            User tech = new User();
            tech.setEmail("tech.alice@gmail.com");
            tech.setName("Alice Tech");
            tech.setPassword(passwordEncoder.encode("tech123"));
            tech.setRole(User.UserRole.TECHNICIAN);
            tech.setSkills("APPLICATION,DATABASE,NETWORK,SECURITY,HARDWARE");
            tech.setApproved(true);
            userRepository.save(tech);

            User tech2 = new User();
            tech2.setEmail("tech.bob@gmail.com");
            tech2.setName("Bob Loadbalancer");
            tech2.setPassword(passwordEncoder.encode("tech123"));
            tech2.setRole(User.UserRole.TECHNICIAN);
            tech2.setSkills("SOFTWARE,CLOUD,STORAGE");
            tech2.setApproved(true);
            userRepository.save(tech2);

            User emp = new User();
            emp.setEmail("employee@gmail.com");
            emp.setName("John Employee");
            emp.setPassword(passwordEncoder.encode("emp123"));
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

    // ---------- Validation helpers ----------

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(400).body(Map.of("error", message));
    }

    private ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(403).body(Map.of("error", message));
    }

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

    // ---------- Security helpers ----------

    // The logged-in user, put there by AuthInterceptor after checking the token
    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("authUser");
    }

    // Admin: everything. Employee: only their own tickets. Technician: only tickets assigned to them.
    private boolean canAccess(User me, Incident inc) {
        if (me.getRole() == User.UserRole.ADMINISTRATOR) return true;
        if (me.getRole() == User.UserRole.EMPLOYEE) {
            return inc.getReporter() != null && inc.getReporter().getId().equals(me.getId());
        }
        if (me.getRole() == User.UserRole.TECHNICIAN) {
            return inc.getAssignedTechnician() != null && inc.getAssignedTechnician().getId().equals(me.getId());
        }
        return false;
    }

    // ---------- Priority calculation ----------

    private static int level(String v) {
        if ("HIGH".equals(v)) return 3;
        if ("LOW".equals(v)) return 1;
        return 2; // MEDIUM or unknown
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

    // ---------- Auth (public: no token needed) ----------

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email().trim());
        // Same message for "no such user" and "wrong password" so attackers can't find valid emails
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password."));
        }

        User user = userOpt.get();
        String stored = user.getPassword();
        boolean passwordOk;
        if (stored != null && stored.startsWith("$2")) {
            // Already a BCrypt hash
            passwordOk = passwordEncoder.matches(req.password(), stored);
        } else {
            // Old plain-text password from before this change: accept once, then upgrade it to a hash
            passwordOk = stored != null && stored.equals(req.password());
            if (passwordOk) {
                user.setPassword(passwordEncoder.encode(req.password()));
                userRepository.save(user);
            }
        }
        if (!passwordOk) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password."));
        }
        if (!user.isApproved()) {
            return ResponseEntity.status(403).body(Map.of("error", "Your account is pending administrator approval."));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(
            user.getId(), user.getName(), user.getEmail(), user.getRole().name(), token));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(400).body(Map.of("error", "Email already registered."));
        }
        User newUser = new User();
        newUser.setName(req.name().trim());
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(req.password()));
        newUser.setRole(User.UserRole.EMPLOYEE);
        newUser.setApproved(false);
        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Pending administrator approval."));
    }

    // ---------- Lookups (any logged-in user) ----------

    @GetMapping("/services")
    public List<ItService> getServicesPublic() {
        return serviceRepository.findAll();
    }

    @GetMapping("/categories")
    public List<IncidentCategory> getCategoriesPublic() {
        return categoryRepository.findAll();
    }

    // ---------- Admin: categories (admin only, enforced by AuthInterceptor) ----------

    @GetMapping("/admin/categories")
    public List<IncidentCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<?> saveCategory(@Valid @RequestBody IncidentCategory c) {
        c.setName(c.getName().trim());
        if (c.getId() == null && categoryRepository.existsByNameIgnoreCase(c.getName())) {
            return badRequest("Category already exists.");
        }
        return ResponseEntity.ok(categoryRepository.save(c));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Category not found."));
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ---------- Admin: users ----------

    @GetMapping("/admin/users")
    public List<User> getAllUsers() { return userRepository.findAll(); }

    @PutMapping("/admin/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }
        User u = opt.get();
        u.setApproved(true);
        userRepository.save(u);
        return ResponseEntity.ok(u);
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }
        if (currentUser(request).getId().equals(id)) {
            return badRequest("You cannot delete your own account.");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ---------- Admin: services ----------

    @GetMapping("/admin/services")
    public List<ItService> getAllServices() {
        return serviceRepository.findAll();
    }

    @PostMapping("/admin/services")
    public ItService saveService(@Valid @RequestBody ItService s) {
        s.setServiceName(s.getServiceName().trim());
        return serviceRepository.save(s);
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        if (!serviceRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Service not found."));
        }
        serviceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ---------- Incidents ----------

    @GetMapping("/incidents")
    public List<Incident> getAllIncidents(HttpServletRequest request) {
        // Who you are comes from your token, NOT from query parameters you could fake
        User me = currentUser(request);
        if (me.getRole() == User.UserRole.EMPLOYEE) {
            return incidentRepository.findByReporterId(me.getId());
        } else if (me.getRole() == User.UserRole.TECHNICIAN) {
            return incidentRepository.findByAssignedTechnicianId(me.getId());
        }
        return incidentRepository.findAll();
    }

    @PostMapping("/incidents")
    public ResponseEntity<?> reportIncident(@Valid @RequestBody IncidentRequest req, HttpServletRequest request) {
        try {
            // The reporter is always the logged-in user (req.email() is ignored)
            User reporter = currentUser(request);

            String category = req.category().trim();
            if (!categoryRepository.existsByNameIgnoreCase(category)) {
                return badRequest("Unknown category: " + category);
            }

            Incident inc = new Incident();
            inc.setReporter(reporter);
            inc.setCategory(category.toUpperCase());
            inc.setDescription(req.description().trim());

            if (req.serviceId() != null) {
                Optional<ItService> serviceOpt = serviceRepository.findById(req.serviceId());
                if (serviceOpt.isEmpty()) {
                    return badRequest("Selected IT service does not exist.");
                }
                inc.setAffectedService(serviceOpt.get());
            }

            inc.setImpact(req.impact() != null ? req.impact() : "MEDIUM");
            inc.setUrgency(req.urgency() != null ? req.urgency() : "MEDIUM");
            inc.setAffectedUsers(req.affectedUsers() != null ? req.affectedUsers() : 1);

            String criticality = inc.getAffectedService() != null
                ? inc.getAffectedService().getBusinessCriticality()
                : "MEDIUM";

            String priority = calculatePriority(inc.getImpact(), inc.getUrgency(), criticality, inc.getAffectedUsers());
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
    public ResponseEntity<?> updateIncidentStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload,
                                                  HttpServletRequest request) {
        Optional<Incident> incOpt = incidentRepository.findById(id);
        if (incOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Incident not found."));
        }
        Incident inc = incOpt.get();

        // --- who is calling, and are they allowed to touch this ticket? ---
        User me = currentUser(request);
        if (!canAccess(me, inc)) {
            return forbidden("You are not allowed to modify this incident.");
        }
        String userEmail = me.getEmail(); // from the token, not from the request body

        // --- validate input ---
        String newStatus = asText(payload.get("status"));

        if (isBlank(newStatus)) return badRequest("Status is required.");
        newStatus = newStatus.trim().toUpperCase();
        if (!isKnownStatus(newStatus)) return badRequest("Unknown status: " + newStatus);

        // --- role rules for which status changes are allowed ---
        boolean sameStatus = newStatus.equals(inc.getStatus());
        if (me.getRole() == User.UserRole.EMPLOYEE && !sameStatus && !"VERIFIED".equals(newStatus)) {
            return forbidden("Employees can only verify a resolved incident.");
        }
        if (me.getRole() == User.UserRole.TECHNICIAN && !sameStatus
                && ("VERIFIED".equals(newStatus) || "CLOSED".equals(newStatus))) {
            return forbidden("Technicians cannot verify or close incidents.");
        }

        for (String field : TEXT_FIELDS) {
            String value = asText(payload.get(field));
            if (value != null && value.length() > MAX_TEXT_LENGTH) {
                return badRequest(field + " must be at most " + MAX_TEXT_LENGTH + " characters.");
            }
        }

        if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(inc.getStatus()) && isBlank(asText(payload.get("resolutionDetails")))) {
            return badRequest("Resolution details are required to resolve an incident.");
        }

        Integer rating = null;
        if (payload.containsKey("feedbackRating") && payload.get("feedbackRating") != null) {
            try {
                rating = Integer.valueOf(payload.get("feedbackRating").toString().trim());
            } catch (NumberFormatException e) {
                return badRequest("Feedback rating must be a number from 1 to 5.");
            }
            if (rating < 1 || rating > 5) {
                return badRequest("Feedback rating must be a number from 1 to 5.");
            }
        }

        String oldStatus = inc.getStatus();
        if (!IncidentLifecycleManager.isValidTransition(oldStatus, newStatus)) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid state transition from " + oldStatus + " to " + newStatus));
        }

        // --- apply changes ---
        inc.setStatus(newStatus);
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

    // ---------- Admin: SLA & config ----------

    @GetMapping("/admin/sla")
    public List<SlaPolicy> getAllSla() { return slaPolicyRepository.findAll(); }

    @PostMapping("/admin/sla")
    public ResponseEntity<?> saveSla(@Valid @RequestBody SlaPolicy sla) {
        if (sla.getResolutionMinutes() < sla.getResponseMinutes()) {
            return badRequest("Resolution time cannot be shorter than response time.");
        }
        return ResponseEntity.ok(slaPolicyRepository.save(sla));
    }

    @GetMapping("/admin/config")
    public List<SystemConfiguration> getAllConfig() { return configRepository.findAll(); }

    @PostMapping("/admin/config")
    public ResponseEntity<?> saveConfig(@Valid @RequestBody SystemConfiguration cfg) {
        if ("ASSIGNMENT_STRATEGY".equals(cfg.getConfigKey()) && !ASSIGNMENT_STRATEGIES.contains(cfg.getConfigValue())) {
            return badRequest("Assignment strategy must be SKILL_BASED or LOAD_BASED.");
        }
        SystemConfiguration existing = configRepository.findByConfigKey(cfg.getConfigKey()).orElse(new SystemConfiguration());
        existing.setConfigKey(cfg.getConfigKey());
        existing.setConfigValue(cfg.getConfigValue());
        return ResponseEntity.ok(configRepository.save(existing));
    }

    @GetMapping("/admin/sla-violations")
    public List<Incident> getSlaViolations() {
        LocalDateTime now = LocalDateTime.now();
        return incidentRepository.findAll().stream()
            .filter(i -> !i.getStatus().equals("CLOSED") && !i.getStatus().equals("RESOLVED"))
            .filter(i -> i.getSlaResolutionDeadline() != null && now.isAfter(i.getSlaResolutionDeadline()))
            .collect(Collectors.toList());
    }

    // ---------- Notifications & audit ----------

    @GetMapping("/notifications")
    public List<Notification> getNotifications(HttpServletRequest request) {
        // You only ever get YOUR notifications
        return notificationRepository.findByRecipientEmail(currentUser(request).getEmail());
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id, HttpServletRequest request) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null) {
            if (!currentUser(request).getEmail().equals(n.getRecipientEmail())) {
                return forbidden("This notification is not yours.");
            }
            n.setRead(true);
            notificationRepository.save(n);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit/{incidentId}")
    public ResponseEntity<?> getAuditLogs(@PathVariable Long incidentId, HttpServletRequest request) {
        Optional<Incident> incOpt = incidentRepository.findById(incidentId);
        if (incOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Incident not found."));
        }
        if (!canAccess(currentUser(request), incOpt.get())) {
            return forbidden("You are not allowed to view this incident's history.");
        }
        return ResponseEntity.ok(auditRepository.findByIncidentId(incidentId));
    }

    // ---------- Analytics (admin only, enforced by AuthInterceptor) ----------

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
