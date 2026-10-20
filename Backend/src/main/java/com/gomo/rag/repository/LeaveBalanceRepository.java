package com.gomo.rag.repository;

import com.gomo.rag.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for LeaveBalance.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @Query(value = """
        SELECT id, employee_id, year, total_days, used_days, remaining_days, updated_at
        FROM leave_balance
        WHERE employee_id = :employeeId AND year = :year
        LIMIT 1
        """, nativeQuery = true)
    Optional<LeaveBalance> findByEmployeeIdAndYear(@Param("employeeId") Long employeeId,
                                                    @Param("year") int year);

    @Modifying
    @Query(value = """
            UPDATE leave_balance
            SET    used_days      = used_days + :days,
                   remaining_days = remaining_days - :days,
                   updated_at     = NOW()
            WHERE  employee_id = :employeeId
              AND  year        = :year
            """, nativeQuery = true)
    void deductDays(@Param("employeeId") Long employeeId,
                    @Param("year") int year,
                    @Param("days") int days);

    @Modifying
    @Query(value = """
            UPDATE leave_balance
            SET    used_days      = used_days - :days,
                   remaining_days = remaining_days + :days,
                   updated_at     = NOW()
            WHERE  employee_id = :employeeId
              AND  year        = :year
            """, nativeQuery = true)
    void restoreDays(@Param("employeeId") Long employeeId,
                     @Param("year") int year,
                     @Param("days") int days);
}