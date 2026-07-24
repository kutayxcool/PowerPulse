package com.powerpulse.core.home;

import com.powerpulse.core.auth.User;
import com.powerpulse.core.home.dto.ApplianceStatusResponse;
import com.powerpulse.core.home.dto.RegisterApplianceRequest;
import com.powerpulse.core.home.dto.RegisterHomeRequest;
import com.powerpulse.core.home.dto.RegisteredApplianceResponse;
import com.powerpulse.core.home.dto.RegisteredHomeResponse;
import com.powerpulse.core.home.dto.UpdateApplianceStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
public class HomeController {

    private final HomeRegistrationService registrationService;
    private final HomeDeletionService deletionService;
    private final ApplianceManagementService applianceManagementService;

    public HomeController(
            HomeRegistrationService registrationService,
            HomeDeletionService deletionService,
            ApplianceManagementService applianceManagementService
    ) {
        this.registrationService = registrationService;
        this.deletionService = deletionService;
        this.applianceManagementService = applianceManagementService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredHomeResponse> register(
            @Valid @RequestBody RegisterHomeRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegisteredHomeResponse response =
                registrationService.register(request, currentUser);

        return ResponseEntity
                .created(URI.create("/api/homes/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{homeId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID homeId,
            @AuthenticationPrincipal User currentUser
    ) {
        deletionService.delete(homeId, currentUser.getId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{homeId}/appliances")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisteredApplianceResponse addAppliance(
            @PathVariable UUID homeId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RegisterApplianceRequest request
    ) {
        return applianceManagementService.addAppliance(
                homeId,
                currentUser.getId(),
                request
        );
    }

    @DeleteMapping("/{homeId}/appliances/{applianceId}")
    public ResponseEntity<Void> removeAppliance(
            @PathVariable UUID homeId,
            @PathVariable UUID applianceId,
            @AuthenticationPrincipal User currentUser
    ) {
        applianceManagementService.removeAppliance(
                homeId,
                currentUser.getId(),
                applianceId
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{homeId}/appliances/{applianceId}/status")
    public ApplianceStatusResponse updateApplianceStatus(
            @PathVariable UUID homeId,
            @PathVariable UUID applianceId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateApplianceStatusRequest request
    ) {
        return applianceManagementService.setApplianceActive(
                homeId,
                currentUser.getId(),
                applianceId,
                request.active()
        );
    }
}
