package com.powerpulse.core.advisory;

import com.powerpulse.core.auth.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
public class HomeAssistantController {

    private final HomeAssistantService assistantService;

    public HomeAssistantController(
            HomeAssistantService assistantService
    ) {
        this.assistantService = assistantService;
    }

    @PostMapping("/{homeId}/ask")
    public ResponseEntity<AskQuestionResponse> ask(
            @PathVariable UUID homeId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AskQuestionRequest request
    ) {
        return ResponseEntity.ok(
                assistantService.ask(homeId, currentUser.getId(), request.question())
        );
    }
}
