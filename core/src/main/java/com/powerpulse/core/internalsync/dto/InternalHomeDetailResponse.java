package com.powerpulse.core.internalsync.dto;

import java.util.List;
import java.util.UUID;

// Sensors modulunun CoreHomeDetail(String id, String name, List<CoreApplianceDetail> appliances)
// kaydiyla eslesir.
public record InternalHomeDetailResponse(
        UUID id,
        String name,
        List<InternalApplianceResponse> appliances
) {
}
