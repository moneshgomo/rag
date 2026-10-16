package com.gomo.rag.service;

import com.gomo.rag.dto.ReimbursementRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the core PDF-extraction + AI-analysis part of a money reclaim.
 * Full DB persistence (save to reimbursement_requests) will be wired in the
 * next phase once JWT auth and user-context are set up.
 */
@Service
public class ReimbursementService {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementService.class);

    private final PdfExtractionService pdfExtractionService;
    private final AiBillAnalysisService aiBillAnalysisService;

    public ReimbursementService(PdfExtractionService pdfExtractionService,
                                AiBillAnalysisService aiBillAnalysisService) {
        this.pdfExtractionService = pdfExtractionService;
        this.aiBillAnalysisService = aiBillAnalysisService;
    }

    /**
     * Extracts text from the PDF bill and runs AI analysis on it.
     *
     * @param request DTO containing the MultipartFile bill + employee-supplied fields.
     * @return a map with billText, billAnalysis, and all request fields.
     */
    public Map<String, Object> process(ReimbursementRequest request) throws Exception {

        log.info("[ReimbursementService] Processing reclaim for employee={} amount={}",
                request.getEmployeeName(), request.getAmount());

        String billText = pdfExtractionService.extractText(request.getBill());
        log.debug("[ReimbursementService] Extracted {} chars from PDF bill.", billText.length());

        String billAnalysis = aiBillAnalysisService.analyzeBill(billText);
        log.debug("[ReimbursementService] AI bill analysis complete.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount",       request.getAmount());
        result.put("currency",     request.getCurrency() != null ? request.getCurrency() : "INR");
        result.put("reason",       request.getReason());
        result.put("employeeName", request.getEmployeeName());
        result.put("tlName",       request.getTlName());
        result.put("teamName",     request.getTeamName());
        result.put("billText",     billText);
        result.put("billAnalysis", billAnalysis);

        return result;
    }
}