package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.request.SendBookByEmailRequest;
import com.adityachandel.booklore.service.email.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailService emailService;

    @PreAuthorize("@securityUtil.canEmailBook() or @securityUtil.isAdmin()")
    @PostMapping("/send-book")
    public ResponseEntity<?> sendEmail(@Validated @RequestBody SendBookByEmailRequest request) {
        emailService.emailBook(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@securityUtil.canEmailBook() or @securityUtil.isAdmin()")
    @PostMapping("/send-book/{bookId}")
    public ResponseEntity<?> emailBookQuick(@PathVariable Long bookId) {
        emailService.emailBookQuick(bookId);
        return ResponseEntity.noContent().build();
    }
}