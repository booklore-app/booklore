package com.adityachandel.booklore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@Profile({"dev"})
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();

        log.info("Incoming request: {} {} from IP {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ServletUriComponentsBuilder servletUriComponentsBuilder = ServletUriComponentsBuilder
                .fromCurrentContextPath();

        log.info("servletUriComponentsBuilder.toUriString(): {}", servletUriComponentsBuilder.toUriString());

        var headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.info("Header: {}={}", headerName, headerValue);
            }
        }

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        log.info("Completed {} {} with status {} in {} ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }
}