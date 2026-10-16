package com.gomo.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks each employee's leave quota per calendar year.
 * One record per (employee, year) - enforced by a unique constraint.
 * remainingDays = totalDays - usedDays (maintained by service layer).
 */
@Entity
@Table(name = "leave_balance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_leave_balance_employee_year",
                        columnNames = {"employee_id", "year"}
                )
        },
        indexes = {
                @Index(name = "idx_lb_employee", columnList = "employee_id"),
                @Index(name = "idx_lb_year",     columnList = "year")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private Integer year;

    /** Organisation-wide default annual leave days. Default: 24. */
    @Column(name = "total_days", nullable = false)
    @Builder.Default
    private Integer totalDays = 24;

    /** Days consumed so far this year (incremented on leave approval). */
    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private Integer usedDays = 0;

    /** Remaining = totalDays - usedDays. Kept in sync by service. */
    @Column(name = "remaining_days", nullable = false)
    @Builder.Default
    private Integer remainingDays = 24;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}