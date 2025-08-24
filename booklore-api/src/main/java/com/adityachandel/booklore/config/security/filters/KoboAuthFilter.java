package com.adityachandel.booklore.config.security.filters;

import com.adityachandel.booklore.config.security.userdetails.UserAuthenticationDetails;
import com.adityachandel.booklore.mapper.custom.BookLoreUserTransformer;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.repository.KoboUserSettingsRepository;
import com.adityachandel.booklore.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class KoboAuthFilter extends OncePerRequestFilter {

    private final KoboUserSettingsRepository koboUserSettingsRepository;
    private final UserRepository userRepository;
    private final BookLoreUserTransformer bookLoreUserTransformer;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/kobo/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromPath(request.getRequestURI());
        if (token == null) {
            reject(response, "KOBO token missing", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var userTokenOpt = koboUserSettingsRepository.findByToken(token);
        if (userTokenOpt.isEmpty()) {
            log.warn("Invalid KOBO token: {}", token);
            reject(response, "Invalid KOBO token", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var entityOpt = userRepository.findById(userTokenOpt.get().getUserId());
        if (entityOpt.isEmpty()) {
            log.warn("User not found for token: {}", token);
            reject(response, "User not found", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var entity = entityOpt.get();
        if (entity.getPermissions() == null || !entity.getPermissions().isPermissionSyncKobo()) {
            log.warn("User {} does not have syncKobo permission", entity.getId());
            reject(response, "Insufficient permissions", HttpServletResponse.SC_FORBIDDEN);
            return;
        }


        BookLoreUser user = bookLoreUserTransformer.toDTO(entity);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, null);
        authentication.setDetails(new UserAuthenticationDetails(request, user.getId()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    private void reject(HttpServletResponse response, String message, int status) throws IOException {
        response.sendError(status, message);
    }
}