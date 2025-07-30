package com.adityachandel.booklore.service.metadata.extractor;

import com.adityachandel.booklore.model.dto.BookMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class CbxMetadataExtractor implements FileMetadataExtractor {

    @Override
    public BookMetadata extractMetadata(File file) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        return BookMetadata.builder()
                .title(baseName)
                .build();
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