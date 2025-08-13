package com.adityachandel.booklore.util;
import com.adityachandel.booklore.model.dto.Book;

public class BookUtils {

    public static String cleanFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        fileName = fileName.replace("(Z-Library)", "").trim();
        fileName = fileName.replaceAll("\\s?\\(.*?\\)", "").trim(); // Remove the author name inside parentheses (e.g. (Jon Yablonski))
        int dotIndex = fileName.lastIndexOf('.'); // Remove the file extension (e.g., .pdf, .docx)
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex).trim();
        }
        return fileName;
    }

    public static String cleanAndTruncateSearchTerm(String term) {
        term = term.replaceAll("[^\\p{Alnum}# ]", "").trim();
        if (term.length() > 60) {
            String[] words = term.split("\\s+");
            StringBuilder truncated = new StringBuilder();
            for (String word : words) {
                if (truncated.length() + word.length() + 1 > 60) break;
                if (!truncated.isEmpty()) truncated.append(" ");
                truncated.append(word);
            }
            term = truncated.toString();
        }
        return term;
    }

    public static String buildComicSearchTerm(Book book) {
        if (book == null || book.getMetadata() == null) {
            return null;
        }
        String seriesName = book.getMetadata().getSeriesName();
        Float seriesNumber = book.getMetadata().getSeriesNumber();
        if (seriesName == null || seriesName.isBlank() || seriesNumber == null) {
            return null;
        }
        int intValue = seriesNumber.intValue();
        String issue = seriesNumber.equals((float) intValue)
                ? String.valueOf(intValue)
                : seriesNumber.toString();
        return seriesName + " #" + issue;
    }
}
