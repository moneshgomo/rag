package com.gomo.rag.repository;

import com.gomo.rag.dto.EmployeesList;
import com.gomo.rag.dto.ManagersList;
import com.gomo.rag.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for User.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = """
        SELECT id, name, email, contact_number, password_hash, role, team_id, is_active, password_change_required, created_at, updated_at
        FROM users
        WHERE email = :email
        LIMIT 1
        """, nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);


    @Query(value = """
        SELECT
            u.name AS name,
            u.email AS email,
            u.contact_number AS contactNumber,
            u.role AS role,
            t.name AS teamName
        FROM users AS u
        LEFT JOIN teams AS t
            ON u.id = t.manager_id
        WHERE u.role = :role
        ORDER BY u.name
        """, nativeQuery = true)
    List<ManagersList> findManagerAllByRole(@Param("role") String role);




    @Query("""
        SELECT u
        FROM User u
        LEFT JOIN FETCH u.team
        WHERE u.team.id = :teamId
    """)
    List<User> findMembersByTeamIdWithTeam(
            @Param("teamId") Long teamId
    );


    @Query(value = """
        SELECT
            u.name AS name,
            u.email AS email,
            u.contact_number AS contactNumber,
            u.role AS role,
            t.name AS teamName
        FROM users AS u
        LEFT JOIN teams AS t
            ON u.team_id = t.id
        WHERE u.role = :role
        ORDER BY u.name
        """, nativeQuery = true)
    List<EmployeesList> findEmployeesAllByRole(@Param("role") String role);


    @Query(value = """
        SELECT id, name, email, contact_number, password_hash, role, team_id, is_active, password_change_required, created_at, updated_at
        FROM users
        WHERE role = :role
        ORDER BY name
        """, nativeQuery = true)
    List<User> findAllByRole(@Param("role") String role);

    @Query(value = """
        SELECT id, name, email, contact_number, password_hash, role, team_id, is_active, password_change_required, created_at, updated_at
        FROM users
        WHERE team_id = :teamId
        ORDER BY name
        """, nativeQuery = true)
    List<User> findAllByTeamId(@Param("teamId") Long teamId);


    @Query(value = """
    SELECT
        u.id AS "id",
        u.name AS "name",
        u.email AS "email",
        u.contact_number AS "contactNumber",
        u.role AS "role",
        u.team_id AS "teamId",
        t.name AS "teamName",
        u.is_active AS "active",
        u.created_at AS "createdAt"
    FROM users u
    INNER JOIN teams t
        ON u.team_id = t.id
    WHERE u.team_id = :teamId
    ORDER BY u.name ASC
    """, nativeQuery = true)
    List<TeamMemberProjection> findTeamMemberDetails(
            @Param("teamId") Long teamId
    );


    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)",
            nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    @Query(value = """
        SELECT 
            u.id,
            u.name,
            u.email,
            u.contact_number,
            u.password_hash,
            u.role,
            u.team_id,
            u.is_active,
            u.password_change_required,
            u.created_at,
            u.updated_at
        FROM users u
        JOIN teams t ON t.id = u.team_id
        WHERE t.manager_id = :managerId
          AND u.role = 'EMPLOYEE'
        ORDER BY u.name
        """, nativeQuery = true)
    List<User> findEmployeesByManagerId(@Param("managerId") Long managerId);

    @Modifying
    @Query(value = "UPDATE users SET is_active = :active, updated_at = NOW() WHERE id = :id",
            nativeQuery = true)
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

    @Modifying
    @Query(value = "UPDATE users SET team_id = :teamId, updated_at = NOW() WHERE id = :id",
            nativeQuery = true)
    void updateTeam(@Param("id") Long id, @Param("teamId") Long teamId);

    @Query(value = """
        SELECT id, name, email, contact_number, password_hash, role, team_id, is_active, password_change_required, created_at, updated_at
        FROM users
        WHERE role = 'EMPLOYEE' AND team_id IS NULL AND is_active = true
        ORDER BY name ASC
        """, nativeQuery = true)
    List<User> findUnassignedEmployees();

    @Query(value = """
        SELECT id, name, email, contact_number, password_hash, role, team_id, is_active, password_change_required, created_at, updated_at
        FROM users
        WHERE role = 'EMPLOYEE' AND team_id IS NULL AND is_active = true
          AND LOWER(email) LIKE LOWER(CONCAT('%', :email, '%'))
        ORDER BY name ASC
        """, nativeQuery = true)
    List<User> searchUnassignedEmployeesByEmail(@Param("email") String email);
}