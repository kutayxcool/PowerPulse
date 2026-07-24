package com.powerpulse.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank(message = "İsim boş bırakılamaz.")
        @Size(max = 150, message = "İsim en fazla 150 karakter olabilir.")
        String name,

        @NotBlank(message = "E-posta boş bırakılamaz.")
        @Email(message = "Geçerli bir e-posta adresi giriniz.")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir.")
        String email,

        @NotBlank(message = "Şifre boş bırakılamaz.")
        @Size(min = 6, max = 72, message = "Şifre en az 6 karakter olmalıdır.")
        String password
) {
}
