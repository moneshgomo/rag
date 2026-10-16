package com.gomo.rag.service;

import com.gomo.rag.dto.AuthResponse;
import com.gomo.rag.dto.LoginRequest;
import com.gomo.rag.dto.RegisterRequest;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.entity.LeaveBalance;
import com.gomo.rag.entity.Team;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.Role;
import com.gomo.rag.repository.LeaveBalanceRepository;
import com.gomo.rag.repository.TeamRepository;
import com.gomo.rag.repository.UserRepository;
import com.gomo.rag.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.gomo.rag.service.AdminService.getUserResponse;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       TeamRepository teamRepository,
                       LeaveBalanceRepository leaveBalanceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        log.info("[AuthService] Registering user email={}", req.getEmail());

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + req.getEmail());
        }

        Team team = null;
        if (req.getTeamId() != null) {
            team = teamRepository.findById(req.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + req.getTeamId()));
        }

        Role role = req.getRole() != null ? req.getRole() : Role.EMPLOYEE;

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .contactNumber(req.getContactNumber())
                .role(role)
                .team(team)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Initialize Leave Balance for Employee/Manager
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = LeaveBalance.builder()
                .employee(savedUser)
                .year(currentYear)
                .totalDays(20)
                .usedDays(0)
                .remainingDays(20)
                .build();
        leaveBalanceRepository.save(balance);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        log.info("[AuthService] Logging in user email={}", req.getEmail());

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("User account is disabled");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found for authenticated email: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        return getUserResponse(user);
    }
}