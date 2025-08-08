package com.adityachandel.booklore.repository;


import com.adityachandel.booklore.model.entity.KoreaderUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KoreaderUserRepository extends JpaRepository<KoreaderUserEntity, Long> {
    Optional<KoreaderUserEntity> findByUsername(String username);

    Optional<KoreaderUserEntity> findByBookLoreUserId(Long bookLoreUserId);
}
