package com.powerpulse.core.home.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RegisterHomeRequest(

        @NotBlank(message = "Ev adı boş bırakılamaz.")
        @Size(max = 150, message = "Ev adı en fazla 150 karakter olabilir.")
        String name,

        @NotBlank(message = "E-posta adresi boş bırakılamaz.")
        @Email(message = "Geçerli bir e-posta adresi girilmelidir.")
        String contactEmail,

        @DecimalMin(
                value = "0.01",
                message = "Enerji kotası sıfırdan büyük olmalıdır."
        )
        BigDecimal budgetQuotaKwh,

        @NotEmpty(message = "En az bir cihaz eklenmelidir.")
        List<@Valid RegisterApplianceRequest> appliances
) {
}