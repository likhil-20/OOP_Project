package com.itsm.service;

import com.itsm.dto.LoginRequest;
import com.itsm.dto.LoginResponse;
import com.itsm.exception.TooManyRequestsException;
import com.itsm.exception.UnauthorizedException;
import com.itsm.model.User;
import com.itsm.repository.UserRepository;
import com.itsm.security.JwtUtil;
import com.itsm.security.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Spy private LoginAttemptService loginAttemptService = new LoginAttemptService(3, Duration.ofMinutes(15));
    @InjectMocks private AuthService authService;

    private User user(String email, String hash) {
        User u = new User();
        u.setId(1L);
        u.setName("Test User");
        u.setEmail(email);
        u.setPassword(hash);
        u.setRole(User.UserRole.EMPLOYEE);
        u.setApproved(true);
        return u;
    }

    @Test
    @DisplayName("TC-10: After 3 wrong passwords the account is locked, even for the correct password")
    void locksAfterRepeatedWrongPasswords() {
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user("a@x.com", "$2a$hash")));
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        LoginRequest bad = new LoginRequest("a@x.com", "wrong");
        for (int i = 0; i < 3; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login(bad));
        }

        LoginRequest good = new LoginRequest("a@x.com", "right");
        assertThrows(TooManyRequestsException.class, () -> authService.login(good));
    }

    @Test
    @DisplayName("TC-11: Correct email and password returns a token and the user's role")
    void successfulLogin() {
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user("a@x.com", "$2a$hash")));
        when(passwordEncoder.matches("right", "$2a$hash")).thenReturn(true);
        when(jwtUtil.generateToken("a@x.com", "EMPLOYEE")).thenReturn("token123");

        LoginResponse res = authService.login(new LoginRequest("a@x.com", "right"));

        assertEquals("token123", res.token());
        assertEquals("EMPLOYEE", res.role());
    }

    @Test
    @DisplayName("TC-12: Unknown email gives the same generic error message")
    void unknownEmail() {
        when(userRepository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
            () -> authService.login(new LoginRequest("nobody@x.com", "whatever")));

        assertEquals("Invalid email or password.", ex.getMessage());
    }
    
    @Test
    @DisplayName("TC-26: Wrong passwords from one IP do not lock the account for a different IP")
    void lockIsPerIp() {
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user("a@x.com", "$2a$hash")));
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);
        when(passwordEncoder.matches("right", "$2a$hash")).thenReturn(true);
        when(jwtUtil.generateToken("a@x.com", "EMPLOYEE")).thenReturn("token123");

        LoginRequest bad = new LoginRequest("a@x.com", "wrong");
        for (int i = 0; i < 3; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login(bad, "1.1.1.1"));
        }
        assertThrows(TooManyRequestsException.class, () -> authService.login(bad, "1.1.1.1"));

        LoginResponse res = authService.login(new LoginRequest("a@x.com", "right"), "2.2.2.2");
        assertEquals("token123", res.token());
    }
}