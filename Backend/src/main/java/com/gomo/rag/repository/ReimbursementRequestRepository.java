package com.gomo.rag.repository;

import com.gomo.rag.entity.ReimbursementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ReimbursementRequest.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, Long> {

    @Query(value = "SELECT * FROM reimbursement_requests WHERE employee_id = :employeeId ORDER BY created_at DESC",
            nativeQuery = true)
    List<ReimbursementRequest> findAllByEmployeeId(@Param("employeeId") Long employeeId);

    @Query(value = "SELECT * FROM reimbursement_requests WHERE manager_id = :managerId ORDER BY created_at DESC",
            nativeQuery = true)
    List<ReimbursementRequest> findAllByManagerId(@Param("managerId") Long managerId);

    @Query(value = "SELECT * FROM reimbursement_requests WHERE manager_id = :managerId AND status = :status ORDER BY created_at DESC",
            nativeQuery = true)
    List<ReimbursementRequest> findAllByManagerIdAndStatus(@Param("managerId") Long managerId,
                                                            @Param("status") String status);

    @Query(value = "SELECT * FROM reimbursement_requests WHERE status = 'FINANCE_REVIEW' ORDER BY created_at ASC",
            nativeQuery = true)
    List<ReimbursementRequest> findAllPendingForFinance();

    /** Manager sets status to MANAGER_REVIEW and attaches the RAG analysis. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    status       = 'MANAGER_REVIEW',
                   rag_analysis = :ragAnalysis,
                   updated_at   = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void setManagerReview(@Param("id") Long id,
                          @Param("ragAnalysis") String ragAnalysis);

    /** Manager forwards request to Finance team. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    status     = 'FINANCE_REVIEW',
                   updated_at = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void forwardToFinance(@Param("id") Long id);

    /** Finance sets Razorpay order ID before payment. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    razorpay_order_id = :orderId,
                   updated_at        = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void setRazorpayOrderId(@Param("id") Long id,
                            @Param("orderId") String orderId);

    /** After Razorpay payment is verified - marks PAID. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    status              = 'PAID',
                   razorpay_payment_id = :paymentId,
                   processed_by        = :processedBy,
                   updated_at          = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void markPaid(@Param("id") Long id,
                  @Param("paymentId") String paymentId,
                  @Param("processedBy") Long processedBy);

    /** Reject - can be called by Manager or Finance. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    status           = 'REJECTED',
                   rejection_reason = :reason,
                   processed_by     = :processedBy,
                   updated_at       = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void markRejected(@Param("id") Long id,
                      @Param("reason") String reason,
                      @Param("processedBy") Long processedBy);

    /** Store bill_url and extracted text after PDF.co processing. */
    @Modifying
    @Query(value = """
            UPDATE reimbursement_requests
            SET    bill_url            = :billUrl,
                   bill_extracted_text = :extractedText,
                   updated_at          = NOW()
            WHERE  id = :id
            """, nativeQuery = true)
    void updateBillData(@Param("id") Long id,
                        @Param("billUrl") String billUrl,
                        @Param("extractedText") String extractedText);
}