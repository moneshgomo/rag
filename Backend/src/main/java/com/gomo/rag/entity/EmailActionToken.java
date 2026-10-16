package com.gomo.rag.entity;

import com.gomo.rag.enums.RequestType;
import com.gomo.rag.enums.TokenAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Secure, one-time-use token embedded in email deep links.
 *
 * When a Manager/Finance member receives an email with an approve/reject/review link,
 * the link contains this token (UUID). On click the backend validates:
 *   1. Token exists
 *   2. Not expired (expires_at > now)
 *   3. Not already used
 *
 * Then the action is executed and used is flipped to true.
 */
@Entity
@Table(name = "email_action_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailActionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID string - embedded as a path/query param in email deep links. */
    @Column(nullable = false, unique = true, length = 36)
    @Builder.Default
    private String token = UUID.randomUUID().toString();

    /** Whether this token is for a LEAVE or REIMBURSEMENT request. */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private RequestType requestType;

    /**
     * ID of the linked record:
     *   - requestType = LEAVE         -> leave_requests.id
     *   - requestType = REIMBURSEMENT -> reimbursement_requests.id
     */
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    /** What action this token authorises: APPROVE, REJECT, or REVIEW. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenAction action;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    /** Token is invalid after this timestamp (default: 24 hours from creation). */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Flipped to true the moment the token is consumed. Prevents replay. */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (token == null) token = UUID.randomUUID().toString();
    }
}