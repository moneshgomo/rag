package com.gomo.rag.repository;

import com.gomo.rag.entity.EmailActionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for EmailActionToken.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface EmailActionTokenRepository extends JpaRepository<EmailActionToken, Long> {

    @Query(value = """
        SELECT id, token, request_type, request_id, action, recipient_email, expires_at, used, created_at
        FROM email_action_tokens
        WHERE token = :token
        LIMIT 1
        """, nativeQuery = true)
    Optional<EmailActionToken> findByToken(@Param("token") String token);

    /** Marks a token as used - called immediately after the action is executed. Prevents replay attacks. */
    @Modifying
    @Query(value = "UPDATE email_action_tokens SET used = true WHERE token = :token",
            nativeQuery = true)
    void markUsed(@Param("token") String token);

    /** Invalidates all unused tokens for a request when it is closed (approved/rejected). */
    @Modifying
    @Query(value = """
            UPDATE email_action_tokens
            SET    used = true
            WHERE  request_id   = :requestId
              AND  request_type = :requestType
              AND  used         = false
            """, nativeQuery = true)
    void invalidateAllForRequest(@Param("requestId") Long requestId,
                                  @Param("requestType") String requestType);
}