package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.UserEmailProviderEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEmailProviderRepository extends JpaRepository<UserEmailProviderEntity, Long> {
    List<UserEmailProviderEntity> findByUserId(Long userId);

    @Transactional
    void deleteByUserId(Long userId);
}
