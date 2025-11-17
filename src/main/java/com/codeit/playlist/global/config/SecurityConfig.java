package com.codeit.playlist.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
    return http
        .csrf(csrf -> csrf.disable()) // 개발중이기 때문에 csrf를 disable 함. 운영환경에서는 무조!!!!!!!!!!!!!!!!!!!!!!!!!!!!건 켜야함
        .authorizeHttpRequests((authorize) -> authorize.anyRequest()
//            .requestMatchers(HttpMethod.POST, "/api/users", "/api/auth/**")
            .permitAll())
//            .anyRequest().authenticated())

        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
