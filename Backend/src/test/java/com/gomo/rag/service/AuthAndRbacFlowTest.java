package com.gomo.rag.service;

import com.gomo.rag.dto.AddMemberRequest;
import com.gomo.rag.dto.AuthResponse;
import com.gomo.rag.dto.LoginRequest;
import com.gomo.rag.entity.Team;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.Role;
import com.gomo.rag.exception.PasswordChangeRequiredException;
import com.gomo.rag.repository.LeaveBalanceRepository;
import com.gomo.rag.repository.TeamRepository;
import com.gomo.rag.repository.UserRepository;
import com.gomo.rag.security.JwtUtil;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AuthAndRbacFlowTest {

    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private LeaveBalanceRepository leaveBalanceRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private PasswordSetupService passwordSetupService;

    private AuthService authService;
    private TeamService teamService;

    @BeforeMethod
    public void setUp() {
        userRepository = mock(UserRepository.class);
        teamRepository = mock(TeamRepository.class);
        leaveBalanceRepository = mock(LeaveBalanceRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = mock(JwtUtil.class);
        passwordSetupService = mock(PasswordSetupService.class);

        authService = new AuthService(userRepository, teamRepository, leaveBalanceRepository, passwordEncoder, jwtUtil);
        teamService = new TeamService(teamRepository, userRepository, leaveBalanceRepository, passwordEncoder, passwordSetupService);
    }

    @AfterMethod
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test(expectedExceptions = PasswordChangeRequiredException.class)
    public void testLoginBlockedWhenPasswordChangeRequired() {
        User user = User.builder()
                .id(1L)
                .name("Unset User")
                .email("unset@finflow.com")
                .passwordHash(passwordEncoder.encode("Temp#123"))
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true) // Not yet set up!
                .build();

        when(userRepository.findByEmail("unset@finflow.com")).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.builder()
                .email("unset@finflow.com")
                .password("Temp#123")
                .build();

        authService.login(request);
    }

    @Test
    public void testLoginAllowedWhenPasswordChangeNotRequired() {
        User user = User.builder()
                .id(2L)
                .name("Permanent User")
                .email("ready@finflow.com")
                .passwordHash(passwordEncoder.encode("Permanent#Pass123"))
                .role(Role.MANAGER)
                .active(true)
                .passwordChangeRequired(false) // Completed!
                .build();

        when(userRepository.findByEmail("ready@finflow.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("ready@finflow.com", "MANAGER")).thenReturn("mock-jwt-token");

        LoginRequest request = LoginRequest.builder()
                .email("ready@finflow.com")
                .password("Permanent#Pass123")
                .build();

        AuthResponse response = authService.login(request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getToken(), "mock-jwt-token");
        Assert.assertEquals(response.getEmail(), "ready@finflow.com");
        Assert.assertFalse(response.isPasswordChangeRequired());
    }

    @Test(expectedExceptions = AccessDeniedException.class)
    public void testManagerCannotAddEmployeeToAnotherManagersTeam() {
        User manager1 = User.builder()
                .id(10L)
                .email("manager1@finflow.com")
                .role(Role.MANAGER)
                .active(true)
                .build();

        User manager2 = User.builder()
                .id(20L)
                .email("manager2@finflow.com")
                .role(Role.MANAGER)
                .active(true)
                .build();

        Team teamB = Team.builder()
                .id(100L)
                .name("Team B")
                .manager(manager2) // Managed by manager2!
                .build();

        when(teamRepository.findById(100L)).thenReturn(Optional.of(teamB));
        when(userRepository.findByEmail("manager1@finflow.com")).thenReturn(Optional.of(manager1));

        // Authenticate as manager1
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager1@finflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        AddMemberRequest request = AddMemberRequest.builder()
                .name("New Emp")
                .email("newemp@finflow.com")
                .password("Temp#Pass123")
                .build();

        // Manager 1 tries to add an employee to Team B (which belongs to Manager 2)
        teamService.addMemberToTeam(100L, request);
    }

    @Test
    public void testAuthorizedManagerCanAddEmployeeToTheirOwnTeam() {
        User manager1 = User.builder()
                .id(10L)
                .email("manager1@finflow.com")
                .role(Role.MANAGER)
                .active(true)
                .build();

        Team teamA = Team.builder()
                .id(50L)
                .name("Team A")
                .manager(manager1) // Managed by manager1
                .build();

        when(teamRepository.findById(50L)).thenReturn(Optional.of(teamA));
        when(userRepository.findByEmail("manager1@finflow.com")).thenReturn(Optional.of(manager1));
        when(userRepository.existsByEmail("newemp@finflow.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Authenticate as manager1
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager1@finflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        AddMemberRequest request = AddMemberRequest.builder()
                .name("New Emp")
                .email("newemp@finflow.com")
                .password("Temp#Pass123")
                .build();

        var response = teamService.addMemberToTeam(50L, request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getEmail(), "newemp@finflow.com");
        Assert.assertTrue(response.isPasswordChangeRequired());

        // Verify setup token was triggered for the new employee
        verify(passwordSetupService).createAndSendSetupToken(any(User.class), eq("Temp#Pass123"));
    }
}
