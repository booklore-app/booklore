package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.security.AuthenticationService;
import com.adityachandel.booklore.mapper.KoreaderUserMapper;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.dto.KoreaderUser;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.KoreaderUserEntity;
import com.adityachandel.booklore.repository.KoreaderUserRepository;
import com.adityachandel.booklore.repository.UserRepository;
import com.adityachandel.booklore.service.user.UserService;
import com.adityachandel.booklore.util.Md5Util;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KoreaderUserService {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final KoreaderUserRepository koreaderUserRepository;
    private final KoreaderUserMapper koreaderUserMapper;

    @Transactional
    public KoreaderUser createUser(String username, String rawPassword, String displayName) {

        Long id = authenticationService.getAuthenticatedUser().getId();
        Optional<BookLoreUserEntity> bookLoreUser = userRepository.findById(id);


        if (koreaderUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        String md5Password = Md5Util.md5Hex(rawPassword);
        KoreaderUserEntity user = new KoreaderUserEntity();
        user.setBookLoreUser(bookLoreUser.get());
        user.setUsername(username);
        user.setPassword(md5Password);
        user.setDisplayName(displayName);
        KoreaderUserEntity savedUser = koreaderUserRepository.save(user);
        return koreaderUserMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public KoreaderUser findUserDtoByUsername(String username) {
        KoreaderUserEntity user = koreaderUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return koreaderUserMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<KoreaderUser> findAllUsers() {
        List<KoreaderUserEntity> users = koreaderUserRepository.findAll();
        return koreaderUserMapper.toDtoList(users);
    }
}