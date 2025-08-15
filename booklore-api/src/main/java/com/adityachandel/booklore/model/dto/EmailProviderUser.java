package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailProviderUser {
    private Long id;
    private String fromAddress;
    private String name;
}
