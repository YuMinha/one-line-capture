package com.example.capture.capture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaptureCreateRequest(
        @NotBlank
        @Size(max = 500)
        String text
) {}
