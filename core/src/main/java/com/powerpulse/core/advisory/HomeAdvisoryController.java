package com.powerpulse.core.advisory;

import com.powerpulse.core.auth.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
public class HomeAdvisoryController {

    private final HomeAdvisoryService advisoryService;

    public HomeAdvisoryController(
            HomeAdvisoryService advisoryService
    ) {
        this.advisoryService = advisoryService;
    }

    @GetMapping("/{homeId}/recommendation")
    public ResponseEntity<RecommendationResponse>
    generateRecommendation(
            @PathVariable UUID homeId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                advisoryService.generate(homeId, currentUser.getId())
        );
    }
}