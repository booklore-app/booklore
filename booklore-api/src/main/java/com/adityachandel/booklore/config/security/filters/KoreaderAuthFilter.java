package com.adityachandel.booklore.config.security.filters;

import com.adityachandel.booklore.config.security.userdetails.KoreaderUserDetails;
import com.adityachandel.booklore.repository.KoreaderUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class KoreaderAuthFilter extends OncePerRequestFilter {

    private final KoreaderUserRepository koreaderUserRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/koreader/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String username = request.getHeader("x-auth-user");
        String key = request.getHeader("x-auth-key");

        if (username == null || key == null) {
            log.warn("Missing KOReader headers");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication headers");
            return;
        }

        var userOpt = koreaderUserRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("KOReader user '{}' not found", username);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user");
            return;
        }

        var user = userOpt.get();
        if (!user.getPasswordMD5().equalsIgnoreCase(key)) {
            log.warn("KOReader auth failed: password mismatch for user '{}'", username);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
        }

        if (!user.isSyncEnabled()) {
            log.warn("KOReader user '{}' is not allowed to sync", username);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Sync not allowed for user");
            return;
        }

        Long bookLoreUserId = user.getBookLoreUser() != null ? user.getBookLoreUser().getId() : null;

        UserDetails userDetails = new KoreaderUserDetails(user.getUsername(), user.getPasswordMD5(), true, bookLoreUserId, List.of());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, null);

        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}