package com.gomo.rag.repository;

import com.gomo.rag.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for LeaveRequest.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {



    @Query(value = """
    SELECT
        id AS leave_id,
        reason,
        status,
        start_date,
        end_date ,
        days,
        rag_decision,
        created_at,
        manager_id,
        leave_type
    FROM leave_requests
    WHERE employee_id = :employeeId
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<Object[]> getLeaveRequestBy(@Param("employeeId") Long employeeId);

    @Query(value = "SELECT * FROM leave_requests WHERE manager_id = :managerId ORDER BY created_at DESC",
            nativeQuery = true)
    List<LeaveRequest> findAllByManagerId(@Param("managerId") Long managerId);

    @Query(value = "SELECT * FROM leave_requests WHERE manager_id = :managerId AND status = :status ORDER BY created_at DESC",
            nativeQuery = true)
    List<LeaveRequest> findAllByManagerIdAndStatus(@Param("managerId") Long managerId,
                                                    @Param("status") String status);

    @Query(value = "SELECT * FROM leave_requests WHERE manager_id = :managerId AND status = 'PENDING' ORDER BY created_at ASC",
            nativeQuery = true)
    List<LeaveRequest> findPendingByManagerId(@Param("managerId") Long managerId);

    @Modifying
    @Query(value = """
            UPDATE leave_requests
            SET    status          = :status,
                   manager_comment = :comment,
                   rag_decision    = :ragDecision,
                   updated_at      = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void updateDecision(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("comment") String comment,
                        @Param("ragDecision") String ragDecision);

    @Query(value = """
            SELECT COUNT(*) FROM leave_requests
            WHERE  employee_id = :employeeId
              AND  status      = 'APPROVED'
              AND  start_date <= CAST(:endDate AS date)
              AND  end_date   >= CAST(:startDate AS date)
            """, nativeQuery = true)
    int countOverlappingApproved(@Param("employeeId") Long employeeId,
                                  @Param("startDate") String startDate,
                                  @Param("endDate")   String endDate);
}