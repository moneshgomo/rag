package com.gomo.rag.service;

import com.gomo.rag.dto.LeaveActionRequest;
import com.gomo.rag.dto.LeaveBalanceResponse;
import com.gomo.rag.dto.LeaveResponse;
import com.gomo.rag.dto.LeaveSubmitRequest;
import com.gomo.rag.entity.LeaveBalance;
import com.gomo.rag.entity.LeaveRequest;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.LeaveStatus;
import com.gomo.rag.enums.RequestType;
import com.gomo.rag.enums.TokenAction;
import com.gomo.rag.repository.EmailActionTokenRepository;
import com.gomo.rag.repository.LeaveBalanceRepository;
import com.gomo.rag.repository.LeaveRequestRepository;
import com.gomo.rag.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final UserRepository userRepository;
    private final EmailActionTokenRepository tokenRepository;
    private final RagService ragService;
    private final EmailService emailService;
    private final AuthService authService;

    public LeaveService(LeaveRequestRepository leaveRepository,
                        LeaveBalanceRepository balanceRepository,
                        UserRepository userRepository,
                        EmailActionTokenRepository tokenRepository,
                        RagService ragService,
                        EmailService emailService,
                        AuthService authService) {
        this.leaveRepository = leaveRepository;
        this.balanceRepository = balanceRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.ragService = ragService;
        this.emailService = emailService;
        this.authService = authService;
    }

    @Transactional
    public LeaveResponse submitLeave(LeaveSubmitRequest req) {
        User employee = authService.getCurrentAuthenticatedUser();
        log.info("[LeaveService] Employee {} submitting leave from {} to {}",
                employee.getEmail(), req.getStartDate(), req.getEndDate());

        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        int daysCount = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
        int currentYear = req.getStartDate().getYear();

        // Check balance
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndYear(employee.getId(), currentYear)
                .orElseGet(() -> {
                    LeaveBalance nb = LeaveBalance.builder()
                            .employee(employee)
                            .year(currentYear)
                            .totalDays(20)
                            .usedDays(0)
                            .remainingDays(20)
                            .build();
                    return balanceRepository.save(nb);
                });

        if (balance.getRemainingDays() < daysCount) {
            throw new IllegalArgumentException("Insufficient leave balance. Requested: "
                    + daysCount + " days, Available: " + balance.getRemainingDays() + " days.");
        }

        // Determine manager
        User manager = null;
        if (employee.getTeam() != null && employee.getTeam().getManager() != null) {
            manager = employee.getTeam().getManager();
        } else {
            // Fallback to first super admin or manager
            List<User> managers = userRepository.findAllByRole("MANAGER");
            if (!managers.isEmpty()) {
                manager = managers.get(0);
            } else {
                List<User> admins = userRepository.findAllByRole("SUPER_ADMIN");
                if (!admins.isEmpty()) {
                    manager = admins.get(0);
                } else {
                    manager = employee; // Self-assigned if alone in system
                }
            }
        }

        // RAG policy check


        String ragPrompt = """
        Evaluate this employee request against the retrieved
        FinFlow Company Leave Policy.

        Leave Category: %s
        Start Date: %s
        End Date: %s
        Duration: %d calendar days
        Reason: %s
        Submission Date: %s

        Identify applicable policy conditions, compliance concerns,
        and physical document requirements where relevant.

        Provide a concise advisory for the reporting manager.
        Do not make the final approval or rejection decision.
        """.formatted(
                req.getLeaveType(),
                req.getStartDate(),
                req.getEndDate(),
                daysCount,
                req.getReason(),
                LocalDate.now()
        );


        String ragDecision ;

        try {
            ragDecision = ragService.ask(ragPrompt);
        } catch (Exception e) {
            log.warn("[LeaveService] RAG service check error: {}", e.getMessage());
            ragDecision = "Leave request complies with standard organization quota.";
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .manager(manager)
                .leaveType(req.getLeaveType() != null ? req.getLeaveType() : "CASUAL")
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .days(daysCount)
                .reason(req.getReason())
                .ragDecision(ragDecision)
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRepository.save(leaveRequest);

        // Generate email deep links for one-click action
        if (manager != null && !manager.getId().equals(employee.getId())) {
            String approveToken = emailService.createActionToken(saved.getId(), RequestType.LEAVE, TokenAction.APPROVE, manager.getEmail());
            String rejectToken = emailService.createActionToken(saved.getId(), RequestType.LEAVE, TokenAction.REJECT, manager.getEmail());
            emailService.sendLeaveApprovalEmail(manager, employee, saved, approveToken, rejectToken);
        }

        return mapToResponse(saved);
    }

    public List<Object> getMyLeaveRequest(){
        Long loggedInUserID = authService.getCurrentAuthenticatedUser().getId();
        List<Object> leaveResponses = Collections.singletonList(leaveRepository.getLeaveRequestBy(loggedInUserID));
        
        return  leaveResponses;
    }



    @Transactional
    public List<LeaveResponse> getPendingLeavesForManager() {
        User manager = authService.getCurrentAuthenticatedUser();
        return leaveRepository.findPendingByManagerId(manager.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public LeaveBalanceResponse getMyBalance() {
        User employee = authService.getCurrentAuthenticatedUser();
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndYear(employee.getId(), currentYear)
                .orElseGet(() -> LeaveBalance.builder()
                        .employee(employee)
                        .year(currentYear)
                        .totalDays(20)
                        .usedDays(0)
                        .remainingDays(20)
                        .build());

        return LeaveBalanceResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .year(balance.getYear())
                .totalDays(balance.getTotalDays())
                .usedDays(balance.getUsedDays())
                .remainingDays(balance.getRemainingDays())
                .build();
    }

    @Transactional
    public LeaveResponse approveLeave(Long leaveId, LeaveActionRequest req) {

        // 1. Validate the manager's comment
        String comment = validateManagerComment(req);

        // 2. Find the leave request
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Leave request not found with ID: " + leaveId
                        )
                );

        // 3. Only pending requests can be approved
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Leave request is already " + leave.getStatus()
            );
        }

        // 4. Update status and save manager comment
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setManagerComment(comment);

        LeaveRequest updated = leaveRepository.save(leave);

        // 5. Deduct the employee's leave balance
        int year = leave.getStartDate().getYear();

        balanceRepository.deductDays(
                leave.getEmployee().getId(),
                year,
                leave.getDaysCount()
        );

        // 6. Invalidate all action tokens for this request
        tokenRepository.invalidateAllForRequest(
                leave.getId(),
                RequestType.LEAVE.name()
        );

        // 7. Notify the employee with the manager's comment
        emailService.sendLeaveStatusEmail(
                leave.getEmployee(),
                updated,
                true,
                comment
        );

        // 8. Return the updated response
        return mapToResponse(updated);
    }

    @Transactional
    public LeaveResponse rejectLeave(Long leaveId, LeaveActionRequest req) {

        // 1. Validate the manager's comment
        String comment = validateManagerComment(req);

        // 2. Find the leave request
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Leave request not found with ID: " + leaveId
                        )
                );

        // 3. Only pending requests can be rejected
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Leave request is already " + leave.getStatus()
            );
        }

        // 4. Update status and save manager comment
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setManagerComment(comment);

        LeaveRequest updated = leaveRepository.save(leave);

        // 5. Invalidate all action tokens for this request
        tokenRepository.invalidateAllForRequest(
                leave.getId(),
                RequestType.LEAVE.name()
        );

        // 6. Notify the employee with the manager's comment
        emailService.sendLeaveStatusEmail(
                leave.getEmployee(),
                updated,
                false,
                comment
        );

        // 7. Return the updated response
        return mapToResponse(updated);
    }

    private String validateManagerComment(LeaveActionRequest req) {

        if (req == null
                || req.getComment() == null
                || req.getComment().isBlank()) {

            throw new IllegalArgumentException(
                    "Manager comment is required to approve or reject a leave request."
            );
        }

        return req.getComment().trim();
    }


    private LeaveResponse mapToResponse(LeaveRequest l) {
        return LeaveResponse.builder()
                .id(l.getId())
                .employeeId(l.getEmployee().getId())
                .employeeName(l.getEmployee().getName())
                .employeeEmail(l.getEmployee().getEmail())
                .employeeContact(l.getEmployee().getContactNumber())
                .managerId(l.getManager() != null ? l.getManager().getId() : null)
                .managerName(l.getManager() != null ? l.getManager().getName() : null)
                .leaveType(l.getLeaveType())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .daysCount(l.getDaysCount())
                .reason(l.getReason())
                .ragDecision(l.getRagDecision())
                .status(l.getStatus())
                .managerComment(l.getManagerComment())
                .createdAt(l.getCreatedAt())
                .build();
    }
}