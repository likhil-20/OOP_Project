package com.itsm.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
    @NotBlank(message = "Reporter email is required.")
    @Email(message = "Reporter email format is invalid.")
    String email,

    @NotBlank(message = "Category is required.")
    String category,

    @NotBlank(message = "Description is required.")
    @Size(min = 5, max = 2000, message = "Description must be 5 to 2000 characters.")
    String description,

    Long serviceId,

    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "Impact must be HIGH, MEDIUM or LOW.")
    String impact,

    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "Urgency must be HIGH, MEDIUM or LOW.")
    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "Urgency must be HIGH, MEDIUM or LOW.")
    String urgency,

    @Min(value = 1, message = "Affected users must be at least 1.")
    @Max(value = 1000000, message = "Affected users value is too large.")
    Integer affectedUsers

) {}