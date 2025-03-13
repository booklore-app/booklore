package com.adityachandel.booklore.service.user;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.settings.BookPreferences;
import com.adityachandel.booklore.model.dto.UserCreateRequest;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.LibraryEntity;
import com.adityachandel.booklore.model.entity.ShelfEntity;
import com.adityachandel.booklore.model.entity.UserPermissionsEntity;
import com.adityachandel.booklore.repository.LibraryRepository;
import com.adityachandel.booklore.repository.ShelfRepository;
import com.adityachandel.booklore.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserCreatorService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;
    private final ShelfRepository shelfRepository;

    @Transactional
    public void registerUser(UserCreateRequest request) {
        Optional<BookLoreUserEntity> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw ApiError.USERNAME_ALREADY_TAKEN.createException(request.getUsername());
        }

        BookLoreUserEntity user = new BookLoreUserEntity();
        user.setUsername(request.getUsername());
        user.setDefaultPassword(true);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        UserPermissionsEntity permissions = new UserPermissionsEntity();
        permissions.setUser(user);
        permissions.setPermissionUpload(request.isPermissionUpload());
        permissions.setPermissionDownload(request.isPermissionDownload());
        permissions.setPermissionEditMetadata(request.isPermissionEditMetadata());
        permissions.setPermissionEmailBook(request.isPermissionEmailBook());
        user.setPermissions(permissions);

        user.setBookPreferences(buildDefaultBookPreferences());

        if (request.getSelectedLibraries() != null && !request.getSelectedLibraries().isEmpty()) {
            List<LibraryEntity> libraries = libraryRepository.findAllById(request.getSelectedLibraries());
            user.setLibraries(new ArrayList<>(libraries));
        }

        ShelfEntity shelfEntity = ShelfEntity.builder()
                .user(user)
                .name("Favorites")
                .icon("heart")
                .build();
        userRepository.save(user);
        shelfRepository.save(shelfEntity);
    }

    public void createAdminUser() {
        BookLoreUserEntity user = new BookLoreUserEntity();
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setDefaultPassword(true);
        user.setName("Administrator");
        user.setEmail("admin@email.com");

        UserPermissionsEntity permissions = new UserPermissionsEntity();
        permissions.setUser(user);
        permissions.setPermissionUpload(true);
        permissions.setPermissionDownload(true);
        permissions.setPermissionManipulateLibrary(true);
        permissions.setPermissionEditMetadata(true);
        permissions.setPermissionEmailBook(true);
        permissions.setPermissionAdmin(true);

        user.setPermissions(permissions);
        user.setBookPreferences(buildDefaultBookPreferences());

        ShelfEntity shelfEntity = ShelfEntity.builder()
                .user(user)
                .name("Favorites")
                .icon("heart")
                .build();

        userRepository.save(user);
        shelfRepository.save(shelfEntity);

        log.info("Created admin user {}", user.getUsername());
    }

    public boolean doesAdminUserExist() {
        return userRepository.findByUsername("admin").isPresent();
    }

    private BookPreferences buildDefaultBookPreferences() {
        return BookPreferences.builder()
                .perBookSetting(BookPreferences.PerBookSetting.builder()
                        .epub(BookPreferences.GlobalOrIndividual.Individual)
                        .pdf(BookPreferences.GlobalOrIndividual.Individual)
                        .build())
                .pdfReaderSetting(BookPreferences.PdfReaderSetting.builder()
                        .pageSpread("odd")
                        .pageZoom("page-fit")
                        .build())
                .epubReaderSetting(BookPreferences.EpubReaderSetting.builder()
                        .theme("white")
                        .font("serif")
                        .fontSize(150)
                        .build())
                .build();
    }

}