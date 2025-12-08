package com.codeit.playlist.global.config;

import com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter;
import com.codeit.playlist.domain.security.jwt.JwtLogoutSuccessHandler;
import com.codeit.playlist.domain.security.oauth.CustomOAuth2Service;
import com.codeit.playlist.domain.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Slf4j
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2Service customOAuth2Service;
  private final OAuth2SuccessHandler  oAuth2SuccessHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      JwtLogoutSuccessHandler jwtLogoutSuccessHandler) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)

        .formLogin(AbstractHttpConfigurer::disable)

        .logout(logout -> logout
            .logoutUrl("/api/auth/sign-out")
            .logoutSuccessHandler(jwtLogoutSuccessHandler)
        )

        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        )
        .authorizeHttpRequests((authorize) -> authorize
            //로그인 관련
            .requestMatchers("/error").permitAll()
            .requestMatchers("/api/auth/sign-in").permitAll()
            .requestMatchers("/api/auth/sign-up").permitAll()
            .requestMatchers("/api/auth/reset-password").permitAll()
            .requestMatchers("/api/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/api/auth/csrf-token").permitAll()

            // 웹 소켓 핸드웨이크를 위한 엔드포인트
            .requestMatchers("/ws/**").permitAll()

            //정적 리소스
            .requestMatchers("/").permitAll()
            .requestMatchers("/index.html").permitAll()
            .requestMatchers("/vite.svg").permitAll()
            .requestMatchers("/assets/**").permitAll()


            //카카오, 구글 Oauth2
            .requestMatchers("/oauth2/**").permitAll()
            .requestMatchers("/login/oauth2/**").permitAll()
            .anyRequest().authenticated()


        )


        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2Service))
            .successHandler(oAuth2SuccessHandler)
        )

        .addFilterAfter(jwtAuthenticationFilter, OAuth2LoginAuthenticationFilter.class)

        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration)
      throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
