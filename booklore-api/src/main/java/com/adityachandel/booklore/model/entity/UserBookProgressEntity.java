package com.adityachandel.booklore.model.entity;

import com.adityachandel.booklore.model.enums.ReadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_book_progress")
public class UserBookProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BookLoreUserEntity user;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @Column(name = "last_read_time")
    private Instant lastReadTime;

    @Column(name = "pdf_progress")
    private Integer pdfProgress;

    @Column(name = "pdf_progress_percent")
    private Float pdfProgressPercent;

    @Column(name = "epub_progress", length = 1000)
    private String epubProgress;

    @Column(name = "epub_progress_percent")
    private Float epubProgressPercent;

    @Column(name = "cbx_progress")
    private Integer cbxProgress;

    @Column(name = "cbx_progress_percent")
    private Float cbxProgressPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_status")
    private ReadStatus readStatus;

    @Column(name = "date_finished")
    private Instant dateFinished;
}
