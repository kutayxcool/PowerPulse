package com.powerpulse.core.home.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateApplianceStatusRequest(
        @NotNull(message = "active alanı boş bırakılamaz.")
        Boolean active
) {
}
