package com.codeit.playlist.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ResetPasswordRequest (

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않음")
    @Length(min = 1, max = 300, message = "이메일 길이는 1자 이상 300자 이하여야 합니다.")
    String email
){
}
