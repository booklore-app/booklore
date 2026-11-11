package org.booklore.service.book;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the small, valid EPUB that stands in for a physical book on disk.
 * <p>
 * It carries the book's metadata (and cover, when there is one) in standard EPUB fields, so Booklore's
 * normal metadata extraction reads back the same information, and it has a single page saying the book
 * has no digital content.
 */
public final class PlaceholderEpubBuilder {

    public record Spec(String title,
                       List<String> authors,
                       String description,
                       String publisher,
                       String publishedDate,
                       String language,
                       String isbn,
                       Integer pageCount,
                       List<String> subjects,
                       byte[] cover) {
    }

    private static final String MIMETYPE = "application/epub+zip";

    private static final String CONTAINER_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
            """;

    private PlaceholderEpubBuilder() {
    }

    public static byte[] build(Spec spec) throws IOException {
        String title = clean(spec.title());
        if (title.isBlank()) {
            title = "Untitled";
        }
        String language = clean(spec.language());
        if (language.isBlank()) {
            language = "en";
        }

        byte[] cover = spec.cover() != null && spec.cover().length > 0 ? spec.cover() : null;
        boolean png = cover != null && cover.length > 3 && (cover[0] & 0xFF) == 0x89 && cover[1] == 'P' && cover[2] == 'N' && cover[3] == 'G';
        String coverName = png ? "cover.png" : "cover.jpg";
        String coverType = png ? "image/png" : "image/jpeg";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            writeStored(zip, "mimetype", MIMETYPE.getBytes(StandardCharsets.US_ASCII));
            writeDeflated(zip, "META-INF/container.xml", CONTAINER_XML.getBytes(StandardCharsets.UTF_8));
            writeDeflated(zip, "OEBPS/content.opf", opf(spec, title, language, cover != null, coverName, coverType).getBytes(StandardCharsets.UTF_8));
            writeDeflated(zip, "OEBPS/nav.xhtml", nav(title, language).getBytes(StandardCharsets.UTF_8));
            writeDeflated(zip, "OEBPS/page.xhtml", page(spec, title, language).getBytes(StandardCharsets.UTF_8));
            if (cover != null) {
                writeDeflated(zip, "OEBPS/" + coverName, cover);
            }
        }
        return out.toByteArray();
    }

    private static String opf(Spec spec, String title, String language, boolean hasCover, String coverName, String coverType) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"bookid\">\n");
        sb.append("  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n");
        sb.append("    <dc:identifier id=\"bookid\">urn:uuid:").append(UUID.randomUUID()).append("</dc:identifier>\n");

        String isbn = clean(spec.isbn());
        if (!isbn.isBlank()) {
            sb.append("    <dc:identifier>urn:isbn:").append(esc(isbn)).append("</dc:identifier>\n");
        }
        sb.append("    <dc:title>").append(esc(title)).append("</dc:title>\n");
        for (String author : safeList(spec.authors())) {
            String name = clean(author);
            if (!name.isBlank()) {
                sb.append("    <dc:creator>").append(esc(name)).append("</dc:creator>\n");
            }
        }
        sb.append("    <dc:language>").append(esc(language)).append("</dc:language>\n");
        appendIfPresent(sb, "publisher", spec.publisher());
        appendIfPresent(sb, "date", spec.publishedDate());
        appendIfPresent(sb, "description", spec.description());
        for (String subject : safeList(spec.subjects())) {
            appendIfPresent(sb, "subject", subject);
        }
        if (spec.pageCount() != null && spec.pageCount() > 0) {
            sb.append("    <meta name=\"calibre:pages\" content=\"").append(spec.pageCount()).append("\"/>\n");
        }
        if (hasCover) {
            sb.append("    <meta name=\"cover\" content=\"cover-image\"/>\n");
        }
        sb.append("    <meta property=\"dcterms:modified\">").append(Instant.now().truncatedTo(ChronoUnit.SECONDS)).append("</meta>\n");
        sb.append("  </metadata>\n");
        sb.append("  <manifest>\n");
        sb.append("    <item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
        sb.append("    <item id=\"page\" href=\"page.xhtml\" media-type=\"application/xhtml+xml\"/>\n");
        if (hasCover) {
            sb.append("    <item id=\"cover-image\" href=\"").append(coverName).append("\" media-type=\"").append(coverType).append("\" properties=\"cover-image\"/>\n");
        }
        sb.append("  </manifest>\n");
        sb.append("  <spine>\n");
        sb.append("    <itemref idref=\"page\"/>\n");
        sb.append("  </spine>\n");
        sb.append("</package>\n");
        return sb.toString();
    }

    private static String nav(String title, String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" xml:lang=\"" + esc(language) + "\">\n"
                + "<head><meta charset=\"utf-8\"/><title>" + esc(title) + "</title></head>\n"
                + "<body>\n"
                + "<nav epub:type=\"toc\"><ol><li><a href=\"page.xhtml\">" + esc(title) + "</a></li></ol></nav>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private static String page(Spec spec, String title, String language) {
        StringBuilder authors = new StringBuilder();
        for (String author : safeList(spec.authors())) {
            String name = clean(author);
            if (!name.isBlank()) {
                if (authors.length() > 0) {
                    authors.append(", ");
                }
                authors.append(name);
            }
        }
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"").append(esc(language)).append("\">\n");
        sb.append("<head><meta charset=\"utf-8\"/><title>").append(esc(title)).append("</title></head>\n");
        sb.append("<body>\n");
        sb.append("<h1>").append(esc(title)).append("</h1>\n");
        if (authors.length() > 0) {
            sb.append("<p>").append(esc(authors.toString())).append("</p>\n");
        }
        sb.append("<p>This file is a placeholder for a physical book. It has no digital content.</p>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String element, String value) {
        String text = clean(value);
        if (!text.isBlank()) {
            sb.append("    <dc:").append(element).append(">").append(esc(text)).append("</dc:").append(element).append(">\n");
        }
    }

    private static List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    /** The EPUB spec requires the mimetype entry to be first and stored without compression. */
    private static void writeStored(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private static void writeDeflated(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    /** Drops characters that are not allowed in XML 1.0 documents and trims the result. */
    static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        value.codePoints()
                .filter(cp -> cp == 0x9 || cp == 0xA || cp == 0xD
                        || (cp >= 0x20 && cp <= 0xD7FF)
                        || (cp >= 0xE000 && cp <= 0xFFFD)
                        || (cp >= 0x10000 && cp <= 0x10FFFF))
                .forEach(sb::appendCodePoint);
        return sb.toString().trim();
    }

    static String esc(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
