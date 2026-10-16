package com.gomo.rag.service;

import com.gomo.rag.entity.EmailActionToken;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenActionService {

    private static final Logger log = LoggerFactory.getLogger(TokenActionService.class);

    private final EmailActionTokenRepository tokenRepository;
    private final ReimbursementRequestRepository reimbursementRepository;
    private final UserRepository userRepository;
    private final LeaveService leaveService;
    private final EmailService emailService;

    public TokenActionService(EmailActionTokenRepository tokenRepository,
                              ReimbursementRequestRepository reimbursementRepository,
                              UserRepository userRepository,
                              LeaveService leaveService,
                              EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.reimbursementRepository = reimbursementRepository;
        this.userRepository = userRepository;
        this.leaveService = leaveService;
        this.emailService = emailService;
    }

    @Transactional
    public String executeAction(String token) {
        EmailActionToken tokenEntity = tokenRepository.findByToken(token)
                .orElse(null);

        if (tokenEntity == null) {
            return errorHtml("Invalid Token", "This action token does not exist or has expired.");
        }

        if (tokenEntity.isUsed()) {
            return errorHtml("Token Already Used", "This request link has already been used and cannot be reused.");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return errorHtml("Token Expired", "This action link has expired. Please check your FinFlow portal instead.");
        }

        try {
            if (tokenEntity.getRequestType() == RequestType.LEAVE) {
                if (tokenEntity.getAction() == TokenAction.APPROVE) {
                    leaveService.approveLeave(tokenEntity.getRequestId(), null);
                    tokenRepository.markUsed(token);
                    return successHtml("Leave Approved", "The leave request #" + tokenEntity.getRequestId() + " has been successfully approved.");
                } else {
                    leaveService.rejectLeave(tokenEntity.getRequestId(), null);
                    tokenRepository.markUsed(token);
                    return successHtml("Leave Rejected", "The leave request #" + tokenEntity.getRequestId() + " has been rejected.");
                }
            } else if (tokenEntity.getRequestType() == RequestType.REIMBURSEMENT) {
                ReimbursementRequest req = reimbursementRepository.findById(tokenEntity.getRequestId())
                        .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with ID: " + tokenEntity.getRequestId()));

                if (tokenEntity.getAction() == TokenAction.APPROVE) {
                    req.setStatus(ReimbursementStatus.FINANCE_REVIEW);
                    reimbursementRepository.save(req);
                    tokenRepository.markUsed(token);
                    tokenRepository.invalidateAllForRequest(req.getId(), RequestType.REIMBURSEMENT.name());

                    // Notify finance team
                    List<User> financeUsers = userRepository.findAllByRole("FINANCE");
                    for (User f : financeUsers) {
                        emailService.sendReimbursementFinanceNotification(f, req);
                    }

                    return successHtml("Reimbursement Verified & Forwarded",
                            "Reimbursement request #" + req.getId() + " for " + req.getCurrency() + " " + req.getAmount()
                                    + " has been verified and forwarded to the Finance team for disbursement.");
                } else {
                    req.setStatus(ReimbursementStatus.REJECTED);
                    req.setRejectionReason("Rejected via email quick action");
                    reimbursementRepository.save(req);
                    tokenRepository.markUsed(token);
                    tokenRepository.invalidateAllForRequest(req.getId(), RequestType.REIMBURSEMENT.name());

                    return successHtml("Reimbursement Rejected",
                            "Reimbursement request #" + req.getId() + " has been marked as rejected.");
                }
            }

            return errorHtml("Unknown Request Type", "Unsupported request type.");
        } catch (Exception e) {
            log.error("[TokenActionService] Error executing token action", e);
            return errorHtml("Action Failed", e.getMessage());
        }
    }

    private String successHtml(String title, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s - FinFlow</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                        .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 480px; text-align: center; border-top: 6px solid #22c55e; }
                        h1 { color: #0f172a; margin-top: 10px; font-size: 24px; }
                        p { color: #64748b; line-height: 1.6; font-size: 16px; }
                        .badge { display: inline-block; background: #dcfce7; color: #15803d; padding: 6px 12px; border-radius: 999px; font-weight: 600; font-size: 14px; margin-bottom: 12px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="badge">&#10003; Action Completed</div>
                        <h1>%s</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(title, title, message);
    }

    private String errorHtml(String title, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s - FinFlow</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                        .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 480px; text-align: center; border-top: 6px solid #ef4444; }
                        h1 { color: #0f172a; margin-top: 10px; font-size: 24px; }
                        p { color: #64748b; line-height: 1.6; font-size: 16px; }
                        .badge { display: inline-block; background: #fee2e2; color: #b91c1c; padding: 6px 12px; border-radius: 999px; font-weight: 600; font-size: 14px; margin-bottom: 12px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="badge">&#9888; Notice</div>
                        <h1>%s</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(title, title, message);
    }
}