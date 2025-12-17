package com.codeit.playlist.domain.auth.service.basic;

import com.codeit.playlist.domain.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicEmailService implements EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.from}")
  private String from;

  public void sendTemporaryPassword(String toEmail, String tempPassword) {
    try {
      log.debug("[메일] : 임시 비밀번호 이메일 전송 시작 - 수신자: {}", toEmail);
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from); // SES Verified Email
      message.setTo(toEmail);
      message.setSubject("[Playlist] 임시 비밀번호 안내");
      message.setText(
          "안녕하세요.\n\n" +
              "요청하신 임시 비밀번호는 아래와 같습니다:\n\n" +
              tempPassword + "\n\n" +
              "로그인 후 반드시 비밀번호를 변경해주세요."
      );

      mailSender.send(message);
      log.info("[메일] : 임시 비밀번호 이메일 전송 완료 - 수신자: {}", toEmail);
    } catch (Exception e) {
      log.error("[메일] : 임시 비밀번호 이메일 전송 실패 - 수신자: {}", toEmail, e);
      throw e;
    }
  }

}
