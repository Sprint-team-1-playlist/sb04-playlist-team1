package com.codeit.playlist.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email
    String username,
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {

}
