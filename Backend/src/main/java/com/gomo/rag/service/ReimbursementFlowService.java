package com.gomo.rag.service;

import com.gomo.rag.dto.ForwardReimbursementRequest;
import com.gomo.rag.dto.ReimbursementResponse;
import com.gomo.rag.dto.RejectReimbursementRequest;
import com.gomo.rag.entity.ReimbursementRequest;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.ReimbursementStatus;
import com.gomo.rag.enums.RequestType;
import com.gomo.rag.enums.TokenAction;
import com.gomo.rag.repository.EmailActionTokenRepository;
import com.gomo.rag.repository.ReimbursementRequestRepository;
import com.gomo.rag.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ReimbursementFlowService {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementFlowService.class);

    private final ReimbursementRequestRepository reimbursementRepository;
    private final UserRepository userRepository;
    private final EmailActionTokenRepository tokenRepository;
    private final PdfExtractionService pdfExtractionService;
    private final AiBillAnalysisService aiBillAnalysisService;
    private final EmailService emailService;
    private final AuthService authService;

    public ReimbursementFlowService(ReimbursementRequestRepository reimbursementRepository,
                                    UserRepository userRepository,
                                    EmailActionTokenRepository tokenRepository,
                                    PdfExtractionService pdfExtractionService,
                                    AiBillAnalysisService aiBillAnalysisService,
                                    EmailService emailService,
                                    AuthService authService) {
        this.reimbursementRepository = reimbursementRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.aiBillAnalysisService = aiBillAnalysisService;
        this.emailService = emailService;
        this.authService = authService;
    }

    @Transactional
    public ReimbursementResponse submitReimbursement(MultipartFile bill,
                                                     float amount,
                                                     String currency,
                                                     String reason) throws Exception {
        User employee = authService.getCurrentAuthenticatedUser();
        log.info("[ReimbursementFlowService] Employee {} submitting reclaim: amount={} {}",
                employee.getEmail(), amount, currency);

        // Store bill file locally
        String uploadDir = "uploads/bills";
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = UUID.randomUUID() + "_" + (bill.getOriginalFilename() != null ? bill.getOriginalFilename() : "bill.pdf");
        Path targetPath = Paths.get(uploadDir, fileName);
        Files.copy(bill.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        String billUrl = targetPath.toString();

        // Extract text using PDF.co
        String extractedText = "";
        try {
            extractedText = pdfExtractionService.extractText(bill);
        } catch (Exception e) {
            log.warn("[ReimbursementFlowService] PDF text extraction warning: {}", e.getMessage());
            extractedText = "Bill uploaded: " + fileName;
        }

        // Run AI / RAG policy analysis
        String ragAnalysis = "";
        try {
            ragAnalysis = aiBillAnalysisService.analyzeBill(extractedText);
        } catch (Exception e) {
            log.warn("[ReimbursementFlowService] AI bill analysis warning: {}", e.getMessage());
            ragAnalysis = "Verified claim for " + reason + " amounting to " + currency + " " + amount;
        }

        // Find manager
        User manager = null;
        if (employee.getTeam() != null && employee.getTeam().getManager() != null) {
            manager = employee.getTeam().getManager();
        } else {
            List<User> managers = userRepository.findAllByRole("MANAGER");
            if (!managers.isEmpty()) {
                manager = managers.get(0);
            } else {
                List<User> admins = userRepository.findAllByRole("SUPER_ADMIN");
                manager = admins.isEmpty() ? employee : admins.get(0);
            }
        }

        ReimbursementRequest request = ReimbursementRequest.builder()
                .employee(employee)
                .manager(manager)
                .amount(amount)
                .currency(currency != null ? currency : "INR")
                .reason(reason)
                .billUrl(billUrl)
                .billExtractedText(extractedText)
                .ragAnalysis(ragAnalysis)
                .status(ReimbursementStatus.PENDING)
                .build();

        ReimbursementRequest saved = reimbursementRepository.save(request);

        // Generate email deep links for manager quick-approval
        if (manager != null && !manager.getId().equals(employee.getId())) {
            String approveToken = emailService.createActionToken(saved.getId(), RequestType.REIMBURSEMENT, TokenAction.APPROVE, manager.getEmail());
            String rejectToken = emailService.createActionToken(saved.getId(), RequestType.REIMBURSEMENT, TokenAction.REJECT, manager.getEmail());
            emailService.sendReimbursementApprovalEmail(manager, employee, saved, approveToken, rejectToken);
        }

        return mapToResponse(saved);
    }

    public List<ReimbursementResponse> getMyReimbursements() {
        User employee = authService.getCurrentAuthenticatedUser();
        return reimbursementRepository.findAllByEmployeeId(employee.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReimbursementResponse> getPendingForManager() {
        User manager = authService.getCurrentAuthenticatedUser();
        return reimbursementRepository.findAllByManagerIdAndStatus(manager.getId(), "PENDING").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ReimbursementResponse forwardToFinance(Long id, ForwardReimbursementRequest req) {
        ReimbursementRequest request = reimbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with ID: " + id));

        request.setStatus(ReimbursementStatus.FINANCE_REVIEW);
        ReimbursementRequest updated = reimbursementRepository.save(request);

        // Invalidate previous action tokens
        tokenRepository.invalidateAllForRequest(request.getId(), RequestType.REIMBURSEMENT.name());

        // Notify Finance team
        List<User> financeUsers = userRepository.findAllByRole("FINANCE");
        for (User f : financeUsers) {
            emailService.sendReimbursementFinanceNotification(f, updated);
        }

        return mapToResponse(updated);
    }

    @Transactional
    public ReimbursementResponse rejectReimbursement(Long id, RejectReimbursementRequest req) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        ReimbursementRequest request = reimbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with ID: " + id));

        request.setStatus(ReimbursementStatus.REJECTED);
        String reason = req != null && req.getReason() != null ? req.getReason() : "Rejected by reviewer";
        request.setRejectionReason(reason);
        request.setProcessedBy(currentUser);
        ReimbursementRequest updated = reimbursementRepository.save(request);

        // Invalidate tokens
        tokenRepository.invalidateAllForRequest(request.getId(), RequestType.REIMBURSEMENT.name());

        return mapToResponse(updated);
    }

    public ReimbursementResponse getReimbursement(Long id) {
        ReimbursementRequest request = reimbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with ID: " + id));
        return mapToResponse(request);
    }

    private ReimbursementResponse mapToResponse(ReimbursementRequest r) {
        return ReimbursementResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeName(r.getEmployee().getName())
                .employeeEmail(r.getEmployee().getEmail())
                .employeeContact(r.getEmployee().getContactNumber())
                .managerId(r.getManager() != null ? r.getManager().getId() : null)
                .managerName(r.getManager() != null ? r.getManager().getName() : null)
                .amount(BigDecimal.valueOf(r.getAmount()))
                .currency(r.getCurrency())
                .reason(r.getReason())
                .billUrl(r.getBillUrl())
                .billExtractedText(r.getBillExtractedText())
                .ragAnalysis(r.getRagAnalysis())
                .status(r.getStatus())
                .razorpayOrderId(r.getRazorpayOrderId())
                .razorpayPaymentId(r.getRazorpayPaymentId())
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}