package com.adityachandel.booklore.config.security.filters;

import com.adityachandel.booklore.config.security.utils.JwtUtils;
import com.adityachandel.booklore.config.security.userdetails.UserAuthenticationDetails;
import com.adityachandel.booklore.mapper.custom.BookLoreUserTransformer;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final BookLoreUserTransformer bookLoreUserTransformer;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {

        String token = extractJwt(req);
        if (token == null) {
            chain.doFilter(req, res);
            return;
        }

        if (!jwtUtils.validateToken(token)) {
            log.warn("Invalid JWT token from request: {}", req.getRequestURI());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        }

        Long userId = jwtUtils.extractUserId(token);
        BookLoreUserEntity entity = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        BookLoreUser user = bookLoreUserTransformer.toDTO(entity);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, null);

        authentication.setDetails(new UserAuthenticationDetails(req, user.getId()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(req, res);
    }

    private String extractJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}