package com.adityachandel.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "koreader_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KoreaderUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booklore_user_id")
    private BookLoreUserEntity bookLoreUser;

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
