package com.itsm.security;

import com.itsm.exception.ForbiddenException;
import com.itsm.exception.UnauthorizedException;
import com.itsm.model.User;
import com.itsm.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Runs before every /api/** request (except /api/auth/**).
 * 1. Checks the JWT token.
 * 2. Loads the user from the database.
 * 3. Blocks non-admins from /api/admin/** and /api/analytics.
 * Errors are thrown as exceptions; GlobalExceptionHandler turns them into JSON.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Browsers send an OPTIONS "preflight" request first; it carries no token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) return true; // login & register are public

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("Please sign in first.");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Session is invalid or expired. Please sign in again.");
        }

        // Use the database as the source of truth (deleted / unapproved users lose access instantly)
        Optional<User> userOpt = userRepository.findByEmail(claims.getSubject());
        if (userOpt.isEmpty() || !userOpt.get().isApproved()) {
            throw new UnauthorizedException("Account no longer valid.");
        }
        User user = userOpt.get();

        boolean adminOnly = path.startsWith("/api/admin/") || path.equals("/api/analytics");
        if (adminOnly && user.getRole() != User.UserRole.ADMINISTRATOR) {
            throw new ForbiddenException("Administrator access required.");
        }

        request.setAttribute("authUser", user);
        return true;
    }
}