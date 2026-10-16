package com.gomo.rag.controller;

import com.gomo.rag.dto.ReimbursementRequest;
import com.gomo.rag.service.ReimbursementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Legacy endpoint kept for standalone PDF-extract + AI-analysis testing.
 * The full authenticated reimbursement flow will be in ReimbursementFlowController
 * (to be created in the next phase with JWT security).
 */
@RestController
@RequestMapping("/reimbursement")
public class ReimbursementController {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementController.class);

    private final ReimbursementService reimbursementService;

    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * POST /reimbursement/process
     * Accepts a PDF bill and request metadata, returns extracted text + AI analysis.
     * Multipart: bill (PDF file) + request (JSON part).
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> process(
            @RequestPart("bill") MultipartFile bill,
            @RequestPart("request") ReimbursementRequest request) throws Exception {

        request.setBill(bill);
        Map<String, Object> result = reimbursementService.process(request);
        log.info("[ReimbursementController] process complete for employee={}",
                request.getEmployeeName());
        return ResponseEntity.ok(result);
    }
}
