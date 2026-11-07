package com.itsm.config;

import com.itsm.model.*;
import com.itsm.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired private UserRepository userRepository;
    @Autowired private ItServiceRepository serviceRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private SystemConfigurationRepository configRepository;
    @Autowired private IncidentCategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            createUser("admin@gmail.com", "System Admin", "admin123", User.UserRole.ADMINISTRATOR, null);
            createUser("tech.alice@gmail.com", "Alice Tech", "tech123", User.UserRole.TECHNICIAN,
                "APPLICATION,DATABASE,NETWORK,SECURITY,HARDWARE");
            createUser("tech.bob@gmail.com", "Bob Loadbalancer", "tech123", User.UserRole.TECHNICIAN,
                "SOFTWARE,CLOUD,STORAGE");
            createUser("employee@gmail.com", "John Employee", "emp123", User.UserRole.EMPLOYEE, null);
        }

        if (categoryRepository.count() == 0) {
            String[] defaults = {"Hardware", "Software", "Network", "Database", "Cloud",
                                 "Security", "Authentication", "Application", "Storage"};
            for (String name : defaults) {
                if (!categoryRepository.existsByName(name)) {
                    IncidentCategory c = new IncidentCategory();
                    c.setName(name);
                    c.setDescription("Standard ITSM classification category");
                    categoryRepository.save(c);
                }
            }
        }

        if (serviceRepository.count() == 0) {
            createService("Payment Application", "HIGH", "Core transaction processing");
            createService("Authentication Service", "HIGH", "OAuth & SSO provider");
            createService("Internal HR Portal", "LOW", "HR records portal");
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

    private void createUser(String email, String name, String password, User.UserRole role, String skills) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(role);
        u.setSkills(skills);
        u.setApproved(true);
        userRepository.save(u);
    }

    private void createService(String name, String criticality, String description) {
        ItService s = new ItService();
        s.setServiceName(name);
        s.setBusinessCriticality(criticality);
        s.setDescription(description);
        serviceRepository.save(s);
    }

    private void createSla(String priority, int resp, int res) {
        SlaPolicy p = new SlaPolicy();
        p.setPriority(priority);
        p.setResponseMinutes(resp);
        p.setResolutionMinutes(res);
        slaPolicyRepository.save(p);
    }
}