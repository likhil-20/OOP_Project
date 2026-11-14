package com.itsm.controller;

import com.itsm.dto.IncidentRequest;
import com.itsm.dto.LoginRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.dto.TriageRequest;
import com.itsm.dto.CreateUserRequest;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.ConflictException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.UserRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.IncidentCategoryRepository;
import com.itsm.model.*;
import com.itsm.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin controller: it only receives the request, calls a service, and returns the response.
 * All business logic lives in the service classes.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ITSMController {

    @Autowired private AuthService authService;
    @Autowired private IncidentService incidentService;
    @Autowired private AdminService adminService;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private NotificationService notificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private com.itsm.repository.SlaViolationRepository slaViolationRepository;
    @Autowired private com.itsm.repository.AuditLogRepository auditLogRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private com.itsm.service.EmailService emailService;
    @Autowired private TriageService triageService;
    // The logged-in user, put there by AuthInterceptor after checking the token
    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("authUser");
    }
    // --- PAIR OF HELPER METHODS GO RIGHT HERE ---
    private String asText(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ---------- Auth (public) ----------

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(req, request.getRemoteAddr()));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Pending administrator approval."));
    }

    // ---------- Lookups (any logged-in user) ----------

    @GetMapping("/services")
    public List<ItService> getServicesPublic() {
        return adminService.getAllServices();
    }

    @GetMapping("/categories")
    public List<IncidentCategory> getCategoriesPublic() {
        return adminService.getAllCategories();
    }

    // ---------- Admin: categories ----------

    @GetMapping("/admin/categories")
    public List<IncidentCategory> getAllCategories() {
        return adminService.getAllCategories();
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<?> saveCategory(@Valid @RequestBody IncidentCategory c) {
        return ResponseEntity.ok(adminService.saveCategory(c));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // ---------- Admin: users ----------

    @GetMapping("/admin/users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PutMapping("/admin/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveUser(id));
    }
    @PostMapping("/admin/users")
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
    String email = req.email().trim();
    if (userRepository.findByEmail(email).isPresent()) {
        throw new ConflictException("Email already registered.");
    }
    User.UserRole role;
    try {
        role = User.UserRole.valueOf(req.role().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new BadRequestException("Role must be EMPLOYEE, TECHNICIAN or ADMINISTRATOR.");
    }
    User u = new User();
    u.setName(req.name().trim());
    u.setEmail(email);
    u.setPassword(passwordEncoder.encode(req.password()));
    u.setRole(role);
    u.setSkills(role == User.UserRole.TECHNICIAN ? cleanSkills(req.skills()) : null);
    u.setApproved(true);
    User saved = userRepository.save(u);

    emailService.send(saved.getEmail(), "Your I-ITSM account is ready",
    "Hello " + saved.getName() + ",\n\nAn I-ITSM account has been created for you.\n\n"
    + "Email: " + saved.getEmail() + "\n"
    + "Password: " + req.password() + "\n"
    + "Role: " + saved.getRole() + "\n\nSign in at http://localhost:3000\n\nI-ITSM Service Desk");
    return ResponseEntity.ok(saved);
}

@PutMapping("/admin/users/{id}/role")
public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestBody Map<String, Object> payload,
                                        HttpServletRequest request) {
    User u = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    if (currentUser(request).getId().equals(id)) {
        throw new BadRequestException("You cannot change your own role.");
    }
    String roleText = asText(payload.get("role"));
    if (isBlank(roleText)) throw new BadRequestException("Role is required.");
    User.UserRole newRole;
    try {
        newRole = User.UserRole.valueOf(roleText.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new BadRequestException("Role must be EMPLOYEE, TECHNICIAN or ADMINISTRATOR.");
    }
    if (u.getRole() == User.UserRole.TECHNICIAN && newRole != User.UserRole.TECHNICIAN) {
        boolean hasActive = incidentRepository.findByAssignedTechnicianId(id).stream()
            .anyMatch(i -> !List.of("RESOLVED", "VERIFIED", "CLOSED").contains(i.getStatus()));
        if (hasActive) {
            throw new BadRequestException("This technician still has active incidents. Resolve them first.");
        }
    }
    u.setRole(newRole);
    if (newRole == User.UserRole.TECHNICIAN) {
        u.setSkills(cleanSkills(asText(payload.get("skills"))));
    } else {
        u.setSkills(null);
        u.setCurrentLoad(0);
    }
    return ResponseEntity.ok(userRepository.save(u));
}

private String cleanSkills(String raw) {
    if (raw == null || raw.isBlank()) return null;
    List<String> out = new ArrayList<>();
    for (String s : raw.split(",")) {
        String skill = s.trim().toUpperCase();
        if (skill.isEmpty()) continue;
        if (!categoryRepository.existsByNameIgnoreCase(skill)) {
            throw new BadRequestException("Unknown skill (not a category): " + skill);
        }
        if (!out.contains(skill)) out.add(skill);
    }
    return out.isEmpty() ? null : String.join(",", out);
}
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        adminService.deleteUser(id, currentUser(request));
        return ResponseEntity.ok().build();
    }

    // ---------- Admin: services ----------

    @GetMapping("/admin/services")
    public List<ItService> getAllServices() {
        return adminService.getAllServices();
    }

    @PostMapping("/admin/services")
    public ItService saveService(@Valid @RequestBody ItService s) {
        return adminService.saveService(s);
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        adminService.deleteService(id);
        return ResponseEntity.ok().build();
    }

    // ---------- Incidents ----------

    @GetMapping("/incidents")
    public List<Incident> getAllIncidents(HttpServletRequest request) {
        return incidentService.getIncidentsFor(currentUser(request));
    }

    @PostMapping("/incidents")
    public ResponseEntity<?> reportIncident(@Valid @RequestBody IncidentRequest req, HttpServletRequest request) {
        return ResponseEntity.ok(incidentService.reportIncident(req, currentUser(request)));
    }

    @PutMapping("/incidents/{id}/status")
    public ResponseEntity<?> updateIncidentStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> payload,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok(incidentService.updateStatus(id, payload, currentUser(request)));
    }

    @PostMapping("/incidents/triage")
    public ResponseEntity<?> triage(@Valid @RequestBody TriageRequest req) {
        return ResponseEntity.ok(triageService.triage(req.description()));
    }

    @GetMapping("/incidents/suggested-resolution")
    public ResponseEntity<?> getSuggestedResolution(@RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String description) {
        return ResponseEntity.ok(incidentService.getSuggestedResolutions(category, description));
    }

    // ---------- Admin: SLA & config ----------

    @GetMapping("/admin/sla")
    public List<SlaPolicy> getAllSla() {
        return adminService.getAllSla();
    }

    @PostMapping("/admin/sla")
    public ResponseEntity<?> saveSla(@Valid @RequestBody SlaPolicy sla) {
        return ResponseEntity.ok(adminService.saveSla(sla));
    }

    @GetMapping("/admin/config")
    public List<SystemConfiguration> getAllConfig() {
        return adminService.getAllConfig();
    }

    @PostMapping("/admin/config")
    public ResponseEntity<?> saveConfig(@Valid @RequestBody SystemConfiguration cfg) {
        return ResponseEntity.ok(adminService.saveConfig(cfg));
    }

    @GetMapping("/admin/sla-violations")
    public List<Incident> getSlaViolations() {
        return analyticsService.getSlaViolations();
    }

    @GetMapping("/admin/sla-risks")
    public List<Map<String, Object>> getSlaRisks() {
        return analyticsService.getSlaRisks();
    }

    @GetMapping("/admin/sla-violation-history")
    public List<SlaViolation> getSlaViolationHistory() {
        return slaViolationRepository.findAllByOrderByDetectedAtDesc();
    }

    @GetMapping("/admin/activity")
    public List<com.itsm.model.AuditLog> getRecentActivity() {
        return auditLogRepository.findTop15ByOrderByTimestampDesc();
    }

    // ---------- Notifications & audit ----------

    @GetMapping("/notifications")
    public List<com.itsm.model.Notification> getNotifications(HttpServletRequest request) {
        return notificationService.getFor(currentUser(request));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead(HttpServletRequest request) {
        notificationService.markAllRead(currentUser(request));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id, HttpServletRequest request) {
        notificationService.markRead(id, currentUser(request));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit/{incidentId}")
    public ResponseEntity<?> getAuditLogs(@PathVariable Long incidentId, HttpServletRequest request) {
        return ResponseEntity.ok(incidentService.getAuditLogs(incidentId, currentUser(request)));
    }

    // ---------- Analytics (admin only, enforced by AuthInterceptor) ----------

        // ---------- Mailbox ----------

    @GetMapping("/emails")
    public List<EmailLog> myEmails(HttpServletRequest request) {
        return emailService.getFor(currentUser(request));
    }

    @GetMapping("/admin/emails")
    public List<EmailLog> allEmails() {
        return emailService.getAll();
    }

    @PutMapping("/emails/{id}/open")
    public ResponseEntity<?> openEmail(@PathVariable Long id, HttpServletRequest request) {
        emailService.markOpened(id, currentUser(request));
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics() {
        return analyticsService.getAnalytics();
    }
}