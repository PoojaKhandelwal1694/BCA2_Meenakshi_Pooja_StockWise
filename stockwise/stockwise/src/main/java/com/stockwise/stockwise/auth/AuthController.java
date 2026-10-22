
package com.stockwise.stockwise.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
    },
    allowCredentials = "true"
)
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody Map<String, String> body) {

        String fullName = body.get("fullName");
        String email = body.get("email");
        String password = body.get("password");

        if (fullName == null || fullName.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message",
                        "Enter your name, email, and a password of at least 8 characters."
                    ));
        }

        fullName = fullName.trim();
        email = email.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "An account with this email already exists."));
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Account created successfully."
                ));
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Email and password are required."));
        }

        User user = userRepository
                .findByEmail(email.trim().toLowerCase())
                .orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "message", "Invalid email or password."
                    ));
        }

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());

        System.out.println("LOGIN Session ID: " + session.getId());
        System.out.println("LOGIN userId: " + session.getAttribute("userId"));
        System.out.println("LOGIN userName: " + session.getAttribute("userName"));

        return ResponseEntity.ok(Map.of(
            "message", "Login successful.",
            "fullName", user.getFullName()
        ));
    }

    // Check current login session
    @GetMapping("/me")
public ResponseEntity<?> getCurrentUser(HttpSession session) {

    System.out.println("Session ID: " + session.getId());
    System.out.println("Session attributes: "
            + java.util.Collections.list(
                    session.getAttributeNames()));

    Object userId = session.getAttribute("userId");
    Object userName = session.getAttribute("userName");

    if (userId == null || userName == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Please log in first."));
    }

    return ResponseEntity.ok(
            Map.of("fullName", userName));
}
    // Logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully."));
    }
}
