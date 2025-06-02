package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.BookMetadataAuthorMapping;
import com.adityachandel.booklore.model.entity.BookMetadataAuthorKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BookMetadataAuthorMappingRepository extends JpaRepository<BookMetadataAuthorMapping, BookMetadataAuthorKey> {

    List<BookMetadataAuthorMapping> findAllByBookIdIn(Set<Long> bookIds);
}
