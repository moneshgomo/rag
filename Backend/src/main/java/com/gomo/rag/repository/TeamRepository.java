package com.gomo.rag.repository;

import com.gomo.rag.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Team.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query(value = """
        SELECT id, name, description, manager_id, created_at
        FROM teams
        WHERE name = :name
        LIMIT 1
        """, nativeQuery = true)
    Optional<Team> findByName(@Param("name") String name);

    @Query(value = """
        SELECT id, name, description, manager_id, created_at
        FROM teams
        WHERE manager_id = :managerId
        ORDER BY name
        """, nativeQuery = true)
    List<Team> findAllByManagerId(@Param("managerId") Long managerId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM teams WHERE name = :name)",
            nativeQuery = true)
    boolean existsByName(@Param("name") String name);

    @Query(value = """
        SELECT id, name, description, manager_id, created_at
        FROM teams
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<Team> findAllOrderByCreatedAtDesc();
}