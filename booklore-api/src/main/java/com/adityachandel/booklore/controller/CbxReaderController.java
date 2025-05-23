package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.service.CbxReaderService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cbx")
@RequiredArgsConstructor
public class CbxReaderController {

    private final CbxReaderService cbxReaderService;

    @GetMapping("/{bookId}/pages")
    public List<Integer> listPages(@PathVariable Long bookId) {
        return cbxReaderService.getAvailablePages(bookId);
    }

    @GetMapping("/{bookId}/pages/{pageNumber}")
    public void getPage(@PathVariable Long bookId, @PathVariable int pageNumber, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        cbxReaderService.streamPageImage(bookId, pageNumber, response.getOutputStream());
    }
}