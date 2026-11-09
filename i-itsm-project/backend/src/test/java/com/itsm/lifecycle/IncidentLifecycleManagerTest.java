package com.itsm.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentLifecycleManagerTest {

    @Test
    @DisplayName("TC-01: Normal lifecycle NEW -> ... -> CLOSED is allowed step by step")
    void happyPathIsAllowed() {
        assertTrue(IncidentLifecycleManager.isValidTransition("NEW", "CLASSIFIED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("CLASSIFIED", "ASSIGNED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("ASSIGNED", "ACKNOWLEDGED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("ACKNOWLEDGED", "IN_PROGRESS"));
        assertTrue(IncidentLifecycleManager.isValidTransition("IN_PROGRESS", "RESOLVED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("RESOLVED", "VERIFIED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("VERIFIED", "CLOSED"));
        assertTrue(IncidentLifecycleManager.isValidTransition("ASSIGNED", "RESOLVED"));
    }

    @Test
    @DisplayName("TC-02: Skipping or going backwards in the lifecycle is rejected")
    void skippingStatesIsRejected() {
        assertFalse(IncidentLifecycleManager.isValidTransition("NEW", "RESOLVED"));
        assertFalse(IncidentLifecycleManager.isValidTransition("CLASSIFIED", "IN_PROGRESS"));
        assertFalse(IncidentLifecycleManager.isValidTransition("IN_PROGRESS", "ASSIGNED"));
    }

    @Test
    @DisplayName("TC-03: A CLOSED incident cannot move to any other state")
    void closedIsFinal() {
        assertFalse(IncidentLifecycleManager.isValidTransition("CLOSED", "NEW"));
        assertFalse(IncidentLifecycleManager.isValidTransition("CLOSED", "RESOLVED"));
    }

    @Test
    @DisplayName("TC-04: Null or unknown statuses are rejected, lowercase is accepted")
    void nullAndUnknownStatuses() {
        assertFalse(IncidentLifecycleManager.isValidTransition(null, "NEW"));
        assertFalse(IncidentLifecycleManager.isValidTransition("NEW", null));
        assertFalse(IncidentLifecycleManager.isValidTransition("NEW", "BANANA"));
        assertTrue(IncidentLifecycleManager.isValidTransition("new", "classified"));
    }
}