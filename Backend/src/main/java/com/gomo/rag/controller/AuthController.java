package com.gomo.rag.controller;

import com.gomo.rag.dto.*;
import com.gomo.rag.service.AuthService;
import com.gomo.rag.service.PasswordSetupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final PasswordSetupService passwordSetupService;

    @Value("${app.frontend-url:${app.base-url:http://localhost:5173}}")
    private String frontendUrl;

    public AuthController(AuthService authService, PasswordSetupService passwordSetupService) {
        this.authService = authService;
        this.passwordSetupService = passwordSetupService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("request reached controller");
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }

    @GetMapping("/setup-password/validate")
    public ResponseEntity<TokenValidationResponse> validateSetupToken(@RequestParam("token") String token) {
        log.info("[AuthController] Validating password setup token");
        return ResponseEntity.ok(passwordSetupService.validateSetupToken(token));
    }

    @PostMapping("/setup-password")
    public ResponseEntity<UserResponse> setupPassword(@Valid @RequestBody SetupPasswordRequest request) {
        log.info("[AuthController] Setting permanent password for token");
        return ResponseEntity.ok(passwordSetupService.setupPassword(request));
    }

    @PostMapping("/regenerate-setup-link")
    public ResponseEntity<Map<String, String>> regenerateSetupLink(@RequestBody RegenerateSetupLinkRequest request) {
        log.info("[AuthController] Regenerating setup link");
        passwordSetupService.regenerateSetupLink(request);
        return ResponseEntity.ok(Map.of("message", "A new password setup link has been sent to your registered email address."));
    }



    @GetMapping("/setup-password")
    public ResponseEntity<Void> redirectToFrontendSetup(@RequestParam("token") String token) {
        String targetUrl = frontendUrl + "/setup-password?token=" + token;
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}