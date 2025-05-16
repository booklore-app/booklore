package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.entity.UserSettingEntity;
import com.adityachandel.booklore.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;

    public Optional<UserSettingEntity> getSetting(Long userId, String settingKey) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, settingKey);
    }

    public List<UserSettingEntity> getAllSettings(Long userId) {
        return userSettingRepository.findByUserId(userId);
    }

    @Transactional
    public UserSettingEntity saveOrUpdate(Long userId, String settingKey, String settingValue) {
        UserSettingEntity setting = userSettingRepository
                .findByUserIdAndSettingKey(userId, settingKey)
                .orElse(UserSettingEntity.builder()
                        .userId(userId)
                        .settingKey(settingKey)
                        .build());

        setting.setSettingValue(settingValue);
        return userSettingRepository.save(setting);
    }

    @Transactional
    public void deleteSetting(Long userId, String settingKey) {
        userSettingRepository.findByUserIdAndSettingKey(userId, settingKey).ifPresent(userSettingRepository::delete);
    }
}
