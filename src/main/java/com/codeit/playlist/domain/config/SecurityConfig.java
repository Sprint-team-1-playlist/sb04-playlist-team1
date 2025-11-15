package com.codeit.playlist.domain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Slf4j
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(
            csrf -> csrf.disable()) // 개발중이기 때문에 csrf를 disable 함. 운영환경에서는 무조!!!!!!!!!!!!!!!!!!!!!!!!!!!!건 켜야함
        .authorizeHttpRequests((authorize) -> authorize
            //로그인 관련
            .requestMatchers("/api/auth/sign-in").permitAll()
            .requestMatchers("/api/auth/sign-up").permitAll()
            .requestMatchers("/api/auth/refresh").permitAll()
            .requestMatchers("/api/users", "/api/auth/**").permitAll()

            //정적 리소스
            .requestMatchers("/", "/index.html", "/vite.svg", "/assets/**")
            .permitAll()
            .anyRequest().authenticated())

        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
      throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

}
