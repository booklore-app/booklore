package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.settings.PublicAppSetting;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public-settings")
@RequiredArgsConstructor
public class PublicAppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping
    public PublicAppSetting getPublicSettings() {
        return appSettingService.getPublicSettings();
    }
}
