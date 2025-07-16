package com.adityachandel.booklore.controller;


import com.adityachandel.booklore.model.dto.KoreaderUser;
import com.adityachandel.booklore.service.KoreaderUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/koreader-users")
@RequiredArgsConstructor
public class KoreaderUserController {

    private final KoreaderUserService koreaderUserService;


    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KoreaderUser> createUser(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String password = userData.get("password");
        String displayName = userData.getOrDefault("displayName", null);

        log.info("Admin creating KOReader user: {}", username);

        KoreaderUser createdUser = koreaderUserService.createUser(username, password, displayName);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KoreaderUser> getUser(@PathVariable String username) {
        KoreaderUser userDto = koreaderUserService.findUserDtoByUsername(username);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KoreaderUser>> getAllUsers() {
        List<KoreaderUser> users = koreaderUserService.findAllUsers();
        return ResponseEntity.ok(users);
    }
}
   
