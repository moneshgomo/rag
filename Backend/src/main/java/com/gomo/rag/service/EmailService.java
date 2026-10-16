package com.gomo.rag.service;

import com.gomo.rag.entity.*;
import com.gomo.rag.enums.RequestType;
import com.gomo.rag.enums.TokenAction;
import com.gomo.rag.repository.EmailActionTokenRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailActionTokenRepository tokenRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@finflow.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:FinFlow System}")
    private String fromName;

    @Value("${app.token.expiry-hours:24}")
    private int tokenExpiryHours;

    public EmailService(EmailActionTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }


    @Transactional
    public String createActionToken(Long requestId, RequestType requestType, TokenAction action, String recipientEmail) {
        String token = UUID.randomUUID().toString();
        EmailActionToken tokenEntity = EmailActionToken.builder()
                .token(token)
                .requestId(requestId)
                .requestType(requestType)
                .action(action)
                .recipientEmail(recipientEmail)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpiryHours))
                .used(false)
                .build();

        tokenRepository.save(tokenEntity);
        log.info("[EmailService] Generated {} token for requestId={} type={} recipient={}",
                action, requestId, requestType, recipientEmail);
        return token;
    }


//    public void sendLeaveApprovalEmail(User manager, User employee, LeaveRequest leave, String approveToken, String rejectToken) {
//        String approveUrl = baseUrl + "/api/token/" + approveToken + "/action";
//        String rejectUrl = baseUrl + "/api/token/" + rejectToken + "/action";
//
//        String subject = "[Action Required] Leave Request from " + employee.getName();
//        String html = """
//                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
//                    <h2 style="color: #2b3a4a;">Leave Request for Review</h2>
//                    <p>Dear <strong>%s</strong>,</p>
//                    <p>Your team member <strong>%s</strong> has submitted a leave request:</p>
//                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
//                        <tr><td style="padding: 8px; font-weight: bold; width: 140px;">Leave Type:</td><td>%s</td></tr>
//                        <tr><td style="padding: 8px; font-weight: bold;">Dates:</td><td>%s to %s (%d days)</td></tr>
//                        <tr><td style="padding: 8px; font-weight: bold;">Reason:</td><td>%s</td></tr>
//                        <tr><td style="padding: 8px; font-weight: bold;">RAG Policy Check:</td><td style="color: #4a5568;">%s</td></tr>
//                    </table>
//                    <div style="text-align: center; margin: 30px 0;">
//                        <a href="%s" style="background-color: #22c55e; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; margin-right: 15px;">Approve Leave</a>
//                        <a href="%s" style="background-color: #ef4444; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">Reject Leave</a>
//                    </div>
//                    <p style="font-size: 12px; color: #718096; text-align: center;">These links are single-use and will expire in %d hours.</p>
//                </div>
//                """.formatted(
//                manager.getName(),
//                employee.getName(),
//                leave.getLeaveType(),
//                leave.getStartDate(),
//                leave.getEndDate(),
//                leave.getDaysCount(),
//                leave.getReason(),
//                leave.getRagDecision() != null ? leave.getRagDecision() : "Standard policy limits apply.",
//                approveUrl,
//                rejectUrl,
//                tokenExpiryHours
//        );
//
//        sendHtmlEmail(manager.getEmail(), subject, html);
//    }


    public void sendLeaveApprovalEmail(
            User manager,
            User employee,
            LeaveRequest leave,
            String approveToken,
            String rejectToken) {

        String approveUrl = frontendUrl
                + "/leave-action?token=" + approveToken
                + "&action=approve";

        String rejectUrl = frontendUrl
                + "/leave-action?token=" + rejectToken
                + "&action=reject";

        String subject = "[Action Required] Leave Request from "
                + employee.getName();

        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px;
                        margin: auto; padding: 24px; border: 1px solid #e0e0e0;
                        border-radius: 8px; color: #2b3a4a;">

                <h2>Leave Request for Review</h2>

                <p>Dear <strong>%s</strong>,</p>

                <p>
                    Your team member <strong>%s</strong> has submitted
                    a leave request for your review.
                </p>

                <table style="width: 100%%; border-collapse: collapse;
                              margin-bottom: 20px;">

                    <tr>
                        <td style="padding: 8px; font-weight: bold;">
                            Leave Type
                        </td>
                        <td style="padding: 8px;">%s</td>
                    </tr>

                    <tr>
                        <td style="padding: 8px; font-weight: bold;">
                            Dates
                        </td>
                        <td style="padding: 8px;">
                            %s to %s (%d days)
                        </td>
                    </tr>

                    <tr>
                        <td style="padding: 8px; font-weight: bold;">
                            Reason
                        </td>
                        <td style="padding: 8px;">%s</td>
                    </tr>

                    <tr>
                        <td style="padding: 8px; font-weight: bold;">
                            RAG Policy Check
                        </td>
                        <td style="padding: 8px;">%s</td>
                    </tr>
                </table>

                <p>
                    <strong>Manager comment is required.</strong>
                    Select an action below, enter your comment on the
                    FinFlow page, and confirm your decision.
                </p>

                <div style="text-align: center; margin: 28px 0;">

                    <a href="%s"
                       style="display: inline-block; background: #16a34a;
                              color: white; padding: 12px 20px;
                              text-decoration: none; border-radius: 6px;
                              font-weight: bold; margin: 5px;">
                        Approve Leave
                    </a>

                    <a href="%s"
                       style="display: inline-block; background: #dc2626;
                              color: white; padding: 12px 20px;
                              text-decoration: none; border-radius: 6px;
                              font-weight: bold; margin: 5px;">
                        Reject Leave
                    </a>

                </div>

                <p style="font-size: 12px; color: #718096; text-align: center;">
                    These action links are single-use and expire in %d hours.
                </p>
            </div>
            """.formatted(
                manager.getName(),
                employee.getName(),
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getDaysCount(),
                leave.getReason(),
                leave.getRagDecision() != null
                        ? leave.getRagDecision()
                        : "Standard policy limits apply.",
                approveUrl,
                rejectUrl,
                tokenExpiryHours
        );

        sendHtmlEmail(manager.getEmail(), subject, html);
    }

    /**
     * Sends Reimbursement Review email to Manager with Approve & Reject deep links.
     */
    public void sendReimbursementApprovalEmail(User manager, User employee, ReimbursementRequest req, String approveToken, String rejectToken) {
        String approveUrl = frontendUrl + "/api/token/" + approveToken + "/action";
        String rejectUrl = frontendUrl + "/api/token/" + rejectToken + "/action";

        String subject = "[Action Required] Reimbursement Reclaim from " + employee.getName();
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #2b3a4a;">Reimbursement Request for Review</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Employee <strong>%s</strong> (Contact: %s) has submitted a reimbursement reclaim:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                        <tr><td style="padding: 8px; font-weight: bold; width: 140px;">Claim Amount:</td><td style="font-size: 18px; color: #0284c7; font-weight: bold;">%s %s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Employee ID:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Reason:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">RAG Bill Analysis:</td><td style="color: #4a5568;">%s</td></tr>
                    </table>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #22c55e; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; margin-right: 15px;">Verify & Forward to Finance</a>
                        <a href="%s" style="background-color: #ef4444; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">Reject Claim</a>
                    </div>
                    <p style="font-size: 12px; color: #718096; text-align: center;">These links are single-use and will expire in %d hours.</p>
                </div>
                """.formatted(
                manager.getName(),
                employee.getName(),
                employee.getContactNumber() != null ? employee.getContactNumber() : "N/A",
                req.getCurrency(),
                req.getAmount(),
                employee.getId(),
                req.getReason(),
                req.getRagAnalysis() != null ? req.getRagAnalysis() : "Analyzed against company reimbursement policy.",
                approveUrl,
                rejectUrl,
                tokenExpiryHours
        );

        sendHtmlEmail(manager.getEmail(), subject, html);
    }

    /**
     * Sends Notification to Finance team when a claim is verified by manager.
     */
    public void sendReimbursementFinanceNotification(User financeUser, ReimbursementRequest req) {
        User employee = req.getEmployee();
        String subject = "[Ready for Payment] Reimbursement Verified for " + employee.getName();
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #2b3a4a;">Reimbursement Ready for Payout</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>A reimbursement request has been verified by the manager and is ready for disbursement:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                        <tr><td style="padding: 8px; font-weight: bold; width: 140px;">Employee ID:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Employee Name:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Contact Number:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Exact Amount:</td><td style="font-size: 18px; color: #16a34a; font-weight: bold;">%s %s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Reason:</td><td>%s</td></tr>
                    </table>
                    <p>Log in to FinFlow portal to initiate Razorpay disbursement.</p>
                </div>
                """.formatted(
                financeUser.getName(),
                employee.getId(),
                employee.getName(),
                employee.getContactNumber() != null ? employee.getContactNumber() : "N/A",
                req.getCurrency(),
                req.getAmount(),
                req.getReason()
        );

        sendHtmlEmail(financeUser.getEmail(), subject, html);
    }


    public void sendPaymentReceiptEmail(User employee, ReimbursementRequest req, PaymentRecord payment) {
        String subject = "[Payment Completed] Reimbursement of " + req.getCurrency() + " " + req.getAmount();
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #16a34a;">Payment Successfully Processed</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your reimbursement reclaim has been successfully disbursed by the finance team:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                        <tr><td style="padding: 8px; font-weight: bold; width: 150px;">Amount Paid:</td><td style="font-size: 18px; color: #16a34a; font-weight: bold;">%s %s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Payment ID:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Employee ID:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Contact:</td><td>%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">Paid Date:</td><td>%s</td></tr>
                    </table>
                    <p>Thank you for submitting your claims through FinFlow.</p>
                </div>
                """.formatted(
                employee.getName(),
                req.getCurrency(),
                req.getAmount(),
                payment.getRazorpayPaymentId(),
                employee.getId(),
                employee.getContactNumber() != null ? employee.getContactNumber() : "N/A",
                payment.getPaidAt()
        );

        sendHtmlEmail(employee.getEmail(), subject, html);
    }


    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public void sendLeaveStatusEmail(
            User employee,
            LeaveRequest leave,
            boolean approved,
            String comment) {

        String status = approved ? "APPROVED" : "REJECTED";
        String color = approved ? "#16a34a" : "#dc2626";

        String safeComment = escapeHtml(
                comment != null ? comment.trim() : ""
        );

        String subject = "Leave Request " + status;

        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px;
                        margin: auto; padding: 24px; border: 1px solid #e0e0e0;
                        border-radius: 8px; color: #2b3a4a;">

                <h2 style="color: %s;">Leave Request %s</h2>

                <p>Dear <strong>%s</strong>,</p>

                <p>
                    Your leave request from <strong>%s</strong> to
                    <strong>%s</strong> has been
                    <strong>%s</strong>.
                </p>

                <div style="margin-top: 20px; padding: 16px;
                            background: #f8fafc; border-radius: 6px;">

                    <p style="margin-top: 0;">
                        <strong>Manager Comment</strong>
                    </p>

                    <p style="white-space: pre-wrap; margin-bottom: 0;">%s</p>

                </div>

                <p style="font-size: 12px; color: #718096; margin-top: 24px;">
                    This is an automated notification from FinFlow.
                </p>

            </div>
            """.formatted(
                color,
                approved ? "Approved" : "Rejected",
                escapeHtml(employee.getName()),
                leave.getStartDate(),
                leave.getEndDate(),
                approved ? "approved" : "rejected",
                safeComment.isBlank() ? "No comment provided." : safeComment
        );

        sendHtmlEmail(employee.getEmail(), subject, html);
    }

    /**
     * Internal email sender with graceful fallback.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("[EmailService] Sending email to={} subject={}", to, subject);
        if (mailSender == null) {
            log.warn("[EmailService] MailSender not configured; simulated email sent to={}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EmailService] Successfully sent email to={}", to);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send email to={} (Error: {}). Continuing without interruption.",
                    to, e.getMessage());
        }
    }
}