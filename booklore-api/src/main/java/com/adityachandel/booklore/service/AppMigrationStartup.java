package com.adityachandel.booklore.service;

import com.adityachandel.booklore.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppMigrationStartup {

    private final AppMigrationService appMigrationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrationsOnce() {
        appMigrationService.populateMissingFileSizesOnce();
        appMigrationService.addCbxReaderSettingToExistingUsers(userRepository, objectMapper);
        appMigrationService.addNewPdfReaderSettingToExistingUsers(userRepository, objectMapper);
    }
}
