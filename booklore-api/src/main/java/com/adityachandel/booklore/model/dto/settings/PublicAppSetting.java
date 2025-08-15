package com.adityachandel.booklore.model.dto.settings;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicAppSetting {
    private boolean oidcEnabled;
    private OidcProviderDetails oidcProviderDetails;
}