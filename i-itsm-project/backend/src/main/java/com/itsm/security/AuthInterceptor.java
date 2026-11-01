package com.itsm.security;

import com.itsm.model.User;
import com.itsm.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * Runs before every /api/** request (except /api/auth/**).
 * 1. Checks the JWT token.
 * 2. Loads the user from the database.
 * 3. Blocks non-admins from /api/admin/** and /api/analytics.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // Browsers send an OPTIONS "preflight" request first; it carries no token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) return true; // login & register are public

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return deny(response, 401, "Please sign in first.");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return deny(response, 401, "Session is invalid or expired. Please sign in again.");
        }

        // Use the database as the source of truth (deleted / unapproved users lose access instantly)
        Optional<User> userOpt = userRepository.findByEmail(claims.getSubject());
        if (userOpt.isEmpty() || !userOpt.get().isApproved()) {
            return deny(response, 401, "Account no longer valid.");
        }
        User user = userOpt.get();

        boolean adminOnly = path.startsWith("/api/admin/") || path.equals("/api/analytics");
        if (adminOnly && user.getRole() != User.UserRole.ADMINISTRATOR) {
            return deny(response, 403, "Administrator access required.");
        }

        request.setAttribute("authUser", user);
        return true;
    }

    private boolean deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
