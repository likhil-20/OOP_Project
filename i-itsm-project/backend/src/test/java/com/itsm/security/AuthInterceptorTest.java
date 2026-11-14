package com.itsm.security;

import com.itsm.exception.ForbiddenException;
import com.itsm.exception.UnauthorizedException;
import com.itsm.model.User;
import com.itsm.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @InjectMocks private AuthInterceptor interceptor;

    private MockHttpServletRequest request(String path, String token) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", path);
        r.setRequestURI(path);
        if (token != null) r.addHeader("Authorization", "Bearer " + token);
        return r;
    }

    private void signedInAs(User.UserRole role) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("a@x.com");
        when(jwtUtil.parse("tok")).thenReturn(claims);
        User u = new User();
        u.setEmail("a@x.com");
        u.setRole(role);
        u.setApproved(true);
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(u));
    }

    @Test
    @DisplayName("TC-34: A request without a token is rejected with 401")
    void noTokenRejected() {
        assertThrows(UnauthorizedException.class,
            () -> interceptor.preHandle(request("/api/incidents", null), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("TC-35: An employee cannot call an admin endpoint (403)")
    void employeeBlockedFromAdmin() {
        signedInAs(User.UserRole.EMPLOYEE);
        assertThrows(ForbiddenException.class,
            () -> interceptor.preHandle(request("/api/admin/users", "tok"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("TC-36: An administrator can call an admin endpoint")
    void adminAllowed() {
        signedInAs(User.UserRole.ADMINISTRATOR);
        assertTrue(interceptor.preHandle(request("/api/admin/users", "tok"), new MockHttpServletResponse(), new Object()));
    }
}