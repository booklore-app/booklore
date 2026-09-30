package org.booklore.service;

import org.booklore.service.book.PlaceholderEpubBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderEpubBuilderTest {

    private static final String DC = "http://purl.org/dc/elements/1.1/";

    @TempDir
    Path dir;

    private PlaceholderEpubBuilder.Spec spec(String title, List<String> authors, String isbn, byte[] cover) {
        return new PlaceholderEpubBuilder.Spec(title, authors, "A <b>description</b> & more", "Ace", "1990-05-01", "en", isbn, 412,
                List.of("Fantasy"), cover);
    }

    private ZipFile open(byte[] epub) throws Exception {
        Path file = Files.createTempFile(dir, "stub", ".epub");
        Files.write(file, epub);
        return new ZipFile(file.toFile());
    }

    private Document parseOpf(ZipFile zip) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream in = zip.getInputStream(zip.getEntry("OEBPS/content.opf"))) {
            return factory.newDocumentBuilder().parse(in);
        }
    }

    @Test
    void mimetypeIsFirstAndStored() throws Exception {
        try (ZipFile zip = open(PlaceholderEpubBuilder.build(spec("Dune", List.of("Frank Herbert"), null, null)))) {
            ZipEntry first = Collections.list(zip.entries()).getFirst();
            assertEquals("mimetype", first.getName());
            assertEquals(ZipEntry.STORED, first.getMethod());
            try (InputStream in = zip.getInputStream(first)) {
                assertEquals("application/epub+zip", new String(in.readAllBytes(), StandardCharsets.US_ASCII));
            }
            assertNotNull(zip.getEntry("META-INF/container.xml"));
            assertNotNull(zip.getEntry("OEBPS/nav.xhtml"));
            assertNotNull(zip.getEntry("OEBPS/page.xhtml"));
        }
    }

    @Test
    void metadataIsWrittenAndSpecialCharactersAreEscaped() throws Exception {
        try (ZipFile zip = open(PlaceholderEpubBuilder.build(spec("Tom & Jerry <Live>", List.of("A. Author", "B. Writer"), "978-0-441-17271-9", null)))) {
            Document opf = parseOpf(zip);
            assertEquals("Tom & Jerry <Live>", opf.getElementsByTagNameNS(DC, "title").item(0).getTextContent());
            assertEquals(2, opf.getElementsByTagNameNS(DC, "creator").getLength());
            assertEquals("A <b>description</b> & more", opf.getElementsByTagNameNS(DC, "description").item(0).getTextContent());
            assertEquals("Ace", opf.getElementsByTagNameNS(DC, "publisher").item(0).getTextContent());

            boolean hasIsbn = false;
            for (int i = 0; i < opf.getElementsByTagNameNS(DC, "identifier").getLength(); i++) {
                if ("urn:isbn:978-0-441-17271-9".equals(opf.getElementsByTagNameNS(DC, "identifier").item(i).getTextContent())) {
                    hasIsbn = true;
                }
            }
            assertTrue(hasIsbn);
        }
    }

    @Test
    void blankTitleFallsBackAndInvalidXmlCharactersAreRemoved() throws Exception {
        try (ZipFile zip = open(PlaceholderEpubBuilder.build(spec("  ", List.of("Bad\u0001Name"), null, null)))) {
            Document opf = parseOpf(zip);
            assertEquals("Untitled", opf.getElementsByTagNameNS(DC, "title").item(0).getTextContent());
            assertEquals("BadName", opf.getElementsByTagNameNS(DC, "creator").item(0).getTextContent());
        }
    }

    @Test
    void coverIsEmbeddedWhenProvided() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
        try (ZipFile zip = open(PlaceholderEpubBuilder.build(spec("Dune", List.of("Frank Herbert"), null, jpeg)))) {
            ZipEntry cover = zip.getEntry("OEBPS/cover.jpg");
            assertNotNull(cover);
            try (InputStream in = zip.getInputStream(cover)) {
                assertArrayEquals(jpeg, in.readAllBytes());
            }
            parseOpf(zip);
        }
    }

    @Test
    void everyStubHasAUniqueIdentifierSoHashesDiffer() throws Exception {
        byte[] first = PlaceholderEpubBuilder.build(spec("Dune", List.of("Frank Herbert"), null, null));
        byte[] second = PlaceholderEpubBuilder.build(spec("Dune", List.of("Frank Herbert"), null, null));
        assertFalse(java.util.Arrays.equals(first, second));
    }
}
