package com.agora.booking.management.security;

import com.agora.booking.management.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Ambil token dari header Authorization
        String token = extractTokenFromRequest(request);

        // 2. Jika token ada dan valid, set authentication
        if (StringUtils.hasText(token) && jwtUtil.isTokenValid(token)) {

            String email = jwtUtil.extractEmail(token);

            // 3. Load user dari DB berdasarkan email
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 4. Buat authentication object dan set ke Security Context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication set for user: {}", email);
        }

        // 5. Lanjut ke filter berikutnya
        filterChain.doFilter(request, response);
    }

    // =============================================
    // Extract token dari header "Authorization: Bearer <token>"
    // =============================================
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // hapus prefix "Bearer "
        }

        return null;
    }
}