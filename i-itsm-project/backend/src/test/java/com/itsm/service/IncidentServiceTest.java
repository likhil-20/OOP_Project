package com.itsm.service;

import com.itsm.dto.IncidentRequest;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.ForbiddenException;
import com.itsm.model.Incident;
import com.itsm.model.ItService;
import com.itsm.model.User;
import com.itsm.observer.IncidentEventPublisher;
import com.itsm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ItServiceRepository serviceRepository;
    @Mock private SlaPolicyRepository slaPolicyRepository;
    @Mock private SystemConfigurationRepository configRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentCategoryRepository categoryRepository;
    @Mock private AuditLogRepository auditRepository;
    @Mock private IncidentEventPublisher eventPublisher;
    @InjectMocks private IncidentService incidentService;

    @BeforeEach
    void commonStubs() {
        lenient().when(categoryRepository.existsByNameIgnoreCase("Network")).thenReturn(true);
        lenient().when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User user(Long id, User.UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@x.com");
        u.setRole(role);
        return u;
    }

    private ItService service(Long id, String criticality) {
        ItService s = new ItService();
        s.setId(id);
        s.setServiceName("Service " + id);
        s.setBusinessCriticality(criticality);
        return s;
    }

    private Incident incident(User reporter, User tech, String status) {
        Incident i = new Incident();
        i.setId(10L);
        i.setReporter(reporter);
        i.setAssignedTechnician(tech);
        i.setStatus(status);
        i.setCategory("NETWORK");
        i.setDescription("Something broke");
        return i;
    }

    @Test
    @DisplayName("TC-20: High impact + high urgency + critical service + many users = P1")
    void priorityP1() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, "HIGH")));
        IncidentRequest req = new IncidentRequest("user1@x.com", "Network", "Payment page is down", 1L, "HIGH", "HIGH", 200);

        Incident inc = incidentService.reportIncident(req, user(1L, User.UserRole.EMPLOYEE));

        assertEquals("P1", inc.getPriority());
    }

    @Test
    @DisplayName("TC-21: Low impact + low urgency + 1 user = P4")
    void priorityP4() {
        IncidentRequest req = new IncidentRequest("user1@x.com", "Network", "Mouse is not working", null, "LOW", "LOW", 1);

        Incident inc = incidentService.reportIncident(req, user(1L, User.UserRole.EMPLOYEE));

        assertEquals("P4", inc.getPriority());
    }

    @Test
    @DisplayName("TC-22: Same description, category, service and priority is flagged as a duplicate")
    void duplicateDetected() {
        ItService svc = service(1L, "HIGH");
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(svc));

        Incident existing = incident(user(5L, User.UserRole.EMPLOYEE), null, "ASSIGNED");
        existing.setId(99L);
        existing.setDescription("Payment page is down");
        existing.setCategory("NETWORK");
        existing.setPriority("P1");
        existing.setAffectedService(svc);
        when(incidentRepository.findDuplicateCandidates("NETWORK", "P1", 1L)).thenReturn(List.of(existing));

        IncidentRequest req = new IncidentRequest("user1@x.com", "Network", "Payment page is down", 1L, "HIGH", "HIGH", 200);
        Incident inc = incidentService.reportIncident(req, user(1L, User.UserRole.EMPLOYEE));

        assertTrue(inc.isDuplicate());
        assertEquals(99L, inc.getDuplicateOfIncidentId());
    }

    @Test
    @DisplayName("TC-23: An employee cannot move an incident to IN_PROGRESS")
    void employeeCannotStartProgress() {
        User employee = user(1L, User.UserRole.EMPLOYEE);
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident(employee, null, "ASSIGNED")));

        assertThrows(ForbiddenException.class,
            () -> incidentService.updateStatus(10L, Map.of("status", "IN_PROGRESS"), employee));
    }

    @Test
    @DisplayName("TC-24: Resolving an incident without resolution details is rejected")
    void resolveNeedsDetails() {
        User tech = user(2L, User.UserRole.TECHNICIAN);
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident(user(1L, User.UserRole.EMPLOYEE), tech, "IN_PROGRESS")));

        assertThrows(BadRequestException.class,
            () -> incidentService.updateStatus(10L, Map.of("status", "RESOLVED"), tech));
    }

    @Test
    @DisplayName("TC-25: Resolving an incident lowers the technician's current load by 1")
    void resolveLowersLoad() {
        User tech = user(2L, User.UserRole.TECHNICIAN);
        tech.setCurrentLoad(1);
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident(user(1L, User.UserRole.EMPLOYEE), tech, "IN_PROGRESS")));

        Incident updated = incidentService.updateStatus(10L,
            Map.of("status", "RESOLVED", "resolutionDetails", "Restarted the router"), tech);

        assertEquals("RESOLVED", updated.getStatus());
        assertEquals(0, tech.getCurrentLoad());
        verify(userRepository).save(tech);
    }
}