package com.powerpulse.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "E-posta boş bırakılamaz.")
        String email,

        @NotBlank(message = "Şifre boş bırakılamaz.")
        String password
) {
}
