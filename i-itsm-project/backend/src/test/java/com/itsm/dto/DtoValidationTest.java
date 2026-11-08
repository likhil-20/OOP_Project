package com.itsm.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static <T> boolean hasMessage(Set<ConstraintViolation<T>> violations, String message) {
        return violations.stream().anyMatch(v -> v.getMessage().equals(message));
    }

    @Test
    @DisplayName("TC-16: Login email longer than 150 characters is rejected")
    void loginEmailTooLong() {
        String longEmail = "a".repeat(145) + "@x.com"; // 151 characters
        var violations = validator.validate(new LoginRequest(longEmail, "secret"));
        assertTrue(hasMessage(violations, "Email must be at most 150 characters."));
    }

    @Test
    @DisplayName("TC-17: Login password longer than 100 characters is rejected")
    void loginPasswordTooLong() {
        var violations = validator.validate(new LoginRequest("a@x.com", "p".repeat(101)));
        assertTrue(hasMessage(violations, "Password must be at most 100 characters."));
    }

    @Test
    @DisplayName("TC-18: Incident description longer than 2000 characters is rejected")
    void incidentDescriptionTooLong() {
        var violations = validator.validate(
            new IncidentRequest("emp@x.com", "Network", "d".repeat(2001), 1L, "HIGH", "HIGH", 5));
        assertTrue(hasMessage(violations, "Description must be 5 to 2000 characters."));
    }

    @Test
    @DisplayName("TC-19: A correct incident request passes validation")
    void validIncidentPasses() {
        var violations = validator.validate(
            new IncidentRequest("emp@x.com", "Network", "Wi-Fi is down in block A", 1L, "HIGH", "HIGH", 5));
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("TC-27: Affected users above 10000 is rejected, exactly 10000 is accepted")
    void affectedUsersCap() {
        var tooMany = validator.validate(
            new IncidentRequest("emp@x.com", "Network", "Wi-Fi is down in block A", 1L, "HIGH", "HIGH", 10001));
        assertTrue(hasMessage(tooMany, "Affected users cannot be more than 10000."));

        var atLimit = validator.validate(
            new IncidentRequest("emp@x.com", "Network", "Wi-Fi is down in block A", 1L, "HIGH", "HIGH", 10000));
        assertTrue(atLimit.isEmpty());
    }
}