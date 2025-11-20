package com.codeit.playlist.domain.auth.service;

public interface EmailService {

  void sendTemporaryPassword(String toEmail, String tempPassword);
}
