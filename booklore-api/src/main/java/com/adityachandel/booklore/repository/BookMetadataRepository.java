package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.dto.Series;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface BookMetadataRepository extends JpaRepository<BookMetadataEntity, Long> {


    @Query("SELECT m FROM BookMetadataEntity m WHERE m.bookId IN :bookIds")
    List<BookMetadataEntity> findAllByBookIds(@Param("bookIds") Set<Long> bookIds);

    @Query("SELECT m FROM BookMetadataEntity m WHERE m.bookId IN :bookIds")
    List<BookMetadataEntity> getMetadataForBookIds(@Param("bookIds") Set<Long> bookIds);

    @Query("SELECT m FROM BookMetadataEntity m WHERE m.bookId IN :bookIds")
    List<BookMetadataEntity> getMetadataForBookIds(@Param("bookIds") List<Long> bookIds);


    @EntityGraph(attributePaths = {"authors", "categories"})
    List<BookMetadataEntity> findAllByBookIdIn(Set<Long> bookIds);

	
    @Query("SELECT new com.adityachandel.booklore.model.dto.Series(m.seriesName, COUNT(m)) " +
            "FROM BookMetadataEntity m WHERE m.seriesName IS NOT NULL " +
            "GROUP BY m.seriesName ORDER BY m.seriesName")
    List<Series> findAllSeriesWithBookCount();    
}
