package com.itsm.service;

import com.itsm.exception.BadRequestException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.*;
import com.itsm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AdminService {

    private static final Set<String> ASSIGNMENT_STRATEGIES = Set.of("SKILL_BASED", "LOAD_BASED");

    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private SystemConfigurationRepository configRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    public static final String DEFAULT_ADMIN_EMAIL = "admin@gmail.com";

    // ---------- Categories ----------

    public List<IncidentCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public IncidentCategory saveCategory(IncidentCategory c) {
        c.setName(c.getName().trim());
        if (c.getId() == null && categoryRepository.existsByNameIgnoreCase(c.getName())) {
            throw new BadRequestException("Category already exists.");
        }
        return categoryRepository.save(c);
    }

    public void deleteCategory(Long id) {
        IncidentCategory cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // Remove this skill from every technician who had it
        for (User u : userRepository.findByRole(User.UserRole.TECHNICIAN)) {
            if (u.getSkills() == null) continue;
            List<String> kept = new java.util.ArrayList<>();
            for (String s : u.getSkills().split(",")) {
                String skill = s.trim();
                if (!skill.isEmpty() && !skill.equalsIgnoreCase(cat.getName().trim())) kept.add(skill);
            }
            u.setSkills(kept.isEmpty() ? null : String.join(",", kept));
            userRepository.save(u);
        }

        categoryRepository.deleteById(id);
    }

    // ---------- Users ----------

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User approveUser(Long id) {
        User u = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        u.setApproved(true);
        return userRepository.save(u);
    }

    public void deleteUser(Long id, User me) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found.");
        }
        if (me.getId().equals(id)) {
            throw new BadRequestException("You cannot delete your own account.");
        }
            User target = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    
    if (DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(target.getEmail())) {
        throw new BadRequestException("The default system admin cannot be removed.");
    }

    if (target.getRole() == User.UserRole.TECHNICIAN) {
        List<Incident> mine = incidentRepository.findByAssignedTechnicianId(id);
        boolean hasActive = mine.stream()
            .anyMatch(i -> !List.of("RESOLVED", "VERIFIED", "CLOSED").contains(i.getStatus()));
        if (hasActive) {
            throw new BadRequestException("This technician still has active incidents. Resolve or reassign them first.");
        }
        // Finished incidents: keep them, just detach the technician so the delete is allowed
        for (Incident i : mine) {
            i.setAssignedTechnician(null);
            incidentRepository.save(i);
        }
    }
        userRepository.deleteById(id);
        
    }

        // Admin sets a new password; if none is given, the system makes a random one
    public void resetPassword(Long id, String newPassword) {
        User target = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String password = newPassword == null ? "" : newPassword;
        if (password.isBlank()) {
            password = generateTempPassword();
        } else if (password.length() < 6 || password.length() > 100) {
            throw new BadRequestException("Password must be 6 to 100 characters.");
        }

        target.setPassword(passwordEncoder.encode(password));
        userRepository.save(target);

        emailService.send(target.getEmail(), "Your I-ITSM password was changed",
            "Hello " + target.getName() + ",\n\nAn administrator changed your I-ITSM password.\n\n"
            + "Email: " + target.getEmail() + "\nNew password: " + password
            + "\n\nSign in at http://localhost:3000 and keep this password private.\n\nI-ITSM Service Desk");
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // ---------- IT services ----------

    public List<ItService> getAllServices() {
        return serviceRepository.findAll();
    }

    public ItService saveService(ItService s) {
        s.setServiceName(s.getServiceName().trim());
        return serviceRepository.save(s);
    }

    public void deleteService(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service not found.");
        }
        serviceRepository.deleteById(id);
    }

    // ---------- SLA & config ----------

    public List<SlaPolicy> getAllSla() {
        return slaPolicyRepository.findAll();
    }

    public SlaPolicy saveSla(SlaPolicy sla) {
        if (sla.getResolutionMinutes() < sla.getResponseMinutes()) {
            throw new BadRequestException("Resolution time cannot be shorter than response time.");
        }
        return slaPolicyRepository.save(sla);
    }

    public List<SystemConfiguration> getAllConfig() {
        return configRepository.findAll();
    }

    public SystemConfiguration saveConfig(SystemConfiguration cfg) {
        if ("ASSIGNMENT_STRATEGY".equals(cfg.getConfigKey()) && !ASSIGNMENT_STRATEGIES.contains(cfg.getConfigValue())) {
            throw new BadRequestException("Assignment strategy must be SKILL_BASED or LOAD_BASED.");
        }
        SystemConfiguration existing = configRepository.findByConfigKey(cfg.getConfigKey())
            .orElse(new SystemConfiguration());
        existing.setConfigKey(cfg.getConfigKey());
        existing.setConfigValue(cfg.getConfigValue());
        return configRepository.save(existing);
    }
}