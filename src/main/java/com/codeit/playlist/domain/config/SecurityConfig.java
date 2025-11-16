package com.codeit.playlist.domain.config;

import com.codeit.playlist.domain.security.LoginFailureHandler;
import com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter;
import com.codeit.playlist.domain.security.jwt.JwtLoginSuccessHandler;
import com.codeit.playlist.domain.security.jwt.JwtLogoutSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@Slf4j
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      JwtLoginSuccessHandler jwtLoginSuccessHandler,
      ObjectMapper objectMapper,
      LoginFailureHandler loginFailureHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      JwtLogoutSuccessHandler jwtLogoutSuccessHandler) throws Exception {
    return http
        .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/",
                    "/index.html",
                    "/vite.svg",
                    "/assets/**",
                    "/api/auth/login",
                    "/api/auth/sign-in",
                    "/api/auth/sign-up",
                    "/api/auth/refresh",
                    "/api/auth/logout")
        )
         // 개발중이기 때문에 csrf를 disable 함. 운영환경에서는 무조!!!!!!!!!!!!!!!!!!!!!!!!!!!!건 켜야함

        .formLogin(login -> login
            .loginProcessingUrl("/api/auth/login")
            .successHandler(jwtLoginSuccessHandler)
            .failureHandler(loginFailureHandler)
        )

        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .logoutSuccessHandler(jwtLogoutSuccessHandler)
        )

        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        )
        .authorizeHttpRequests((authorize) -> authorize
            //로그인 관련
            .requestMatchers("/api/auth/sign-in").permitAll()
            .requestMatchers("/api/auth/sign-up").permitAll()
            .requestMatchers("/api/auth/refresh").permitAll()
            .requestMatchers("/api/users", "/api/auth/**").permitAll()
            .requestMatchers("/api/sse","/api/sse/**").permitAll()

            //정적 리소스
            .requestMatchers("/", "/index.html", "/vite.svg", "/assets/**")
            .permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)


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
