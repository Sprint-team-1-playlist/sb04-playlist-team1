package com.codeit.playlist.global.config;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import com.codeit.playlist.domain.security.oauth.OAuth2SuccessHandler;
import com.codeit.playlist.domain.security.jwt.JwtAuthenticationFilter;
import com.codeit.playlist.domain.security.jwt.JwtLogoutSuccessHandler;
import com.codeit.playlist.domain.security.oauth.CustomOAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers(
                new AntPathRequestMatcher("/"),
                new AntPathRequestMatcher("/error"),
                new AntPathRequestMatcher("/index.html"),
                new AntPathRequestMatcher("/vite.svg"),
                new AntPathRequestMatcher("/assets/**"),
                new AntPathRequestMatcher("/api/auth/sign-in"),
                new AntPathRequestMatcher("/api/auth/sign-up"),
                new AntPathRequestMatcher("/api/auth/reset-password"),
                new AntPathRequestMatcher("/api/auth/sign-out"),
                new AntPathRequestMatcher("/api/users/*/password"),
                new AntPathRequestMatcher("/api/users", POST.name()),
                new AntPathRequestMatcher("/api/users/*/role"),
                new AntPathRequestMatcher("/api/users/*/locked"),
                new AntPathRequestMatcher("/api/users/*", PATCH.name()),
                new AntPathRequestMatcher("/api/auth/refresh")
            )
        )

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

            //정적 리소스
            .requestMatchers("/").permitAll()
            .requestMatchers("/index.html").permitAll()
            .requestMatchers("/vite.svg").permitAll()
            .requestMatchers("/assets/**").permitAll()
            .anyRequest().authenticated()
        )


        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2Service))
            .successHandler(oAuth2SuccessHandler)
        )

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        .oauth2Login(Customizer.withDefaults())


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
