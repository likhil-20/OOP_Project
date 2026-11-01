package com.itsm.dto;

// What the browser gets after login. Note: NO password here.
public record LoginResponse(Long id, String name, String email, String role, String token) {}
