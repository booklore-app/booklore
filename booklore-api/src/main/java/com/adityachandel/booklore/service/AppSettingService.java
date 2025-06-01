package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.AppProperties;
import com.adityachandel.booklore.model.dto.request.MetadataRefreshOptions;
import com.adityachandel.booklore.model.dto.settings.*;
import com.adityachandel.booklore.model.entity.AppSettingEntity;
import com.adityachandel.booklore.repository.AppSettingsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppProperties appProperties;
    private final AppSettingsRepository appSettingsRepository;
    private final ObjectMapper objectMapper;

    private volatile AppSettings appSettings;
    private final ReentrantLock lock = new ReentrantLock();

    public AppSettings getAppSettings() {
        if (appSettings == null) {
            lock.lock();
            try {
                if (appSettings == null) {
                    appSettings = buildAppSettings();
                }
            } finally {
                lock.unlock();
            }
        }
        return appSettings;
    }

    @Transactional
    public void updateSetting(AppSettingKey key, Object val) throws JsonProcessingException {
        AppSettingEntity setting = appSettingsRepository.findByName(key.toString());
        if (setting == null) {
            setting = new AppSettingEntity();
            setting.setName(key.toString());
        }
        setting.setVal(serializeSettingValue(key, val));
        appSettingsRepository.save(setting);
        refreshCache();
    }

    private void refreshCache() {
        lock.lock();
        try {
            appSettings = buildAppSettings();
        } finally {
            lock.unlock();
        }
    }

    private AppSettings buildAppSettings() {
        Map<String, String> settingsMap = appSettingsRepository.findAll().stream().collect(Collectors.toMap(AppSettingEntity::getName, AppSettingEntity::getVal));

        AppSettings.AppSettingsBuilder builder = AppSettings.builder();
        builder.remoteAuthEnabled(appProperties.getRemoteAuth().isEnabled());

        builder.metadataRefreshOptions(getJsonSetting(settingsMap, AppSettingKey.QUICK_BOOK_MATCH, MetadataRefreshOptions.class, null, false));
        builder.oidcProviderDetails(getJsonSetting(settingsMap, AppSettingKey.OIDC_PROVIDER_DETAILS, OidcProviderDetails.class, null, false));
        builder.oidcAutoProvisionDetails(getJsonSetting(settingsMap, AppSettingKey.OIDC_AUTO_PROVISION_DETAILS, OidcAutoProvisionDetails.class, null, false));
        builder.metadataProviderSettings(getJsonSetting(settingsMap, AppSettingKey.METADATA_PROVIDER_SETTINGS, MetadataProviderSettings.class, getMetadataProviderSettings(), true));

        builder.coverResolution(getOrCreateSetting(AppSettingKey.COVER_IMAGE_RESOLUTION, "250x350"));
        builder.autoBookSearch(Boolean.parseBoolean(getOrCreateSetting(AppSettingKey.AUTO_BOOK_SEARCH, "true")));
        builder.uploadPattern(getOrCreateSetting(AppSettingKey.UPLOAD_FILE_PATTERN, ""));
        builder.similarBookRecommendation(Boolean.parseBoolean(getOrCreateSetting(AppSettingKey.SIMILAR_BOOK_RECOMMENDATION, "true")));
        builder.opdsServerEnabled(Boolean.parseBoolean(getOrCreateSetting(AppSettingKey.OPDS_SERVER_ENABLED, "false")));
        builder.oidcEnabled(Boolean.parseBoolean(getOrCreateSetting(AppSettingKey.OIDC_ENABLED, "false")));
        builder.cbxCacheSizeInMb(Integer.parseInt(getOrCreateSetting(AppSettingKey.CBX_CACHE_SIZE_IN_MB, "5120")));
        builder.pdfCacheSizeInMb(Integer.parseInt(getOrCreateSetting(AppSettingKey.PDF_CACHE_SIZE_IN_MB, "5120")));
        builder.maxFileUploadSizeInMb(Integer.parseInt(getOrCreateSetting(AppSettingKey.MAX_FILE_UPLOAD_SIZE_IN_MB, "100")));

        return builder.build();
    }

    private String getOrCreateSetting(AppSettingKey key, String defaultValue) {
        AppSettingEntity setting = appSettingsRepository.findByName(key.toString());
        if (setting != null) {
            return setting.getVal();
        }
        saveDefaultSetting(key, defaultValue);
        return defaultValue;
    }

    private void saveDefaultSetting(AppSettingKey key, String value) {
        AppSettingEntity setting = new AppSettingEntity();
        setting.setName(key.toString());
        setting.setVal(value);
        appSettingsRepository.save(setting);
    }

    private <T> T getJsonSetting(Map<String, String> settingsMap, AppSettingKey key, Class<T> clazz, T defaultValue, boolean persistDefault) {
        String json = settingsMap.get(key.toString());
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse " + key.toString(), e);
            }
        }
        if (defaultValue != null && persistDefault) {
            try {
                saveDefaultSetting(key, objectMapper.writeValueAsString(defaultValue));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to persist default for " + key.toString(), e);
            }
        }
        return defaultValue;
    }

    private String serializeSettingValue(AppSettingKey key, Object val) throws JsonProcessingException {
        return key.isJson() ? objectMapper.writeValueAsString(val) : val.toString();
    }

    private static MetadataProviderSettings getMetadataProviderSettings() {
        MetadataProviderSettings defaultMetadataProviderSettings = new MetadataProviderSettings();

        MetadataProviderSettings.Amazon defaultAmazon = new MetadataProviderSettings.Amazon();
        defaultAmazon.setEnabled(true);
        defaultAmazon.setCookie(null);
        MetadataProviderSettings.Google defaultGoogle = new MetadataProviderSettings.Google();
        defaultGoogle.setEnabled(true);
        MetadataProviderSettings.Goodreads defaultGoodreads = new MetadataProviderSettings.Goodreads();
        defaultGoodreads.setEnabled(true);
        MetadataProviderSettings.Hardcover defaultHardcover = new MetadataProviderSettings.Hardcover();
        defaultHardcover.setEnabled(false);
        defaultHardcover.setApiKey(null);

        defaultMetadataProviderSettings.setAmazon(defaultAmazon);
        defaultMetadataProviderSettings.setGoogle(defaultGoogle);
        defaultMetadataProviderSettings.setGoodReads(defaultGoodreads);
        defaultMetadataProviderSettings.setHardcover(defaultHardcover);
        return defaultMetadataProviderSettings;
    }
}