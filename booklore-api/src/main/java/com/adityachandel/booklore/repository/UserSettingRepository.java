package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.UserSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSettingEntity, Long> {
    Optional<UserSettingEntity> findByUserIdAndSettingKey(Long userId, String settingKey);
    List<UserSettingEntity> findByUserId(Long userId);
}
