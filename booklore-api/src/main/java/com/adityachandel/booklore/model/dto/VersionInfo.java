package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VersionInfo {
    private String current;
    private String latest;
}
