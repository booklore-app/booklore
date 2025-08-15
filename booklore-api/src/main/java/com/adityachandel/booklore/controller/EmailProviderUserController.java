package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.EmailProviderUser;
import com.adityachandel.booklore.service.email.UserEmailProviderService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/email/provider-user")
public class EmailProviderUserController {

    private final UserEmailProviderService userEmailProviderService;

    @GetMapping
    public List<EmailProviderUser> getProvidersForCurrentUser() {
        return userEmailProviderService.getProvidersForUser();
    }
}