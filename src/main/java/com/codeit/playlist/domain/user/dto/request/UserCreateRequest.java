package com.codeit.playlist.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UserCreateRequest (

    @NotBlank(message = "이름 누락")
    @Length(min = 1, max = 50, message = "이름의 길이는 1자 이상 50자 이하여야 합니다.")
    String name,

    @NotBlank(message = "이메일 누락")
    @Email(message = "이메일 형식이 올바르지 않음")
    @Length(min = 1, max = 300, message = "이메일 길이는 1자 이상 300자 이하여야 합니다.")
    String email,

    @NotBlank(message = "비밀번호 누락")
    @Length(min = 8, max = 100 ,message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    String password
){

}
