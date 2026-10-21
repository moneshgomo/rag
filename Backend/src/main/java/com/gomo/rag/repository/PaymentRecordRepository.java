package com.gomo.rag.repository;

import com.gomo.rag.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PaymentRecord.
 * All queries use nativeQuery = true (PostgreSQL SQL - no JPQL).
 */
@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    @Query(value = """
        SELECT id, reimbursement_id, razorpay_order_id, razorpay_payment_id, razorpay_signature, amount, currency, employee_id, employee_name, employee_contact, paid_by, paid_at
        FROM payment_records
        WHERE reimbursement_id = :reimbursementId
        LIMIT 1
        """, nativeQuery = true)
    Optional<PaymentRecord> findByReimbursementId(@Param("reimbursementId") Long reimbursementId);

    @Query(value = """
        SELECT id, reimbursement_id, razorpay_order_id, razorpay_payment_id, razorpay_signature, amount, currency, employee_id, employee_name, employee_contact, paid_by, paid_at
        FROM payment_records
        WHERE razorpay_order_id = :orderId
        LIMIT 1
        """, nativeQuery = true)
    Optional<PaymentRecord> findByRazorpayOrderId(@Param("orderId") String orderId);

    @Query(value = """
        SELECT id, reimbursement_id, razorpay_order_id, razorpay_payment_id, razorpay_signature, amount, currency, employee_id, employee_name, employee_contact, paid_by, paid_at
        FROM payment_records
        WHERE paid_by = :userId
        ORDER BY paid_at DESC
        """, nativeQuery = true)
    List<PaymentRecord> findAllByPaidBy(@Param("userId") Long userId);

    @Query(value = """
        SELECT id, reimbursement_id, razorpay_order_id, razorpay_payment_id, razorpay_signature, amount, currency, employee_id, employee_name, employee_contact, paid_by, paid_at
        FROM payment_records
        ORDER BY paid_at DESC
        """, nativeQuery = true)
    List<PaymentRecord> findAllOrderByPaidAtDesc();
}