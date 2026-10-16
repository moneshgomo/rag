package com.gomo.rag.service;

import com.gomo.rag.dto.PayableReimbursementResponse;
import com.gomo.rag.dto.PaymentOrderResponse;
import com.gomo.rag.dto.PaymentRecordResponse;
import com.gomo.rag.dto.PaymentVerifyRequest;
import com.gomo.rag.entity.PaymentRecord;
import com.gomo.rag.entity.ReimbursementRequest;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.ReimbursementStatus;
import com.gomo.rag.repository.PaymentRecordRepository;
import com.gomo.rag.repository.ReimbursementRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PaymentFlowService {

    private static final Logger log = LoggerFactory.getLogger(PaymentFlowService.class);

    private final ReimbursementRequestRepository reimbursementRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final AuthService authService;

    public PaymentFlowService(ReimbursementRequestRepository reimbursementRepository,
                              PaymentRecordRepository paymentRecordRepository,
                              PaymentService paymentService,
                              EmailService emailService,
                              AuthService authService) {
        this.reimbursementRepository = reimbursementRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.authService = authService;
    }

    /**
     * Lists all reimbursements verified by Manager / in FINANCE_REVIEW.
     * Shows exact amount needed to pay, employee name, employee ID, and contact number.
     */
    public List<PayableReimbursementResponse> getPayableReimbursements() {
        return reimbursementRepository.findAllPendingForFinance().stream()
                .map(r -> PayableReimbursementResponse.builder()
                        .reimbursementId(r.getId())
                        .employeeId(r.getEmployee().getId())
                        .employeeName(r.getEmployee().getName())
                        .employeeEmail(r.getEmployee().getEmail())
                        .employeeContactNumber(r.getEmployee().getContactNumber())
                        .amount(r.getAmount())
                        .currency(r.getCurrency())
                        .reason(r.getReason())
                        .status(r.getStatus())
                        .managerName(r.getManager() != null ? r.getManager().getName() : "N/A")
                        .ragAnalysis(r.getRagAnalysis())
                        .razorpayOrderId(r.getRazorpayOrderId())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Creates a Razorpay order for the exact amount of the verified reimbursement.
     * Retains employee details: ID, name, contact number, email, amount.
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(Long reimbursementId) {
        ReimbursementRequest req = reimbursementRepository.findById(reimbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found ID: " + reimbursementId));

        if (req.getStatus() != ReimbursementStatus.FINANCE_REVIEW) {
            throw new IllegalStateException("Reimbursement is not in FINANCE_REVIEW status (Current: " + req.getStatus() + ")");
        }

        User employee = req.getEmployee();
        // Convert amount to paise (e.g. INR 1250.00 -> 125000 paise)
        int amountInPaise = (int) req.getAmount();

        String orderId = paymentService.createOrderId(amountInPaise, req.getCurrency());
        req.setRazorpayOrderId(orderId);
        reimbursementRepository.save(req);

        log.info("[PaymentFlow] Created order={} for reimbursementId={} amount={} empId={} empName={} empContact={}",
                orderId, reimbursementId, req.getAmount(), employee.getId(), employee.getName(), employee.getContactNumber());

        return PaymentOrderResponse.builder()
                .razorpayOrderId(orderId)
                .reimbursementId(req.getId())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .employeeContactNumber(employee.getContactNumber())
                .employeeEmail(employee.getEmail())
                .razorpayKeyId(paymentService.getKeyId())
                .build();
    }

    /**
     * Verifies the Razorpay payment signature, marks the reimbursement as PAID,
     * and immutably records the payment with employee name, id, contact number, and exact amount.
     */
    @Transactional
    public PaymentRecordResponse verifyPaymentAndRecord(PaymentVerifyRequest req) throws Exception {
        User financeUser = authService.getCurrentAuthenticatedUser();

        ReimbursementRequest reimbursement = reimbursementRepository.findById(req.getReimbursementId())
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with ID: " + req.getReimbursementId()));

        if (reimbursement.getStatus() == ReimbursementStatus.PAID) {
            throw new IllegalStateException("Reimbursement is already PAID");
        }

        boolean verified = paymentService.verifyPayment(
                req.getRazorpayPaymentId(),
                req.getRazorpayOrderId(),
                req.getRazorpaySignature()
        );

        if (!verified) {
            throw new IllegalArgumentException("Razorpay signature verification failed");
        }

        User employee = reimbursement.getEmployee();

        // Save immutable PaymentRecord with tracked employee details
        PaymentRecord paymentRecord = PaymentRecord.builder()
                .reimbursement(reimbursement)
                .razorpayOrderId(req.getRazorpayOrderId())
                .razorpayPaymentId(req.getRazorpayPaymentId())
                .razorpaySignature(req.getRazorpaySignature())
                .amount(reimbursement.getAmount())
                .currency(reimbursement.getCurrency())
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .employeeContact(employee.getContactNumber())
                .paidBy(financeUser)
                .build();

        PaymentRecord savedPayment = paymentRecordRepository.save(paymentRecord);

        // Update reimbursement request status
        reimbursement.setStatus(ReimbursementStatus.PAID);
        reimbursement.setRazorpayOrderId(req.getRazorpayOrderId());
        reimbursement.setRazorpayPaymentId(req.getRazorpayPaymentId());
        reimbursement.setProcessedBy(financeUser);
        reimbursementRepository.save(reimbursement);

        log.info("[PaymentFlow] Successfully paid reimbursementId={} amount={} empId={} empName={} empContact={}",
                reimbursement.getId(), reimbursement.getAmount(), employee.getId(), employee.getName(), employee.getContactNumber());

        // Send payment confirmation email to employee
        emailService.sendPaymentReceiptEmail(employee, reimbursement, savedPayment);

        return PaymentRecordResponse.builder()
                .id(savedPayment.getId())
                .reimbursementId(reimbursement.getId())
                .razorpayOrderId(savedPayment.getRazorpayOrderId())
                .razorpayPaymentId(savedPayment.getRazorpayPaymentId())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .employeeId(savedPayment.getEmployeeId())
                .employeeName(savedPayment.getEmployeeName())
                .employeeContact(savedPayment.getEmployeeContact())
                .paidByEmail(financeUser.getEmail())
                .paidAt(savedPayment.getPaidAt())
                .build();
    }

    /**
     * Lists history of all completed payments.
     */
    public List<PaymentRecordResponse> getPaymentHistory() {
        return paymentRecordRepository.findAllOrderByPaidAtDesc().stream()
                .map(p -> PaymentRecordResponse.builder()
                        .id(p.getId())
                        .reimbursementId(p.getReimbursement().getId())
                        .razorpayOrderId(p.getRazorpayOrderId())
                        .razorpayPaymentId(p.getRazorpayPaymentId())
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .employeeId(p.getEmployeeId())
                        .employeeName(p.getEmployeeName())
                        .employeeContact(p.getEmployeeContact())
                        .paidByEmail(p.getPaidBy() != null ? p.getPaidBy().getEmail() : "N/A")
                        .paidAt(p.getPaidAt())
                        .build())
                .toList();
    }
}