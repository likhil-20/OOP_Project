package com.itsm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name is required.")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters.")
    String name,

    @NotBlank(message = "Email is required.")
    @Email(message = "Email format is invalid.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    String email,

    @NotBlank(message = "Password is required.")
    @Size(min = 6, max = 100, message = "Password must be 6 to 100 characters.")
    String password
) {}