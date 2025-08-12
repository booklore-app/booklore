package com.adityachandel.booklore.service.metadata.extractor;

import com.adityachandel.booklore.model.dto.BookMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CbxMetadataExtractorTest {

    private final CbxMetadataExtractor extractor = new CbxMetadataExtractor();

    private File createCbz(String fileName, String entryName, String xmlContent) throws IOException {
        Path dir = Files.createTempDirectory("cbz");
        Path file = dir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
            if (entryName != null && xmlContent != null) {
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(xmlContent.getBytes());
                zos.closeEntry();
            }
        }
        return file.toFile();
    }

    @Test
    void extractsMetadataWithMultipleAuthors() throws Exception {
        String xml = "<ComicInfo>" +
                "<Title>Sample</Title>" +
                "<Summary>Summary text</Summary>" +
                "<Publisher>Marvel</Publisher>" +
                "<Series>Series A</Series>" +
                "<Number>5</Number>" +
                "<Count>10</Count>" +
                "<Year>2020</Year><Month>6</Month><Day>15</Day>" +
                "<PageCount>25</PageCount>" +
                "<LanguageISO>en</LanguageISO>" +
                "<Writer>Writer1, Writer2; Writer3</Writer>" +
                "<Penciller>Penciller1</Penciller>" +
                "<Genre>Action;Adventure</Genre>" +
                "<Tags>Sci-Fi</Tags>" +
                "</ComicInfo>";
        File cbz = createCbz("multipleAuthors.cbz", "ComicInfo.xml", xml);

        BookMetadata md = extractor.extractMetadata(cbz);

        assertThat(md.getTitle()).isEqualTo("Sample");
        assertThat(md.getDescription()).isEqualTo("Summary text");
        assertThat(md.getPublisher()).isEqualTo("Marvel");
        assertThat(md.getSeriesName()).isEqualTo("Series A");
        assertThat(md.getSeriesNumber()).isEqualTo(5f);
        assertThat(md.getSeriesTotal()).isEqualTo(10);
        assertThat(md.getPublishedDate()).isEqualTo(java.time.LocalDate.of(2020, 6, 15));
        assertThat(md.getPageCount()).isEqualTo(25);
        assertThat(md.getLanguage()).isEqualTo("en");
        assertThat(md.getAuthors()).containsExactlyInAnyOrder("Writer1", "Writer2", "Writer3", "Penciller1");
        assertThat(md.getCategories()).containsExactlyInAnyOrder("Action", "Adventure", "Sci-Fi");
    }

    @Test
    void handlesMissingFields() throws Exception {
        String xml = "<ComicInfo><Title>OnlyTitle</Title></ComicInfo>";
        File cbz = createCbz("missing.cbz", "ComicInfo.xml", xml);

        BookMetadata md = extractor.extractMetadata(cbz);

        assertThat(md.getTitle()).isEqualTo("OnlyTitle");
        assertThat(md.getPublisher()).isNull();
        assertThat(md.getAuthors()).isNull();
    }

    @Test
    void findsEntryCaseInsensitively() throws Exception {
        String xml = "<ComicInfo><Title>CaseTest</Title></ComicInfo>";
        File cbz = createCbz("CaseFile.CBZ", "comicinfo.xml", xml);

        BookMetadata md = extractor.extractMetadata(cbz);

        assertThat(md.getTitle()).isEqualTo("CaseTest");
    }

    @Test
    void fallsBackToFilenameWhenComicInfoMissing() throws Exception {
        File cbz = createCbz("nofile.cbz", "Other.xml", "<root/>");

        BookMetadata md = extractor.extractMetadata(cbz);

        assertThat(md.getTitle()).isEqualTo("nofile");
    }
}
