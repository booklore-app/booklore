package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.dto.settings.UserSettingKey;
import com.adityachandel.booklore.model.entity.AppMigrationEntity;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.UserSettingEntity;
import com.adityachandel.booklore.repository.AppMigrationRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.repository.UserRepository;
import com.adityachandel.booklore.service.user.UserProvisioningService;
import com.adityachandel.booklore.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
@Service
public class AppMigrationService {

    private AppMigrationRepository migrationRepository;
    private BookRepository bookRepository;
    private UserProvisioningService userProvisioningService;

    @Transactional
    public void populateMissingFileSizesOnce() {
        if (migrationRepository.existsById("populateFileSizes")) {
            return;
        }

        List<BookEntity> books = bookRepository.findByFileSizeKbIsNull();
        for (BookEntity book : books) {
            Long sizeInKb = FileUtils.getFileSizeInKb(book);
            if (sizeInKb != null) {
                book.setFileSizeKb(sizeInKb);
            }
        }
        bookRepository.saveAll(books);

        log.info("Starting migration 'populateFileSizes' for {} books.", books.size());
        AppMigrationEntity migration = new AppMigrationEntity();
        migration.setKey("populateFileSizes");
        migration.setExecutedAt(LocalDateTime.now());
        migration.setDescription("Populate file size for existing books");
        migrationRepository.save(migration);
        log.info("Migration 'populateFileSizes' executed successfully.");
    }

    @Transactional
    public void addCbxReaderSettingToExistingUsers(UserRepository userRepository, ObjectMapper objectMapper) {
        final String migrationKey = "addCbxReaderSetting";

        if (migrationRepository.existsById(migrationKey)) {
            return;
        }

        List<BookLoreUserEntity> users = userRepository.findAll();
        int updatedCount = 0;
        AtomicInteger perBookUpdatedCount = new AtomicInteger();

        for (BookLoreUserEntity user : users) {
            boolean hasCbxSetting = user.getSettings().stream().anyMatch(s -> s.getSettingKey().equals(UserSettingKey.CBX_READER_SETTING.getDbKey()));

            if (!hasCbxSetting) {
                try {
                    UserSettingEntity setting = UserSettingEntity.builder()
                            .user(user)
                            .settingKey(UserSettingKey.CBX_READER_SETTING.getDbKey())
                            .settingValue(objectMapper.writeValueAsString(userProvisioningService.buildDefaultCbxReaderSetting()))
                            .build();
                    user.getSettings().add(setting);
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to create CBX setting for user {}", user.getUsername(), e);
                }
            }

            user.getSettings().stream()
                    .filter(s -> s.getSettingKey().equals(UserSettingKey.PER_BOOK_SETTING.getDbKey()))
                    .findFirst()
                    .ifPresent(setting -> {
                        try {
                            var current = objectMapper.readValue(setting.getSettingValue(), BookLoreUser.UserSettings.PerBookSetting.class);
                            if (current.getCbx() == null) {
                                current.setCbx(BookLoreUser.UserSettings.PerBookSetting.GlobalOrIndividual.Individual);
                                setting.setSettingValue(objectMapper.writeValueAsString(current));
                                perBookUpdatedCount.getAndIncrement();
                            }
                        } catch (Exception e) {
                            log.error("Failed to update PER_BOOK_SETTING for user {}", user.getUsername(), e);
                        }
                    });
        }

        userRepository.saveAll(users);

        AppMigrationEntity migration = new AppMigrationEntity();
        migration.setKey(migrationKey);
        migration.setExecutedAt(LocalDateTime.now());
        migration.setDescription("Add CBX reader setting and update PER_BOOK_SETTING to include CBX for existing users");
        migrationRepository.save(migration);

        log.info("Migration '{}' completed. Added CBX setting to {} users and updated PER_BOOK_SETTING for {} users.", migrationKey, updatedCount, perBookUpdatedCount);
    }


    @Transactional
    public void addNewPdfReaderSettingToExistingUsers(UserRepository userRepository, ObjectMapper objectMapper) {
        final String migrationKey = "addNewPdfReaderSetting";

        if (migrationRepository.existsById(migrationKey)) {
            return;
        }

        List<BookLoreUserEntity> users = userRepository.findAll();
        int updatedCount = 0;
        AtomicInteger perBookUpdatedCount = new AtomicInteger();

        for (BookLoreUserEntity user : users) {
            boolean hasNewPdfSetting = user.getSettings().stream().anyMatch(s -> s.getSettingKey().equals(UserSettingKey.NEW_PDF_READER_SETTING.getDbKey()));

            if (!hasNewPdfSetting) {
                try {
                    UserSettingEntity setting = UserSettingEntity.builder()
                            .user(user)
                            .settingKey(UserSettingKey.NEW_PDF_READER_SETTING.getDbKey())
                            .settingValue(objectMapper.writeValueAsString(userProvisioningService.buildDefaultNewPdfReaderSetting()))
                            .build();
                    user.getSettings().add(setting);
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to create New PDF setting for user {}", user.getUsername(), e);
                }
            }
        }

        userRepository.saveAll(users);

        AppMigrationEntity migration = new AppMigrationEntity();
        migration.setKey(migrationKey);
        migration.setExecutedAt(LocalDateTime.now());
        migration.setDescription("Add New PDF reader setting and update PER_BOOK_SETTING to include CBX for existing users");
        migrationRepository.save(migration);

        log.info("Migration '{}' completed. Added New PDF setting to {} users and updated PER_BOOK_SETTING for {} users.", migrationKey, updatedCount, perBookUpdatedCount);
    }
}
