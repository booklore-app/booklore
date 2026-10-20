package org.booklore.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.booklore.BookloreApplication;
import org.booklore.model.MetadataClearFlags;
import org.booklore.model.MetadataUpdateContext;
import org.booklore.model.MetadataUpdateWrapper;
import org.booklore.model.dto.BookMetadata;
import org.booklore.model.enums.MetadataReplaceMode;
import org.booklore.service.metadata.BookMetadataUpdater;
import org.booklore.model.entity.AuthorEntity;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.entity.BookMetadataEntity;
import org.booklore.model.entity.LibraryEntity;
import org.booklore.model.entity.LibraryPathEntity;
import org.booklore.service.task.TaskCronService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * LOC-539: the generated cover renders "Unknown Author", and the reporter also sees the author
 * disappear from Edit Metadata after a page refresh. Both symptoms require
 * BookMetadataEntity.authors to come back empty, so these tests pin down whether the association
 * survives a write/read round trip at all, and whether each read path used by the affected
 * features returns it.
 */
@SpringBootTest(classes = {BookloreApplication.class})
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:authtestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.path-config=build/tmp/test-config",
        "app.bookdrop-folder=build/tmp/test-bookdrop",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.task.scheduling.enabled=false",
        "app.task.scan-library-cron=*/1 * * * * *",
        "app.task.process-bookdrop-cron=*/1 * * * * *",
        "app.features.oidc-enabled=false"
})
@Import(BookMetadataAuthorPersistenceTest.TestConfig.class)
class BookMetadataAuthorPersistenceTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookMetadataUpdater bookMetadataUpdater;

    @PersistenceContext
    private EntityManager entityManager;

    @org.springframework.boot.test.context.TestConfiguration
    public static class TestConfig {
        @Bean("flyway")
        @Primary
        public org.flywaydb.core.Flyway flyway() {
            return mock(org.flywaydb.core.Flyway.class);
        }

        @Bean
        @Primary
        public TaskCronService taskCronService() {
            return mock(TaskCronService.class);
        }
    }

    private Long seedBookWithAuthors(String... authorNames) {
        LibraryEntity library = LibraryEntity.builder().name("Test Library").icon("book").watch(false).build();
        entityManager.persist(library);

        LibraryPathEntity libraryPath = LibraryPathEntity.builder().library(library).path("/test/path").build();
        entityManager.persist(libraryPath);

        BookEntity book = BookEntity.builder()
                .library(library)
                .libraryPath(libraryPath)
                .addedOn(Instant.now())
                .deleted(false)
                .build();
        entityManager.persist(book);

        List<AuthorEntity> authors = new ArrayList<>();
        for (String name : authorNames) {
            AuthorEntity author = AuthorEntity.builder().name(name).build();
            entityManager.persist(author);
            authors.add(author);
        }

        BookMetadataEntity metadata = BookMetadataEntity.builder()
                .book(book)
                .bookId(book.getId())
                .title("Test Title")
                .authors(authors)
                .build();
        entityManager.persist(metadata);

        entityManager.flush();
        entityManager.clear();
        return book.getId();
    }

    @Test
    void authorsSurviveAWriteReadRoundTrip() {
        Long bookId = seedBookWithAuthors("Helen Woodruff");

        BookEntity reloaded = entityManager.find(BookEntity.class, bookId);

        assertThat(reloaded.getMetadata().getAuthors())
                .extracting(AuthorEntity::getName)
                .containsExactly("Helen Woodruff");
    }

    @Test
    void authorsLoadOnTheCoverGenerationReadPath() {
        Long bookId = seedBookWithAuthors("Helen Woodruff");

        BookEntity book = bookRepository.findByIdWithAuthors(bookId).orElseThrow();

        assertThat(book.getMetadata().getAuthors())
                .extracting(AuthorEntity::getName)
                .containsExactly("Helen Woodruff");
    }

    @Test
    void authorsLoadOnTheAudiobookCoverReadPath() {
        Long bookId = seedBookWithAuthors("Helen Woodruff", "Second Author");

        BookEntity book = bookRepository.findByIdWithBookFiles(bookId).orElseThrow();

        assertThat(book.getMetadata().getAuthors())
                .extracting(AuthorEntity::getName)
                .containsExactly("Helen Woodruff", "Second Author");
    }

    /**
     * findAllWithMetadata backs GET /api/v1/books, which is what the UI reloads on a page
     * refresh. Its @EntityGraph does not name metadata.authors, so the list arrives through the
     * @Fetch(SUBSELECT) lazy path.
     */
    @Test
    void authorsLoadOnThePageRefreshReadPath() {
        Long bookId = seedBookWithAuthors("Helen Woodruff");

        BookEntity book = bookRepository.findAllWithMetadata().stream()
                .filter(b -> b.getId().equals(bookId))
                .findFirst()
                .orElseThrow();

        assertThat(book.getMetadata().getAuthors())
                .extracting(AuthorEntity::getName)
                .containsExactly("Helen Woodruff");
    }

    /**
     * The reporter's actual symptom: add an author in Edit Metadata, save, refresh, author gone.
     * This drives the same call MetadataController.updateMetadata makes - REPLACE_ALL,
     * mergeCategories=false, and clearFlags with authors=false - against a book that starts with
     * no authors, then reads it back from a clean persistence context.
     */
    @Test
    void addingAnAuthorThroughTheEditorSavePathIsPersisted() {
        Long bookId = seedBookWithAuthors();

        BookEntity bookEntity = bookRepository.findAllWithMetadataByIds(Set.of(bookId)).stream()
                .findFirst()
                .orElseThrow();
        assertThat(bookEntity.getMetadata().getAuthors()).isEmpty();

        BookMetadata newMetadata = new BookMetadata();
        newMetadata.setBookId(bookId);
        newMetadata.setTitle("Test Title");
        newMetadata.setAuthors(List.of("Helen Woodruff"));

        bookMetadataUpdater.setBookMetadata(MetadataUpdateContext.builder()
                .bookEntity(bookEntity)
                .metadataUpdateWrapper(MetadataUpdateWrapper.builder()
                        .metadata(newMetadata)
                        .clearFlags(new MetadataClearFlags())
                        .build())
                .updateThumbnail(false)
                .mergeCategories(false)
                .mergeMoods(false)
                .mergeTags(false)
                .replaceMode(MetadataReplaceMode.REPLACE_ALL)
                .build());

        entityManager.flush();
        entityManager.clear();

        BookEntity reloaded = bookRepository.findByIdWithAuthors(bookId).orElseThrow();
        assertThat(reloaded.getMetadata().getAuthors())
                .extracting(AuthorEntity::getName)
                .containsExactly("Helen Woodruff");
    }
}
