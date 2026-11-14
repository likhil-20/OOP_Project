package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriageRequest(
    @NotBlank(message = "Description is required.")
    @Size(min = 5, max = 2000, message = "Description must be 5 to 2000 characters.")
    String description
) {}