package com.gomo.rag.controller;

import com.gomo.rag.dto.ForwardReimbursementRequest;
import com.gomo.rag.dto.ReimbursementResponse;
import com.gomo.rag.dto.RejectReimbursementRequest;
import com.gomo.rag.service.ReimbursementFlowService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
@CrossOrigin(origins = "*")
public class ReimbursementFlowController {

    private final ReimbursementFlowService reimbursementFlowService;

    public ReimbursementFlowController(ReimbursementFlowService reimbursementFlowService) {
        this.reimbursementFlowService = reimbursementFlowService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReimbursementResponse> submit(
            @RequestPart("bill") MultipartFile bill,
            @RequestParam("amount") float amount,
            @RequestParam(value = "currency", defaultValue = "INR") String currency,
            @RequestParam("reason") String reason) throws Exception {
        return ResponseEntity.ok(reimbursementFlowService.submitReimbursement(bill, amount, currency, reason));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReimbursementResponse>> getMy() {
        return ResponseEntity.ok(reimbursementFlowService.getMyReimbursements());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<List<ReimbursementResponse>> getPending() {
        return ResponseEntity.ok(reimbursementFlowService.getPendingForManager());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReimbursementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reimbursementFlowService.getReimbursement(id));
    }

    @PostMapping("/{id}/forward")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ReimbursementResponse> forwardToFinance(
            @PathVariable Long id,
            @RequestBody(required = false) ForwardReimbursementRequest request) {
        return ResponseEntity.ok(reimbursementFlowService.forwardToFinance(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'SUPER_ADMIN')")
    public ResponseEntity<ReimbursementResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectReimbursementRequest request) {
        return ResponseEntity.ok(reimbursementFlowService.rejectReimbursement(id, request));
    }
}