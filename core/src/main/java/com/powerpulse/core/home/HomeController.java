package com.powerpulse.core.home;

import com.powerpulse.core.home.dto.RegisterHomeRequest;
import com.powerpulse.core.home.dto.RegisteredHomeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/homes")
public class HomeController {

    private final HomeRegistrationService registrationService;

    public HomeController(HomeRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredHomeResponse> register(
            @Valid @RequestBody RegisterHomeRequest request
    ) {
        RegisteredHomeResponse response = registrationService.register(request);

        return ResponseEntity
                .created(URI.create("/api/homes/" + response.id()))
                .body(response);
    }
}