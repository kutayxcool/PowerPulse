package com.powerpulse.core.home.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegisterApplianceRequest(

        @NotBlank(message = "Cihaz adı boş bırakılamaz.")
        @Size(max = 150, message = "Cihaz adı en fazla 150 karakter olabilir.")
        String name,

        @DecimalMin(
                value = "0.01",
                message = "Güvenli güç limiti sıfırdan büyük olmalıdır."
        )
        BigDecimal safeLimitWatt
) {
}