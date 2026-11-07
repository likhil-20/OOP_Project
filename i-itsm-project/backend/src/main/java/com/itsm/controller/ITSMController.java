package com.itsm.controller;

import com.itsm.dto.IncidentRequest;
import com.itsm.dto.LoginRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.model.*;
import com.itsm.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // The logged-in user, put there by AuthInterceptor after checking the token
    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("authUser");
    }

    // ---------- Auth (public) ----------

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
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

    // ---------- Notifications & audit ----------

    @GetMapping("/notifications")
    public List<Notification> getNotifications(HttpServletRequest request) {
        return notificationService.getFor(currentUser(request));
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

    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics() {
        return analyticsService.getAnalytics();
    }
}