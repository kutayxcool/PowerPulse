package com.powerpulse.core.advisory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskQuestionRequest(

        @NotBlank(message = "Soru boş bırakılamaz.")
        @Size(max = 500, message = "Soru en fazla 500 karakter olabilir.")
        String question
) {
}
