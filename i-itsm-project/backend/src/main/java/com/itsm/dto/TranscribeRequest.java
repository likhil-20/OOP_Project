package com.itsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranscribeRequest(
    @NotBlank(message = "Audio is required.")
    @Size(max = 6000000, message = "Recording is too long. Keep it under one minute.")
    String audio,

    @NotBlank(message = "Audio type is required.")
    @Size(max = 50, message = "Audio type is invalid.")
    String mimeType
) {}