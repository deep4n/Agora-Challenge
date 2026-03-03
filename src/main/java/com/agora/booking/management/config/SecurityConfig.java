package com.agora.booking.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // =============================================
    // BCrypt Bean — dipakai oleh AuthService
    // untuk hash password sebelum disimpan ke DB
    // =============================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =============================================
    // Security Filter Chain
    // =============================================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — tidak dibutuhkan untuk REST API stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — tidak ada session/cookie, pakai JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Atur akses endpoint
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — tidak butuh token
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Swagger UI — public untuk kemudahan review
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**")
                        .permitAll()

                        // Semua endpoint lain wajib autentikasi
                        .anyRequest().authenticated());

        return http.build();
    }
}