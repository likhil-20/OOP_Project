package com.itsm.service;

import com.itsm.dto.LoginRequest;
import com.itsm.dto.LoginResponse;
import com.itsm.dto.RegisterRequest;
import com.itsm.exception.ConflictException;
import com.itsm.exception.ForbiddenException;
import com.itsm.exception.UnauthorizedException;
import com.itsm.model.User;
import com.itsm.repository.UserRepository;
import com.itsm.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email().trim());
        // Same message for "no such user" and "wrong password" so attackers can't find valid emails
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        User user = userOpt.get();
        String stored = user.getPassword();
        boolean passwordOk;
        if (stored != null && stored.startsWith("$2")) {
            passwordOk = passwordEncoder.matches(req.password(), stored);
        } else {
            // Old plain-text password: accept once, then upgrade it to a hash
            passwordOk = stored != null && stored.equals(req.password());
            if (passwordOk) {
                user.setPassword(passwordEncoder.encode(req.password()));
                userRepository.save(user);
            }
        }
        if (!passwordOk) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        if (!user.isApproved()) {
            throw new ForbiddenException("Your account is pending administrator approval.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), token);
    }

    public void register(RegisterRequest req) {
        String email = req.email().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered.");
        }
        User newUser = new User();
        newUser.setName(req.name().trim());
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(req.password()));
        newUser.setRole(User.UserRole.EMPLOYEE);
        newUser.setApproved(false);
        userRepository.save(newUser);
    }
}