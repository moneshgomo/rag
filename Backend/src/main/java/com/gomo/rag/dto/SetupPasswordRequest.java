package com.gomo.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupPasswordRequest {

    @NotBlank(message = "Setup token is required")
    private String token;

    private String temporaryPassword;

    @NotBlank(message = "New permanent password is required")
    @Size(min = 6, max = 15, message = "Permanent password must be between 6 and 15 characters")
    private String newPassword;

    private String confirmPassword;
}
