package com.gomo.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Secure, single-use token for first-time password setup.
 *
 * Security Architecture:
 * - Only the SHA-256 hash of the random setup token is stored in the database.
 * - The raw token is sent to the user via email and never persisted.
 * - Tokens have a strict expiration time.
 * - Single-use enforcement via the 'used' flag and 'used_at' timestamp.
 */
@Entity
@Table(name = "password_setup_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordSetupToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stored as a secure SHA-256 hash (64 hex characters) of the single-use token.
     * Raw token is delivered via email and never stored in plaintext in the database.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Token expiration timestamp. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Flipped to true once consumed to prevent replay attacks. */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
