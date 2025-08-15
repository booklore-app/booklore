package com.adityachandel.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_email_provider")
public class UserEmailProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private BookLoreUserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private EmailProviderEntity provider;
}
