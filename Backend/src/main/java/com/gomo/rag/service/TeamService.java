package com.gomo.rag.service;

import com.gomo.rag.dto.AddMemberRequest;
import com.gomo.rag.dto.TeamRequest;
import com.gomo.rag.dto.TeamResponse;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.entity.LeaveBalance;
import com.gomo.rag.entity.Team;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.Role;
import com.gomo.rag.repository.LeaveBalanceRepository;
import com.gomo.rag.repository.TeamRepository;
import com.gomo.rag.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.gomo.rag.service.AdminService.getUserResponse;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordSetupService passwordSetupService;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       LeaveBalanceRepository leaveBalanceRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordSetupService passwordSetupService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordSetupService = passwordSetupService;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TeamResponse createTeam(TeamRequest req) {
        log.info("[TeamService] Creating team: {}", req.getName());

        if (teamRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("Team name already exists: " + req.getName());
        }

        User manager = null;
        if (req.getManagerId() != null) {
            manager = userRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found ID: " + req.getManagerId()));
        }

        Team team = Team.builder()
                .name(req.getName())
                .description(req.getDescription())
                .manager(manager)
                .build();

        Team saved = teamRepository.save(team);

        // Also update manager's team if they didn't have one
        if (manager != null && manager.getTeam() == null) {
            manager.setTeam(saved);
            userRepository.save(manager);
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams() {
        return teamRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + id));
        return mapToResponse(team);
    }

    @Transactional
    public UserResponse addMemberToTeam(Long teamId, AddMemberRequest req) {
        log.info("[TeamService] Adding member to teamId={}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        // Enforce RBAC: Only authorized manager of this team (or SUPER_ADMIN) can create employee accounts
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String currentEmail = auth.getName();
            User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
            if (currentUser != null && currentUser.getRole() == Role.MANAGER) {
                if (team.getManager() == null || !team.getManager().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to add members to this team.");
                }
            }
        }

        User user;

        if (req.getUserId() != null) {
            user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + req.getUserId()));
            user.setTeam(team);
        } else {
            // Register new team member
            if (req.getEmail() == null || req.getPassword() == null || req.getName() == null) {
                throw new IllegalArgumentException("Name, email, and password are required to add new member");
            }
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + req.getEmail());
            }

            user = User.builder()
                    .name(req.getName())
                    .email(req.getEmail())
                    .passwordHash(passwordEncoder.encode(req.getPassword()))
                    .contactNumber(req.getContactNumber())
                    .role(Role.EMPLOYEE)
                    .team(team)
                    .active(true)
                    .passwordChangeRequired(true)
                    .build();

            user = userRepository.save(user);

            // Initialize leave balance
            LeaveBalance balance = LeaveBalance.builder()
                    .employee(user)
                    .year(LocalDate.now().getYear())
                    .totalDays(20)
                    .usedDays(0)
                    .remainingDays(20)
                    .build();
            leaveBalanceRepository.save(balance);

            // Generate single-use password setup token and send onboarding email
            passwordSetupService.createAndSendSetupToken(user, req.getPassword());
        }

        User saved = userRepository.save(user);
        return mapUserToResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<UserResponse> listTeamMembers(Long teamId) {

        if (!teamRepository.existsById(teamId)) {
            throw new IllegalArgumentException(
                    "Team not found with ID: " + teamId
            );
        }

        return userRepository.findTeamMemberDetails(teamId)
                .stream()
                .map(member -> UserResponse.builder()
                        .id(member.getId())
                        .name(member.getName())
                        .email(member.getEmail())
                        .contactNumber(member.getContactNumber())
                        .role(Role.valueOf(member.getRole()))
                        .teamId(member.getTeamId())
                        .teamName(member.getTeamName())
                        .active(member.getActive())
                        .createdAt(member.getCreatedAt())
                        .build())
                .toList();
    }


    private TeamResponse mapToResponse(Team team) {
        List<User> members = userRepository.findAllByTeamId(team.getId());
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .managerId(team.getManager() != null ? team.getManager().getId() : null)
                .managerName(team.getManager() != null ? team.getManager().getName() : null)
                .managerEmail(team.getManager() != null ? team.getManager().getEmail() : null)
                .memberCount(members.size())
                .createdAt(team.getCreatedAt())
                .build();
    }

    private UserResponse mapUserToResponse(User user) {
        return AdminService.getUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUnassignedEmployees(String emailFilter) {
        List<User> employees;
        if (emailFilter != null && !emailFilter.trim().isBlank()) {
            employees = userRepository.searchUnassignedEmployeesByEmail(emailFilter.trim());
        } else {
            employees = userRepository.findUnassignedEmployees();
        }
        return employees.stream()
                .map(AdminService::getUserResponse)
                .toList();
    }
}