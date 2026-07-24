package com.powerpulse.core.internalsync.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Sensors modulunun CoreApplianceDetail(String id, String name, BigDecimal safeLimitWatt)
// kaydiyla eslesir - simulasyon icin sadece bu 3 alan yeterli.
public record InternalApplianceResponse(
        UUID id,
        String name,
        BigDecimal safeLimitWatt
) {
}
