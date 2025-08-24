package com.adityachandel.booklore.config.security.filters;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        if (req.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(req, res);
            return;
        }

        long start = System.currentTimeMillis();

        try {
            log.info("Incoming req: {} {} from IP {}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
            log.info("Full req URI: {}", ServletUriComponentsBuilder.fromRequest(req).toUriString());

            Enumeration<String> headerNames = req.getHeaderNames();
            if (headerNames != null) {
                Collections.list(headerNames).forEach(name -> {
                    String value = req.getHeader(name);
                    log.info("Header: {}={}", name, value);
                });
            }

            filterChain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("Completed {} {} with status {} in {} ms", req.getMethod(), req.getRequestURI(), res.getStatus(), duration);
        }
    }
}