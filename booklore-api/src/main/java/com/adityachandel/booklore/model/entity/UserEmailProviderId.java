package com.adityachandel.booklore.model.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class UserEmailProviderId implements Serializable {
    private Long userId;
    private Long providerId;
}
