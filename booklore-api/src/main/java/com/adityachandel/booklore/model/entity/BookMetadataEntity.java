package com.adityachandel.booklore.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "book_metadata")
public class BookMetadataEntity {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "series_name")
    private String seriesName;

    @Column(name = "series_number")
    private Integer seriesNumber;

    @Column(name = "series_total")
    private Integer seriesTotal;

    @Column(name = "isbn_13", length = 13)
    private String isbn13;

    @Column(name = "isbn_10", length = 10)
    private String isbn10;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "thumbnail", length = 1000)
    private String thumbnail;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "cover_updated_on")
    private Instant coverUpdatedOn;

    // Locking fields
    @Column(name = "all_fields_locked")
    private Boolean allFieldsLocked = Boolean.FALSE;

    @Column(name = "title_locked")
    private Boolean titleLocked = Boolean.FALSE;

    @Column(name = "subtitle_locked")
    private Boolean subtitleLocked = Boolean.FALSE;

    @Column(name = "publisher_locked")
    private Boolean publisherLocked = Boolean.FALSE;

    @Column(name = "published_date_locked")
    private Boolean publishedDateLocked = Boolean.FALSE;

    @Column(name = "description_locked")
    private Boolean descriptionLocked = Boolean.FALSE;

    @Column(name = "isbn_13_locked")
    private Boolean isbn13Locked = Boolean.FALSE;

    @Column(name = "isbn_10_locked")
    private Boolean isbn10Locked = Boolean.FALSE;

    @Column(name = "page_count_locked")
    private Boolean pageCountLocked = Boolean.FALSE;

    @Column(name = "thumbnail_locked")
    private Boolean thumbnailLocked = Boolean.FALSE;

    @Column(name = "language_locked")
    private Boolean languageLocked = Boolean.FALSE;

    @Column(name = "rating_locked")
    private Boolean ratingLocked = Boolean.FALSE;

    @Column(name = "review_count_locked")
    private Boolean reviewCountLocked = Boolean.FALSE;

    @Column(name = "cover_locked")
    private Boolean coverLocked = Boolean.FALSE;

    @Column(name = "series_name_locked")
    private Boolean seriesNameLocked = Boolean.FALSE;

    @Column(name = "series_number_locked")
    private Boolean seriesNumberLocked = Boolean.FALSE;

    @Column(name = "series_total_locked")
    private Boolean seriesTotalLocked = Boolean.FALSE;

    @Column(name = "authors_locked")
    private Boolean authorsLocked = Boolean.FALSE;

    @Column(name = "categories_locked")
    private Boolean categoriesLocked = Boolean.FALSE;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "book_id")
    @JsonIgnore
    private BookEntity book;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_metadata_author_mapping",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private List<AuthorEntity> authors;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_metadata_category_mapping",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<CategoryEntity> categories;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "book")
    private List<BookAwardEntity> awards;


    public void applyLockToAllFields(boolean lock) {
        this.allFieldsLocked = lock;
        this.titleLocked = lock;
        this.subtitleLocked = lock;
        this.publisherLocked = lock;
        this.publishedDateLocked = lock;
        this.descriptionLocked = lock;
        this.isbn13Locked = lock;
        this.isbn10Locked = lock;
        this.pageCountLocked = lock;
        this.thumbnailLocked = lock;
        this.languageLocked = lock;
        this.ratingLocked = lock;
        this.reviewCountLocked = lock;
        this.coverLocked = lock;
        this.seriesNameLocked = lock;
        this.seriesNumberLocked = lock;
        this.seriesTotalLocked = lock;
        this.authorsLocked = lock;
        this.categoriesLocked = lock;
    }
}