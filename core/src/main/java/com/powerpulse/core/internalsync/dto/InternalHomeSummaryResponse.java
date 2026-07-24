package com.powerpulse.core.internalsync.dto;

import java.util.UUID;

// Sensors modulunun CoreHomeSummary(String id) kaydiyla eslesir -
// Sensors sadece "hangi ev ID'leri var" bilgisine ihtiyac duyar,
// detaylari ayrica /api/internal/homes/{id} ile ceker.
public record InternalHomeSummaryResponse(UUID id) {
}
