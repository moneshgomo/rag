package com.gomo.rag.controller;

import com.gomo.rag.dto.PayableReimbursementResponse;
import com.gomo.rag.dto.PaymentOrderResponse;
import com.gomo.rag.dto.PaymentRecordResponse;
import com.gomo.rag.dto.PaymentVerifyRequest;
import com.gomo.rag.service.PaymentFlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('FINANCE', 'SUPER_ADMIN', 'MANAGER')")
public class PaymentFlowController {

    private final PaymentFlowService paymentFlowService;

    public PaymentFlowController(PaymentFlowService paymentFlowService) {
        this.paymentFlowService = paymentFlowService;
    }

    /**
     * Shows all reimbursements in FINANCE_REVIEW with exact amount, emp name, id, contact number.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PayableReimbursementResponse>> getPayableReimbursements() {
        return ResponseEntity.ok(paymentFlowService.getPayableReimbursements());
    }

    /**
     * Triggers Razorpay order creation for verified reimbursement.
     */
    @PostMapping("/order/{reimbursementId}")
    public ResponseEntity<PaymentOrderResponse> createOrder(@PathVariable Long reimbursementId) {
        return ResponseEntity.ok(paymentFlowService.createPaymentOrder(reimbursementId));
    }

    /**
     * Verifies payment and immutably records amount, emp name, id, and contact number till the end.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentRecordResponse> verifyPayment(@RequestBody PaymentVerifyRequest request) throws Exception {
        return ResponseEntity.ok(paymentFlowService.verifyPaymentAndRecord(request));
    }

    /**
     * Lists audit payment history.
     */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentRecordResponse>> getPaymentHistory() {
        return ResponseEntity.ok(paymentFlowService.getPaymentHistory());
    }
}