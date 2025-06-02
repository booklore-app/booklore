package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByName(String categoryName);

    List<CategoryEntity> findAllByIdIn(Set<Long> ids);
}

