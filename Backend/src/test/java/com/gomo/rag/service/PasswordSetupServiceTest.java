package com.gomo.rag.service;

import com.gomo.rag.dto.RegenerateSetupLinkRequest;
import com.gomo.rag.dto.SetupPasswordRequest;
import com.gomo.rag.dto.TokenValidationResponse;
import com.gomo.rag.dto.UserResponse;
import com.gomo.rag.entity.PasswordSetupToken;
import com.gomo.rag.entity.User;
import com.gomo.rag.enums.Role;
import com.gomo.rag.repository.PasswordSetupTokenRepository;
import com.gomo.rag.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PasswordSetupServiceTest {

    private PasswordSetupTokenRepository tokenRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private PasswordSetupService setupService;

    @BeforeMethod
    public void setUp() {
        tokenRepository = mock(PasswordSetupTokenRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        emailService = mock(EmailService.class);

        setupService = new PasswordSetupService(tokenRepository, userRepository, passwordEncoder, emailService);
        ReflectionTestUtils.setField(setupService, "tokenExpiryHours", 24);
    }

    @Test
    public void testSuccessfulOnboardingAndTokenGeneration() {
        User user = User.builder()
                .id(1L)
                .name("Alice Manager")
                .email("alice@finflow.com")
                .passwordHash(passwordEncoder.encode("Temp#Pass123"))
                .role(Role.MANAGER)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        String rawToken = setupService.createAndSendSetupToken(user, "Temp#Pass123");

        Assert.assertNotNull(rawToken);
        Assert.assertFalse(rawToken.isBlank());

        // Verify old tokens were invalidated
        verify(tokenRepository).invalidateAllActiveTokensForUser(user.getId());

        // Verify token entity was saved with hash
        ArgumentCaptor<PasswordSetupToken> captor = ArgumentCaptor.forClass(PasswordSetupToken.class);
        verify(tokenRepository).save(captor.capture());

        PasswordSetupToken saved = captor.getValue();
        Assert.assertNotNull(saved);
        Assert.assertEquals(saved.getTokenHash(), PasswordSetupService.hashToken(rawToken));
        Assert.assertFalse(saved.isUsed());
        Assert.assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify email was sent with temporary password and raw token
        verify(emailService).sendOnboardingEmail(eq(user), eq("Temp#Pass123"), eq(rawToken));
    }

    @Test
    public void testValidateSetupToken_Valid() {
        String rawToken = "test-raw-token-12345";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(2L)
                .name("Bob Employee")
                .email("bob@finflow.com")
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(10L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(12))
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        TokenValidationResponse response = setupService.validateSetupToken(rawToken);

        Assert.assertTrue(response.isValid());
        Assert.assertEquals(response.getEmail(), "bob@finflow.com");
        Assert.assertEquals(response.getRole(), Role.EMPLOYEE);
    }

    @Test
    public void testValidateSetupToken_Expired() {
        String rawToken = "expired-token-123";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(3L)
                .name("Charlie")
                .email("charlie@finflow.com")
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(11L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired!
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        TokenValidationResponse response = setupService.validateSetupToken(rawToken);

        Assert.assertFalse(response.isValid());
        Assert.assertTrue(response.getMessage().contains("expired"));
    }

    @Test
    public void testValidateSetupToken_AlreadyUsed() {
        String rawToken = "used-token-123";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(4L)
                .name("David")
                .email("david@finflow.com")
                .role(Role.MANAGER)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(12L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(5))
                .used(true) // Already used!
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        TokenValidationResponse response = setupService.validateSetupToken(rawToken);

        Assert.assertFalse(response.isValid());
        Assert.assertTrue(response.getMessage().contains("already been used"));
    }

    @Test
    public void testSetupPassword_Success() {
        String rawToken = "valid-setup-token";
        String tokenHash = PasswordSetupService.hashToken(rawToken);
        String tempPass = "Temp@Password123";
        String newPass = "NewPermanent#Pass456";

        User user = User.builder()
                .id(5L)
                .name("Eve Manager")
                .email("eve@finflow.com")
                .passwordHash(passwordEncoder.encode(tempPass))
                .role(Role.MANAGER)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(15L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .token(rawToken)
                .temporaryPassword(tempPass)
                .newPassword(newPass)
                .confirmPassword(newPass)
                .build();

        UserResponse response = setupService.setupPassword(request);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getEmail(), "eve@finflow.com");
        Assert.assertFalse(response.isPasswordChangeRequired());

        // Verify user was updated
        Assert.assertFalse(user.isPasswordChangeRequired());
        Assert.assertTrue(passwordEncoder.matches(newPass, user.getPasswordHash()));

        // Verify token was marked used
        Assert.assertTrue(tokenEntity.isUsed());
        Assert.assertNotNull(tokenEntity.getUsedAt());
        verify(tokenRepository).save(tokenEntity);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*different from the temporary password.*")
    public void testSetupPassword_FailsWhenSameAsTemporaryPassword() {
        String rawToken = "same-pass-token";
        String tokenHash = PasswordSetupService.hashToken(rawToken);
        String tempPass = "SameOldPassword123";

        User user = User.builder()
                .id(6L)
                .name("Frank")
                .email("frank@finflow.com")
                .passwordHash(passwordEncoder.encode(tempPass))
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(16L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .token(rawToken)
                .newPassword(tempPass) // Same password!
                .build();

        setupService.setupPassword(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*at least 6 characters.*")
    public void testSetupPassword_FailsWhenTooShort() {
        String rawToken = "short-pass-token";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(7L)
                .name("Grace")
                .email("grace@finflow.com")
                .passwordHash(passwordEncoder.encode("Temp#123"))
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(17L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .token(rawToken)
                .newPassword("12345") // Only 5 characters!
                .build();

        setupService.setupPassword(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*expired.*")
    public void testSetupPassword_FailsWhenTokenExpired() {
        String rawToken = "expired-token-exec";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(8L)
                .name("Hank")
                .email("hank@finflow.com")
                .passwordHash(passwordEncoder.encode("Temp#123"))
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(18L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().minusHours(2)) // Expired
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewValidPass#123")
                .build();

        setupService.setupPassword(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*already been used.*")
    public void testSetupPassword_FailsWhenTokenReused() {
        String rawToken = "reused-token-exec";
        String tokenHash = PasswordSetupService.hashToken(rawToken);

        User user = User.builder()
                .id(9L)
                .name("Ivy")
                .email("ivy@finflow.com")
                .passwordHash(passwordEncoder.encode("Temp#123"))
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        PasswordSetupToken tokenEntity = PasswordSetupToken.builder()
                .id(19L)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(5))
                .used(true) // Reused!
                .build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewValidPass#123")
                .build();

        setupService.setupPassword(request);
    }

    @Test
    public void testRegenerateSetupLink_Success() {
        User user = User.builder()
                .id(10L)
                .name("Jack Manager")
                .email("jack@finflow.com")
                .passwordHash(passwordEncoder.encode("OldTemp#123"))
                .role(Role.MANAGER)
                .active(true)
                .passwordChangeRequired(true)
                .build();

        when(userRepository.findByEmail("jack@finflow.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegenerateSetupLinkRequest request = RegenerateSetupLinkRequest.builder()
                .email("jack@finflow.com")
                .build();

        setupService.regenerateSetupLink(request);

        // Verify user temporary password was updated
        verify(userRepository, atLeastOnce()).save(user);

        // Verify old tokens were invalidated
        verify(tokenRepository, atLeastOnce()).invalidateAllActiveTokensForUser(user.getId());

        // Verify a new token was saved
        verify(tokenRepository, atLeastOnce()).save(any(PasswordSetupToken.class));

        // Verify onboarding email was resent
        verify(emailService, atLeastOnce()).sendOnboardingEmail(eq(user), anyString(), anyString());
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*already completed password setup.*")
    public void testRegenerateSetupLink_FailsIfAlreadyCompleted() {
        User user = User.builder()
                .id(11L)
                .name("Kate")
                .email("kate@finflow.com")
                .role(Role.EMPLOYEE)
                .active(true)
                .passwordChangeRequired(false) // Already completed!
                .build();

        when(userRepository.findByEmail("kate@finflow.com")).thenReturn(Optional.of(user));

        RegenerateSetupLinkRequest request = RegenerateSetupLinkRequest.builder()
                .email("kate@finflow.com")
                .build();

        setupService.regenerateSetupLink(request);
    }
}
