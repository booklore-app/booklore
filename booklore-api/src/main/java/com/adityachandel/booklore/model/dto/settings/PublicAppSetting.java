package com.adityachandel.booklore.model.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicAppSetting {
    private final String key;
    private final Object value;
}