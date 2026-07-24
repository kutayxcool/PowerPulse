package com.powerpulse.core.internalsync;

import com.powerpulse.core.internalsync.dto.InternalHomeDetailResponse;
import com.powerpulse.core.internalsync.dto.InternalHomeSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Sadece Sensors modulunun kullandigi, sistem geneli (tum kullanicilar
// dahil) ev/cihaz listesini donen uc. JWT ile korunmaz - bunun yerine
// InternalApiKeyFilter, "X-Internal-Api-Key" header'ini kontrol eder
// (bkz. SecurityConfig - bu path permitAll ama filtre kendisi 401 doner).
@RestController
@RequestMapping("/api/internal")
public class InternalSyncController {

    private final InternalSyncService internalSyncService;

    public InternalSyncController(InternalSyncService internalSyncService) {
        this.internalSyncService = internalSyncService;
    }

    @GetMapping("/homes")
    public List<InternalHomeSummaryResponse> getAllHomeIds() {
        return internalSyncService.getAllHomeIds();
    }

    @GetMapping("/homes/{homeId}")
    public InternalHomeDetailResponse getHomeDetail(@PathVariable UUID homeId) {
        return internalSyncService.getHomeDetail(homeId);
    }
}
