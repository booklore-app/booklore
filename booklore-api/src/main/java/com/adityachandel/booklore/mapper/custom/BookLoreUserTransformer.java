package com.adityachandel.booklore.mapper.custom;

import com.adityachandel.booklore.mapper.LibraryMapper;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.dto.settings.SidebarSortOption;
import com.adityachandel.booklore.model.dto.settings.UserSettingKey;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.UserSettingEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class BookLoreUserTransformer {

    private final ObjectMapper objectMapper;
    private final LibraryMapper libraryMapper;

    public BookLoreUser toDTO(BookLoreUserEntity userEntity) {
        BookLoreUser.UserPermissions permissions = new BookLoreUser.UserPermissions();
        permissions.setAdmin(userEntity.getPermissions().isPermissionAdmin());
        permissions.setCanUpload(userEntity.getPermissions().isPermissionUpload());
        permissions.setCanDownload(userEntity.getPermissions().isPermissionDownload());
        permissions.setCanEditMetadata(userEntity.getPermissions().isPermissionEditMetadata());
        permissions.setCanEmailBook(userEntity.getPermissions().isPermissionEmailBook());
        permissions.setCanManipulateLibrary(userEntity.getPermissions().isPermissionManipulateLibrary());

        BookLoreUser bookLoreUser = new BookLoreUser();
        bookLoreUser.setId(userEntity.getId());
        bookLoreUser.setUsername(userEntity.getUsername());
        bookLoreUser.setName(userEntity.getName());
        bookLoreUser.setEmail(userEntity.getEmail());
        bookLoreUser.setDefaultPassword(userEntity.isDefaultPassword());
        bookLoreUser.setPermissions(permissions);

        BookLoreUser.UserSettings userSettings = new BookLoreUser.UserSettings();

        for (UserSettingEntity settingEntity : userEntity.getSettings()) {
            String key = settingEntity.getSettingKey();
            String jsonValue = settingEntity.getSettingValue();

            try {
                UserSettingKey settingKey = UserSettingKey.fromDbKey(key);

                switch (settingKey) {
                    case PER_BOOK_SETTING -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.PerBookSetting.class);
                        userSettings.setPerBookSetting(value);
                    }
                    case PDF_READER_SETTING -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.PdfReaderSetting.class);
                        userSettings.setPdfReaderSetting(value);
                    }
                    case EPUB_READER_SETTING -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.EpubReaderSetting.class);
                        userSettings.setEpubReaderSetting(value);
                    }
                    case CBX_READER_SETTING -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.CbxReaderSetting.class);
                        userSettings.setCbxReaderSetting(value);
                    }
                    case NEW_PDF_READER_SETTING -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.NewPdfReaderSetting.class);
                        userSettings.setNewPdfReaderSetting(value);
                    }
                    case SIDEBAR_LIBRARY_SORTING -> {
                        var value = objectMapper.readValue(jsonValue, SidebarSortOption.class);
                        userSettings.setSidebarLibrarySorting(value);
                    }
                    case SIDEBAR_SHELF_SORTING -> {
                        var value = objectMapper.readValue(jsonValue, SidebarSortOption.class);
                        userSettings.setSidebarShelfSorting(value);
                    }
                    case ENTITY_VIEW_PREFERENCES -> {
                        var value = objectMapper.readValue(jsonValue, BookLoreUser.UserSettings.EntityViewPreferences.class);
                        userSettings.setEntityViewPreferences(value);
                    }
                }

            } catch (IllegalArgumentException e) {
                log.warn("Unknown setting key encountered: {}", key);
            } catch (Exception e) {
                log.error("Failed to deserialize setting '{}': {}", key, e.getMessage(), e);
            }
        }

        bookLoreUser.setUserSettings(userSettings);
        bookLoreUser.setAssignedLibraries(userEntity.getLibraries().stream().map(libraryMapper::toLibrary).collect(Collectors.toList()));
        bookLoreUser.setProvisioningMethod(userEntity.getProvisioningMethod());
        return bookLoreUser;
    }
}