package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.MagicShelf;
import com.adityachandel.booklore.service.MagicShelfService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/magic-shelves")
public class MagicShelfController {

    private final MagicShelfService magicShelfService;

    public MagicShelfController(MagicShelfService magicShelfService) {
        this.magicShelfService = magicShelfService;
    }

    @GetMapping
    public ResponseEntity<List<MagicShelf>> getAllForUser() {
        return ResponseEntity.ok(magicShelfService.getUserShelves());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MagicShelf> getShelf(@PathVariable Long id) {
        return ResponseEntity.ok(magicShelfService.getShelf(id));
    }

    @PostMapping
    public ResponseEntity<MagicShelf> createUpdateShelf(@Valid @RequestBody MagicShelf shelf) {
        return ResponseEntity.ok(magicShelfService.createOrUpdateShelf(shelf));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelf(@PathVariable Long id) {
        magicShelfService.deleteShelf(id);
        return ResponseEntity.noContent().build();
    }
}