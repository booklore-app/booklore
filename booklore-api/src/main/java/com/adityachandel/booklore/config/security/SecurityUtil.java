package com.adityachandel.booklore.config.security;

import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.repository.ShelfRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component("securityUtil")
public class SecurityUtil {

    private final ShelfRepository shelfRepository;

    public boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isAdmin();
        }
        return false;
    }

    public boolean isSelf(Long userId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getId().equals(userId);
        }
        return false;
    }

    public boolean canUpload() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isCanUpload();
        }
        return false;
    }

    public boolean canManipulateLibrary() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isCanManipulateLibrary();
        }
        return false;
    }

    public boolean canEditMetadata() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isCanEditMetadata();
        }
        return false;
    }

    public boolean canEmailBook() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isCanEmailBook();
        }
        return false;
    }

    public boolean canViewUserProfile(Long userId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return user.getPermissions().isAdmin() || user.getId().equals(userId);
        }
        return false;
    }

    public boolean isShelfOwner(Long shelfId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BookLoreUser user) {
            return shelfRepository.findById(shelfId)
                    .map(shelf -> shelf.getUser().getId().equals(user.getId()))
                    .orElse(false);
        }
        return false;
    }
}
