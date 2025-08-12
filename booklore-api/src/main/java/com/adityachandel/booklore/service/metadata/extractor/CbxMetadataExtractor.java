package com.adityachandel.booklore.service.metadata.extractor;

import com.adityachandel.booklore.model.dto.BookMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
public class CbxMetadataExtractor implements FileMetadataExtractor {

    @Override
    public BookMetadata extractMetadata(File file) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        if (!file.getName().toLowerCase().endsWith(".cbz")) {
            return BookMetadata.builder().title(baseName).build();
        }

        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry entry = findComicInfoEntry(zipFile);
            if (entry == null) {
                return BookMetadata.builder().title(baseName).build();
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                Document document = buildSecureDocument(is);
                return mapDocumentToMetadata(document, baseName);
            }
        } catch (Exception e) {
            log.warn("Failed to extract metadata from CBZ", e);
            return BookMetadata.builder().title(baseName).build();
        }
    }

    private ZipEntry findComicInfoEntry(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if ("comicinfo.xml".equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    private Document buildSecureDocument(InputStream is) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(is);
    }

    private BookMetadata mapDocumentToMetadata(Document document, String fallbackTitle) {
        BookMetadata.BookMetadataBuilder builder = BookMetadata.builder();

        String title = getTextContent(document, "Title");
        builder.title(title == null || title.isBlank() ? fallbackTitle : title);

        builder.description(coalesce(getTextContent(document, "Summary"), getTextContent(document, "Description")));
        builder.publisher(getTextContent(document, "Publisher"));
        builder.seriesName(getTextContent(document, "Series"));
        builder.seriesNumber(parseFloat(getTextContent(document, "Number")));
        builder.seriesTotal(parseInteger(getTextContent(document, "Count")));
        builder.publishedDate(parseDate(getTextContent(document, "Year"),
                getTextContent(document, "Month"), getTextContent(document, "Day")));
        builder.pageCount(parseInteger(coalesce(getTextContent(document, "PageCount"),
                getTextContent(document, "Pages"))));
        builder.language(getTextContent(document, "LanguageISO"));

        Set<String> authors = new HashSet<>();
        authors.addAll(splitValues(getTextContent(document, "Writer")));
        authors.addAll(splitValues(getTextContent(document, "Penciller")));
        authors.addAll(splitValues(getTextContent(document, "Inker")));
        authors.addAll(splitValues(getTextContent(document, "Colorist")));
        authors.addAll(splitValues(getTextContent(document, "Letterer")));
        authors.addAll(splitValues(getTextContent(document, "CoverArtist")));
        if (!authors.isEmpty()) {
            builder.authors(authors);
        }

        Set<String> categories = new HashSet<>();
        categories.addAll(splitValues(getTextContent(document, "Genre")));
        categories.addAll(splitValues(getTextContent(document, "Tags")));
        if (!categories.isEmpty()) {
            builder.categories(categories);
        }

        return builder.build();
    }

    private String getTextContent(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null && !b.isBlank() ? b : null);
    }

    private Set<String> splitValues(String value) {
        if (value == null) {
            return new HashSet<>();
        }
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(HashSet::new, Set::add, Set::addAll);
    }

    private Integer parseInteger(String value) {
        try {
            return (value == null || value.isBlank()) ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float parseFloat(String value) {
        try {
            return (value == null || value.isBlank()) ? null : Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String year, String month, String day) {
        Integer y = parseInteger(year);
        Integer m = parseInteger(month);
        Integer d = parseInteger(day);
        if (y == null) {
            return null;
        }
        if (m == null) {
            m = 1;
        }
        if (d == null) {
            d = 1;
        }
        try {
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte[] extractCover(File file) {
        return generatePlaceholderCover(250, 350);
    }

    private byte[] generatePlaceholderCover(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, width / 10));
        FontMetrics fm = g.getFontMetrics();
        String text = "Preview Unavailable";

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        g.drawString(text, (width - textWidth) / 2, (height + textHeight) / 2);

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.warn("Failed to generate placeholder image", e);
            return null;
        }
    }
}