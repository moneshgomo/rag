package com.gomo.rag.entity;

import com.gomo.rag.enums.ReimbursementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A money reclaim request submitted by an Employee with a PDF bill.
 *
 * Lifecycle:
 *   PENDING -> MANAGER_REVIEW -> FINANCE_REVIEW -> PAID
 *                                               -> REJECTED
 *
 * PDF is parsed by PDF.co (third-party API).
 * RAG analyses: bill text + employee reason vs reclaim policy document.
 * Human manager forwards to Finance; Finance triggers Razorpay payment.
 */
@Entity
@Table(name = "reimbursement_requests",
        indexes = {
                @Index(name = "idx_rr_employee", columnList = "employee_id"),
                @Index(name = "idx_rr_manager",  columnList = "manager_id"),
                @Index(name = "idx_rr_status",   columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReimbursementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Employee who submitted the reclaim. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    /** Manager who reviews and forwards the reclaim. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(nullable = false, precision = 10)
    private float amount;


    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "bill_url", columnDefinition = "TEXT")
    private String billUrl;

    @Column(name = "bill_extracted_text", columnDefinition = "TEXT")
    private String billExtractedText;

    @Column(name = "rag_analysis", columnDefinition = "TEXT")
    private String ragAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReimbursementStatus status = ReimbursementStatus.PENDING;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)   status   = ReimbursementStatus.PENDING;
        if (currency == null) currency = "INR";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}