package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.BookCreatorService;
import com.adityachandel.booklore.util.FileUtils;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class CbxProcessor implements FileProcessor {

    private final BookRepository bookRepository;
    private final BookCreatorService bookCreatorService;
    private final BookMapper bookMapper;
    private final FileProcessingUtils fileProcessingUtils;
    private final BookMetadataRepository bookMetadataRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Book processFile(LibraryFile libraryFile, boolean forceProcess) {
        File bookFile = new File(libraryFile.getFileName());
        String fileName = bookFile.getName();

        if (!forceProcess) {
            Optional<BookEntity> bookOptional = bookRepository.findBookByFileNameAndLibraryId(fileName, libraryFile.getLibraryEntity().getId());
            return bookOptional.map(bookMapper::toBook).orElseGet(() -> processNewFile(libraryFile));
        } else {
            return processNewFile(libraryFile);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Book processNewFile(LibraryFile libraryFile) {
        BookEntity bookEntity = bookCreatorService.createShellBook(libraryFile, BookFileType.CBX);
        if (generateCover(bookEntity)) {
            fileProcessingUtils.setBookCoverPath(bookEntity.getId(), bookEntity.getMetadata());
        }
        setMetadata(bookEntity);
        bookCreatorService.saveConnections(bookEntity);
        bookEntity = bookRepository.save(bookEntity);
        bookRepository.flush();
        return bookMapper.toBook(bookEntity);
    }

    public boolean generateCover(BookEntity bookEntity) {
        File file = new File(FileUtils.getBookFullPath(bookEntity));
        try {
            List<BufferedImage> images = extractImagesFromArchive(file);
            if (images == null || images.isEmpty()) {
                log.warn("No image entries found in archive: {}", file.getName());
                return false;
            }
            BufferedImage coverImage = images.get(0);
            boolean saved = fileProcessingUtils.saveCoverImage(coverImage, bookEntity.getId());
            bookEntity.getMetadata().setCoverUpdatedOn(Instant.now());
            bookMetadataRepository.save(bookEntity.getMetadata());
            return saved;
        } catch (Exception e) {
            log.error("Error generating cover from archive {}, error: {}", bookEntity.getFileName(), e.getMessage());
            return false;
        }
    }

    private List<BufferedImage> extractImagesFromArchive(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".cbz")) {
            return extractImagesFromZip(file);
        } else if (name.endsWith(".cb7")) {
            return extractImagesFrom7z(file);
        } else if (name.endsWith(".cbr")) {
            return extractImagesFromRar(file);
        } else {
            log.warn("Unsupported archive format: {}", name);
            return List.of();
        }
    }

    private List<BufferedImage> extractImagesFromRar(File file) {
        List<BufferedImage> images = new ArrayList<>();
        try (Archive archive = new Archive(file)) {
            List<FileHeader> headers = archive.getFileHeaders();
            headers.stream()
                    .filter(h -> {
                        if (h.isDirectory()) return false;
                        String fileName = h.getFileNameString().replace("\\", "/").toLowerCase();
                        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp");
                    })
                    .sorted(Comparator.comparing(FileHeader::getFileNameString))
                    .forEach(header -> {
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            archive.extractFile(header, baos);
                            byte[] imageData = baos.toByteArray();
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
                            if (img != null) {
                                images.add(img);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to read image from RAR entry {}: {}", header.getFileNameString(), e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error extracting images from RAR archive {}: {}", file.getName(), e.getMessage());
            return List.of();
        }
        return images;
    }

    /**
     * Extracts image entries from a ZIP archive (.cbz), sorted by entry name.
     */
    private List<BufferedImage> extractImagesFromZip(File file) {
        List<BufferedImage> images = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            List<? extends ZipArchiveEntry> imageEntries = Collections.list(zipFile.getEntries()).stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().matches("(?i).*\\.(jpg|jpeg|png|webp)"))
                    .sorted(Comparator.comparing(ZipArchiveEntry::getName))
                    .toList();
            for (ZipArchiveEntry entry : imageEntries) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) {
                        images.add(img);
                    }
                } catch (Exception e) {
                    log.warn("Failed to read image from ZIP entry {}: {}", entry.getName(), e.getMessage());
                }
            }
            return images;
        } catch (Exception e) {
            log.error("Error extracting images from ZIP archive {}: {}", file.getName(), e.getMessage());
            return List.of();
        }
    }

    private List<BufferedImage> extractImagesFrom7z(File file) {
        List<BufferedImage> images = new ArrayList<>();
        try (SevenZFile sevenZFile = new SevenZFile(file)) {
            List<SevenZArchiveEntry> entries = new ArrayList<>();
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().matches("(?i).*\\.(jpg|jpeg|png|webp)")) {
                    entries.add(entry);
                }
            }
            entries.sort(Comparator.comparing(SevenZArchiveEntry::getName));
            for (SevenZArchiveEntry e : entries) {
                try {
                    byte[] content = new byte[(int) e.getSize()];
                    int offset = 0;
                    while (offset < content.length) {
                        int bytesRead = sevenZFile.read(content, offset, content.length - offset);
                        if (bytesRead < 0) {
                            break;
                        }
                        offset += bytesRead;
                    }
                    BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(content));
                    if (img != null) {
                        images.add(img);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to read image from 7z entry {}: {}", e.getName(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error extracting images from 7z archive {}: {}", file.getName(), e.getMessage());
            return List.of();
        }
        return images;
    }

    private void setMetadata(BookEntity bookEntity) {
        String baseName = new File(bookEntity.getFileName()).getName();
        String title = baseName.replaceAll("(?i)\\.cb[sz7]$", "").replaceAll("[_\\-]", " ").trim();
        bookEntity.getMetadata().setTitle(truncate(title, 1000));
    }

    private String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }
}