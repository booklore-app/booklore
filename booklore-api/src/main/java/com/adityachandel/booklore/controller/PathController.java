package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.service.file.PathService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/path")
@AllArgsConstructor
public class PathController {

    private final PathService pathService;

    @GetMapping
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public List<String> getFolders(@RequestParam String path) {
        return pathService.getFoldersAtPath(path);
    }
}
