package com.codeit.playlist.domain.user.service;

import com.codeit.playlist.domain.user.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

  void sendTemporaryPassword(ResetPasswordRequest request);
}
