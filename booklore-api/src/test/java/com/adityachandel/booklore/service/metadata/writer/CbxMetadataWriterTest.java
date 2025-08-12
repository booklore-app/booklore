package com.adityachandel.booklore.service.metadata.writer;

import com.adityachandel.booklore.model.MetadataClearFlags;
import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.entity.CategoryEntity;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CbxMetadataWriterTest {

    private final CbxMetadataWriter writer = new CbxMetadataWriter();

    private File createCbz(String name, String entryName, String xml) throws Exception {
        Path dir = Files.createTempDirectory("cbz");
        Path path = dir.resolve(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(path))) {
            if (entryName != null && xml != null) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(xml.getBytes());
                zos.closeEntry();
            }
        }
        return path.toFile();
    }

    private Document readComicInfo(File cbz) throws Exception {
        try (ZipFile zip = new ZipFile(cbz)) {
            ZipEntry entry = zip.stream()
                    .filter(e -> e.getName().equalsIgnoreCase("ComicInfo.xml"))
                    .findFirst().orElseThrow();
            try (InputStream is = zip.getInputStream(entry)) {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            }
        }
    }

    @Test
    void writesMetadataIntoNewComicInfo() throws Exception {
        File cbz = createCbz("new.cbz", null, null);
        BookMetadataEntity md = BookMetadataEntity.builder()
                .title("Sample")
                .description("Desc")
                .publisher("Pub")
                .seriesName("Series")
                .seriesNumber(1f)
                .seriesTotal(5)
                .publishedDate(LocalDate.of(2022,3,4))
                .pageCount(30)
                .language("en")
                .authors(Set.of(AuthorEntity.builder().name("A1").build(), AuthorEntity.builder().name("A2").build()))
                .categories(Set.of(CategoryEntity.builder().name("Action").build(), CategoryEntity.builder().name("Sci-Fi").build()))
                .build();
        writer.writeMetadataToFile(cbz, md, null, false, new MetadataClearFlags());

        Document doc = readComicInfo(cbz);
        Element root = doc.getDocumentElement();
        assertThat(root.getElementsByTagName("Title").item(0).getTextContent()).isEqualTo("Sample");
        assertThat(root.getElementsByTagName("Summary").item(0).getTextContent()).isEqualTo("Desc");
        assertThat(root.getElementsByTagName("Publisher").item(0).getTextContent()).isEqualTo("Pub");
        assertThat(root.getElementsByTagName("Series").item(0).getTextContent()).isEqualTo("Series");
        assertThat(root.getElementsByTagName("Number").item(0).getTextContent()).isEqualTo("1");
        assertThat(root.getElementsByTagName("Count").item(0).getTextContent()).isEqualTo("5");
        assertThat(root.getElementsByTagName("Year").item(0).getTextContent()).isEqualTo("2022");
        assertThat(root.getElementsByTagName("Month").item(0).getTextContent()).isEqualTo("3");
        assertThat(root.getElementsByTagName("Day").item(0).getTextContent()).isEqualTo("4");
        assertThat(root.getElementsByTagName("PageCount").item(0).getTextContent()).isEqualTo("30");
        assertThat(root.getElementsByTagName("LanguageISO").item(0).getTextContent()).isEqualTo("en");
        assertThat(root.getElementsByTagName("Writer").item(0).getTextContent()).contains("A1").contains("A2");
        assertThat(root.getElementsByTagName("Genre").item(0).getTextContent()).contains("Action").contains("Sci-Fi");
    }

    @Test
    void updatesExistingComicInfoCaseInsensitive() throws Exception {
        String xml = "<ComicInfo><Title>Old</Title></ComicInfo>";
        File cbz = createCbz("existing.cbz", "comicinfo.xml", xml);
        BookMetadataEntity md = BookMetadataEntity.builder().title("New").build();
        writer.writeMetadataToFile(cbz, md, null, false, new MetadataClearFlags());

        try (ZipFile zip = new ZipFile(cbz)) {
            long count = zip.stream().filter(e -> e.getName().equalsIgnoreCase("ComicInfo.xml")).count();
            assertThat(count).isEqualTo(1);
        }
        Document doc = readComicInfo(cbz);
        assertThat(doc.getElementsByTagName("Title").item(0).getTextContent()).isEqualTo("New");
    }
}
