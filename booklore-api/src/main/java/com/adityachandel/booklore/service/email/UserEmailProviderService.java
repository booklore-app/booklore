package com.adityachandel.booklore.service.email;

import com.adityachandel.booklore.config.security.AuthenticationService;
import com.adityachandel.booklore.mapper.EmailProviderUserMapper;
import com.adityachandel.booklore.model.dto.EmailProviderUser;
import com.adityachandel.booklore.model.entity.UserEmailProviderEntity;
import com.adityachandel.booklore.repository.UserEmailProviderRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class UserEmailProviderService {

    private final UserEmailProviderRepository userEmailProviderRepository;
    private final EmailProviderUserMapper emailProviderMapper;
    private final AuthenticationService authenticationService;

    public List<EmailProviderUser> getProvidersForUser() {
        Long userId = authenticationService.getAuthenticatedUser().getId();
        return userEmailProviderRepository.findByUserId(userId)
                .stream()
                .map(UserEmailProviderEntity::getProvider)
                .map(emailProviderMapper::toUserDto)
                .toList();
    }
}