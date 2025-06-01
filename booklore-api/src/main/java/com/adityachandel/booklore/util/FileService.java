package com.adityachandel.booklore.util;

import com.adityachandel.booklore.config.AppProperties;
import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.entity.BookEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

    private final AppProperties appProperties;

    public void createThumbnailFromFile(long bookId, MultipartFile file) {
        try {
            validateCoverFile(file);
            String outputFolder = getThumbnailPath(bookId);
            File folder = new File(outputFolder);
            if (!folder.exists() && !folder.mkdirs()) {
                throw ApiError.DIRECTORY_CREATION_FAILED.createException(folder.getAbsolutePath());
            }
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw ApiError.IMAGE_NOT_FOUND.createException();
            }
            BufferedImage resizedImage = resizeImage(originalImage);
            File outputFile = new File(folder, "f.jpg");
            ImageIO.write(resizedImage, "JPEG", outputFile);
            log.info("Thumbnail created and saved at: {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("An error occurred while creating the thumbnail: {}", e.getMessage(), e);
            throw ApiError.FILE_READ_ERROR.createException(e.getMessage());
        }
    }

    private void validateCoverFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (!("image/jpeg".equalsIgnoreCase(contentType) || "image/png".equalsIgnoreCase(contentType))) {
            throw new IllegalArgumentException("Only JPEG and PNG files are allowed");
        }
        long maxFileSize = 5 * 1024 * 1024;
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size must not exceed 5 MB");
        }
    }

    public Resource getBookCover(String thumbnailPath) {
        Path thumbPath;
        if (thumbnailPath == null || thumbnailPath.isEmpty()) {
            thumbPath = Paths.get(getMissingThumbnailPath());
        } else {
            thumbPath = Paths.get(thumbnailPath);
        }
        try {
            Resource resource = new UrlResource(thumbPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw ApiError.IMAGE_NOT_FOUND.createException(thumbPath);
            }
        } catch (IOException e) {
            throw ApiError.IMAGE_NOT_FOUND.createException(thumbPath);
        }
    }

    public String createThumbnail(long bookId, String thumbnailUrl) throws IOException {
        String newFilename = "f.jpg";
        resizeAndSaveImage(thumbnailUrl, new File(getThumbnailPath(bookId)), newFilename);
        return getThumbnailPath(bookId) + newFilename;
    }

    private void resizeAndSaveImage(String imageUrl, File outputFolder, String outputFileName) throws IOException {
        BufferedImage originalImage;
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            originalImage = ImageIO.read(inputStream);
        }
        if (originalImage == null) {
            throw new IOException("Failed to read image from URL: " + imageUrl);
        }
        BufferedImage resizedImage = resizeImage(originalImage);
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputFolder.getAbsolutePath());
        }
        File outputFile = new File(outputFolder, outputFileName);
        ImageIO.write(resizedImage, "JPEG", outputFile);
        log.info("Image saved to: {}", outputFile.getAbsolutePath());
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        BufferedImage resizedImage = new BufferedImage(250, 350, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, 250, 350, null);
        g2d.dispose();
        return resizedImage;
    }

    public String getThumbnailPath(long bookId) {
        return appProperties.getPathConfig() + "/thumbs/" + bookId + "/";
    }

    public String getCbxCachePath() {
        return appProperties.getPathConfig() + "/cbx_cache";
    }

    public String getPdfCachePath() {
        return appProperties.getPathConfig() + "/pdf_cache";
    }

    public String getMissingThumbnailPath() {
        return appProperties.getPathConfig() + "/thumbs/missing/m.jpg";
    }

}