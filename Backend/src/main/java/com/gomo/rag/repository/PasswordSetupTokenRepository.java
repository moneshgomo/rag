package com.gomo.rag.repository;

import com.gomo.rag.entity.PasswordSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PasswordSetupToken.
 * Native PostgreSQL SQL queries consistent with repository patterns.
 */
@Repository
public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {

    @Query(value = """
        SELECT id, token_hash, user_id, expires_at, used, used_at, created_at
        FROM password_setup_tokens
        WHERE token_hash = :tokenHash
        LIMIT 1
        """, nativeQuery = true)
    Optional<PasswordSetupToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Query(value = """
        SELECT id, token_hash, user_id, expires_at, used, used_at, created_at
        FROM password_setup_tokens
        WHERE user_id = :userId AND used = false
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<PasswordSetupToken> findAllActiveByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT id, token_hash, user_id, expires_at, used, used_at, created_at
        FROM password_setup_tokens
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PasswordSetupToken> findLatestByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE password_setup_tokens SET used = true, used_at = NOW() WHERE user_id = :userId AND used = false",
            nativeQuery = true)
    void invalidateAllActiveTokensForUser(@Param("userId") Long userId);
}
