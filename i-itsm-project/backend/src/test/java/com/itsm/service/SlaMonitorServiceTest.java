package com.itsm.service;

import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.model.SlaViolation;
import com.itsm.model.User;
import com.itsm.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaMonitorServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditRepository;
    @Mock private SlaViolationRepository violationRepository;
    @InjectMocks private SlaMonitorService service;

    private Incident incident(int createdMinutesAgo, int responseInMinutes, User tech) {
        LocalDateTime now = LocalDateTime.now();
        Incident i = new Incident();
        i.setId(1L);
        i.setStatus("ASSIGNED");
        i.setPriority("P1");
        i.setAssignedTechnician(tech);
        i.setCreatedAt(now.minusMinutes(createdMinutesAgo));
        i.setSlaResponseDeadline(now.plusMinutes(responseInMinutes));
        i.setSlaResolutionDeadline(now.plusMinutes(500));
        return i;
    }

    @Test
    @DisplayName("TC-28: Unacknowledged incident past its response deadline is saved as a violation once")
    void responseBreachSavedOnce() {
        Incident i = incident(10, -5, null);
        when(incidentRepository.findOpen(any())).thenReturn(List.of(i));
        lenient().when(userRepository.findByRole(any())).thenReturn(List.of());

        service.monitor();
        service.monitor();

        verify(violationRepository, times(1)).save(any(SlaViolation.class));
        assertTrue(i.getResponseBreached());
    }

    @Test
    @DisplayName("TC-29: A warning is sent at 80% of the response window, before any breach")
    void warningBeforeDeadline() {
        User tech = new User();
        tech.setEmail("tech@x.com");
        Incident i = incident(9, 1, tech);
        when(incidentRepository.findOpen(any())).thenReturn(List.of(i));

        service.monitor();

        verify(notificationRepository).save(any(Notification.class));
        verify(violationRepository, never()).save(any(SlaViolation.class));
        assertTrue(i.getResponseWarningSent());
    }
}
