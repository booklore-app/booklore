package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.MagicShelfEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MagicShelfRepository extends JpaRepository<MagicShelfEntity, Long> {

    List<MagicShelfEntity> findAllByUserId(Long userId);

    Optional<MagicShelfEntity> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);
}
