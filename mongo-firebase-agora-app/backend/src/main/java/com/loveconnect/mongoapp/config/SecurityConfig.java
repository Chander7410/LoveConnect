package com.loveconnect.mongoapp.config;

import com.loveconnect.mongoapp.security.FirebaseAuthenticationFilter;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        FirebaseAuthenticationFilter firebaseFilter,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/error", "/api/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/google", "/api/auth/send-otp",
                    "/api/auth/verify-otp", "/api/auth/forgot-password", "/api/auth/reset-password",
                    "/api/auth/send-signup-otp", "/api/auth/verify-signup-otp", "/api/auth/complete-signup",
                    "/api/auth/resend-signup-otp", "/api/auth/signup/send-otp", "/api/auth/signup/verify-otp",
                    "/api/auth/verify-login-otp", "/api/auth/forgot-password/send-otp",
                    "/api/auth/forgot-password/verify-otp", "/api/auth/forgot-password/reset",
                    "/api/auth/resend-otp").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        var config = new CorsConfiguration();
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .forEach(origin -> {
                if ("*".equals(origin)) {
                    config.addAllowedOriginPattern("*");
                } else {
                    config.addAllowedOrigin(origin);
                }
            });
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
