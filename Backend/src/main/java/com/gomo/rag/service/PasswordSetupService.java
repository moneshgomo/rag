package com.gomo.rag.service;

import com.gomo.rag.dto.RegenerateSetupLinkRequest;
import com.gomo.rag.dto.SetupPasswordRequest;
import com.gomo.rag.dto.TokenValidationResponse;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.entity.PasswordSetupToken;
import com.gomo.rag.entity.User;
import com.gomo.rag.repository.PasswordSetupTokenRepository;
import com.gomo.rag.repository.UserRepository;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service managing first-time password setup, single-use token lifecycle,
 * token validation, password setup execution, and link regeneration.
 */
@Service
public class PasswordSetupService {

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupService.class);

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final int TOKEN_EXPIRY = 24;

    public PasswordSetupService(PasswordSetupTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }


    @Transactional
    public String createAndSendSetupToken(User user, String rawTemporaryPassword) {
        log.info("[PasswordSetupService] Generating setup token for user={}", user.getEmail());

        tokenRepository.invalidateAllActiveTokensForUser(user.getId());

        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY))
                .used(false)
                .build();

        tokenRepository.save(tokenEntity);

        try {
            emailService.sendOnboardingEmail(user, rawTemporaryPassword, rawToken);
        } catch (Exception e) {
            log.error("[PasswordSetupService] Failed to send onboarding email to {}: {}", user.getEmail(), e.getMessage());
        }

        return rawToken;
    }


    @Transactional
    public TokenValidationResponse validateSetupToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Setup token is required.")
                    .build();
        }

        String tokenHash = hashToken(rawToken.trim());
        PasswordSetupToken tokenEntity = tokenRepository.findByTokenHash(tokenHash).orElse(null);

        if (tokenEntity == null) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid setup token.")
                    .build();
        }

        if (tokenEntity.isUsed()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("This setup link has already been used.")
                    .build();
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("This setup link has expired. Please request a new setup link.")
                    .build();
        }

        User user = tokenEntity.getUser();
        if (user == null || !user.isActive()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("User account is disabled or not found.")
                    .build();
        }

        if (!user.isPasswordChangeRequired()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Password setup has already been completed for this account.")
                    .build();
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .message("Token is valid.")
                .build();
    }


    @Transactional
    public UserResponse setupPassword(SetupPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new IllegalArgumentException("Setup token is required.");
        }

        String rawToken = req.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordSetupToken tokenEntity = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password setup token."));

        if (tokenEntity.isUsed()) {
            throw new IllegalArgumentException("This password setup token has already been used.");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This password setup token has expired. Please request a new setup link.");
        }

        User user = tokenEntity.getUser();
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("User account is inactive or not found.");
        }

        // Validate new password rules
        String newPassword = req.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New permanent password cannot be blank.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Permanent password must be at least 6 characters long.");
        }

        if (req.getConfirmPassword() != null && !req.getConfirmPassword().isBlank()) {
            if (!newPassword.equals(req.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
        }

        // Ensure permanent password is different from the temporary password
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New permanent password must be different from the temporary password.");
        }

        // Optional check if temporary password was supplied
        if (req.getTemporaryPassword() != null && !req.getTemporaryPassword().isBlank()) {
            if (!passwordEncoder.matches(req.getTemporaryPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Temporary password does not match.");
            }
        }

        // Update user: encode new permanent password and clear flag
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        User savedUser = userRepository.save(user);

        // Mark token as used
        tokenEntity.setUsed(true);
        tokenEntity.setUsedAt(LocalDateTime.now());
        tokenRepository.save(tokenEntity);

        // Invalidate any remaining active tokens for this user
        tokenRepository.invalidateAllActiveTokensForUser(savedUser.getId());

        log.info("[PasswordSetupService] Successfully completed initial password setup for user={}", savedUser.getEmail());

        return AdminService.getUserResponse(savedUser);
    }

    /**
     * Securely regenerates an expired or lost setup link.
     * Assigns a fresh temporary password, invalidates previous tokens, and sends a new onboarding email.
     */
    @Transactional
    public void regenerateSetupLink(RegenerateSetupLinkRequest req) {
        User user = null;

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user = userRepository.findByEmail(req.getEmail().trim()).orElse(null);
        } else if (req.getToken() != null && !req.getToken().isBlank()) {
            String tokenHash = hashToken(req.getToken().trim());
            PasswordSetupToken tokenEntity = tokenRepository.findByTokenHash(tokenHash).orElse(null);
            if (tokenEntity != null) {
                user = tokenEntity.getUser();
            }
        }

        if (user == null) {
            throw new IllegalArgumentException("No account found matching the provided details.");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is inactive.");
        }

        if (!user.isPasswordChangeRequired()) {
            throw new IllegalStateException("Account has already completed password setup. Please log in directly or use password reset.");
        }

        // Generate a new secure temporary password
        String newTempPassword = generateSecureTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(newTempPassword));
        userRepository.save(user);

        // Generate new setup token and send email
        createAndSendSetupToken(user, newTempPassword);

        log.info("[PasswordSetupService] Regenerated setup link and temporary password for user={}", user.getEmail());
    }

    /**
     * Deterministic SHA-256 token hashing for secure DB storage and lookup.
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a cryptographically strong temporary password.
     */
    private String generateSecureTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Tmp@");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
