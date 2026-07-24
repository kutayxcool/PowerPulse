package com.powerpulse.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// /api/internal/** JWT ile degil, ayri bir paylasilan-sifre (shared
// secret) ile korunur - cunku bu uclar Sensors gibi tek bir
// kullaniciya ait olmayan, sistem geneli (tum kullanicilar dahil) bir
// gorunum donuyor; normal kullanici JWT'siyle bu kapsam mumkun degil.
// SecurityConfig bu path'i permitAll birakir, gercek koruma burada
// yapilir: header eksik/yanlissa istek daha controller'a ulasmadan
// 401 ile reddedilir.
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final String expectedApiKey;

    public InternalApiKeyFilter(
            @Value("${powerpulse.internal.api-key}") String expectedApiKey
    ) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            String providedKey = request.getHeader(API_KEY_HEADER);

            if (providedKey == null || !providedKey.equals(expectedApiKey)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"message\":\"Geçersiz veya eksik internal API anahtarı.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
