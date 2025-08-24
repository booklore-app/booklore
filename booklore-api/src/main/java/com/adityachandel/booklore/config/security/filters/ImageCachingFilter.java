package com.adityachandel.booklore.config.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ImageCachingFilter extends OncePerRequestFilter {

    private static final long CACHE_DURATION_MS = 3600_000L; // 1 hour
    private static final Pattern BOOK_COVER_PATTERN = Pattern.compile("/api/v1/books/\\d+/cover.*");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !BOOK_COVER_PATTERN.matcher(uri).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (BOOK_COVER_PATTERN.matcher(uri).matches()) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
            response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + CACHE_DURATION_MS);
        }
        filterChain.doFilter(request, response);
    }

    @Bean
    public FilterRegistrationBean<ImageCachingFilter> imageCachingFilterRegistration(ImageCachingFilter filter) {
        FilterRegistrationBean<ImageCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/v1/books/*/cover");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}