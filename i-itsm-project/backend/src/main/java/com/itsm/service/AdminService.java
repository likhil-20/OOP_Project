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
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found.");
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
        userRepository.deleteById(id);
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