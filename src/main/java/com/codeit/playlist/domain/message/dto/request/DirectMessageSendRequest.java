package com.codeit.playlist.domain.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectMessageSendRequest (
    @NotBlank
    @Size(min = 1, max = 1000)
    String content
){
}