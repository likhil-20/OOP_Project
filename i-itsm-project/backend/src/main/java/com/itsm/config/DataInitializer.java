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

        User defaultAdmin = userRepository.findByEmail("admin@gmail.com").orElse(null);
        if (defaultAdmin == null) {
            createUser("admin@gmail.com", "System Admin", "admin123", User.UserRole.ADMINISTRATOR, null);
        } else if (defaultAdmin.getRole() != User.UserRole.ADMINISTRATOR || !defaultAdmin.isApproved()) {
            defaultAdmin.setRole(User.UserRole.ADMINISTRATOR);
            defaultAdmin.setApproved(true);
            userRepository.save(defaultAdmin);
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

        // Any missing default service is added on every start
        createService("Payment Application", "HIGH", "Core transaction processing");
        createService("Authentication Service", "HIGH", "OAuth & SSO provider");
        createService("Internal HR Portal", "LOW", "HR records portal");
        createService("Email & Messaging", "MEDIUM", "Company email, calendar and chat");
        createService("VPN & Remote Access", "HIGH", "Remote work and secure access");
        createService("Corporate Wi-Fi & LAN", "HIGH", "Office wired and wireless network");
        createService("Customer Database", "HIGH", "Customer records and orders");
        createService("Cloud Hosting Platform", "HIGH", "Servers, CDN and DNS for company apps");
        createService("Company Website", "MEDIUM", "Public website and landing pages");
        createService("File Server & Shared Drives", "MEDIUM", "Team documents and shared folders");
        createService("Backup & Recovery", "MEDIUM", "Scheduled backups and restores");
        createService("Payroll & Finance System", "HIGH", "Salaries, invoices and accounting");
        createService("CRM Platform", "MEDIUM", "Sales leads and customer follow-ups");
        createService("Printing Services", "LOW", "Office printers and scanners");
        createService("Video Conferencing", "LOW", "Online meetings and webinars");

        createSla("P1", 5, 30);
        createSla("P2", 15, 120);
        createSla("P3", 60, 480);
        createSla("P4", 240, 1440);
        createSla("P5", 480, 4320);

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
        if (serviceRepository.existsByServiceName(name)) return;
        ItService s = new ItService();
        s.setServiceName(name);
        s.setBusinessCriticality(criticality);
        s.setDescription(description);
        serviceRepository.save(s);
    }

    private void createSla(String priority, int resp, int res) {
        if (slaPolicyRepository.findByPriority(priority).isPresent()) return;
        SlaPolicy p = new SlaPolicy();
        p.setPriority(priority);
        p.setResponseMinutes(resp);
        p.setResolutionMinutes(res);
        slaPolicyRepository.save(p);
    }
}