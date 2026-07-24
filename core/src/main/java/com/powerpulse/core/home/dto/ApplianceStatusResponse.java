package com.powerpulse.core.home.dto;

import java.util.UUID;

public record ApplianceStatusResponse(
        UUID id,
        boolean active
) {
}
