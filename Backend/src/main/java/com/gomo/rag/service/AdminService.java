package com.gomo.rag.service;

import com.gomo.rag.dto.EmployeesList;
import com.gomo.rag.dto.ManagersList;
import com.gomo.rag.dto.RegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordSetupService passwordSetupService;

    public AdminService(UserRepository userRepository,
                        TeamRepository teamRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        PasswordEncoder passwordEncoder,
                        PasswordSetupService passwordSetupService) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordSetupService = passwordSetupService;
    }

    @Transactional
    public UserResponse createManager(RegisterRequest req) {
        log.info("[AdminService] Creating manager: {}", req.getEmail());

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + req.getEmail());
        }

        Team team = null;
        if (req.getTeamId() != null) {
            team = teamRepository.findById(req.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found ID: " + req.getTeamId()));
        }

        User manager = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .contactNumber(req.getContactNumber())
                .role(Role.MANAGER)
                .team(team)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        User saved = userRepository.save(manager);

        // If team was specified without manager, assign this manager to team
        if (team != null && team.getManager() == null) {
            team.setManager(saved);
            teamRepository.save(team);
        }

        // Initialize leave balance
        LeaveBalance balance = LeaveBalance.builder()
                .employee(saved)
                .year(LocalDate.now().getYear())
                .totalDays(20)
                .usedDays(0)
                .remainingDays(20)
                .build();
        leaveBalanceRepository.save(balance);

        // Generate secure setup token and send onboarding email
        passwordSetupService.createAndSendSetupToken(saved, req.getPassword());

        return mapToResponse(saved);
    }

    public List<ManagersList> listManagers() {
        List<ManagersList> manager = userRepository.findManagerAllByRole("MANAGER");

        return manager;
    }

    public List<EmployeesList> listEmployees() {
        List<EmployeesList> employees = userRepository.findEmployeesAllByRole("EMPLOYEE");

        return employees;
    }

    @Transactional
    public UserResponse createUser(RegisterRequest req) {
        log.info("[AdminService] Creating user: {} with role: {}", req.getEmail(), req.getRole());

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + req.getEmail());
        }

        Team team = null;
        if (req.getTeamId() != null) {
            team = teamRepository.findById(req.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found ID: " + req.getTeamId()));
        }

        Role role = req.getRole() != null ? req.getRole() : Role.EMPLOYEE;

        boolean changeRequired = role == Role.MANAGER || role == Role.EMPLOYEE;

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .contactNumber(req.getContactNumber())
                .role(role)
                .team(team)
                .active(true)
                .passwordChangeRequired(changeRequired)
                .build();

        User saved = userRepository.save(user);

        // Initialize leave balance
        LeaveBalance balance = LeaveBalance.builder()
                .employee(saved)
                .year(LocalDate.now().getYear())
                .totalDays(20)
                .usedDays(0)
                .remainingDays(20)
                .build();
        leaveBalanceRepository.save(balance);

        if (changeRequired) {
            passwordSetupService.createAndSendSetupToken(saved, req.getPassword());
        }

        return mapToResponse(saved);
    }

    @Transactional
    public List<UserResponse> listUsers(String role) {
        if (role != null && !role.isBlank()) {
            return userRepository.findAllByRole(role.toUpperCase()).stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setActive(!user.isActive());
        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }



    private UserResponse mapToResponse(User user) {
        return getUserResponse(user);
    }

    static UserResponse getUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .contactNumber(user.getContactNumber())
                .role(user.getRole())
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .active(user.isActive())
                .passwordChangeRequired(user.isPasswordChangeRequired())
                .createdAt(user.getCreatedAt())
                .build();
    }
}