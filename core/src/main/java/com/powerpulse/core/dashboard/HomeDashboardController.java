package com.powerpulse.core.dashboard;

import com.powerpulse.core.auth.User;
import com.powerpulse.core.dashboard.dto.HomeDetailResponse;
import com.powerpulse.core.dashboard.dto.HomeSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
public class HomeDashboardController {

    private final HomeDashboardService dashboardService;

    public HomeDashboardController(
            HomeDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<List<HomeSummaryResponse>> getHomes(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(dashboardService.getHomes(currentUser.getId()));
    }

    @GetMapping("/{homeId}")
    public ResponseEntity<HomeDetailResponse> getHome(
            @PathVariable UUID homeId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return ResponseEntity.ok(
                dashboardService.getHome(homeId, currentUser.getId(), page, size)
        );
    }
}