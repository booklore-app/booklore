package com.adityachandel.booklore.service.email;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.EmailProviderMapper;
import com.adityachandel.booklore.model.dto.EmailProvider;
import com.adityachandel.booklore.model.dto.request.CreateEmailProviderRequest;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.EmailProviderEntity;
import com.adityachandel.booklore.model.entity.UserEmailProviderEntity;
import com.adityachandel.booklore.model.entity.UserEmailProviderId;
import com.adityachandel.booklore.repository.EmailProviderRepository;
import com.adityachandel.booklore.repository.UserEmailProviderRepository;
import com.adityachandel.booklore.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class EmailProviderService {

    private final EmailProviderRepository emailProviderRepository;
    private final UserEmailProviderRepository userEmailProviderRepository;
    private final EmailProviderMapper emailProviderMapper;
    private final UserRepository userRepository;

    public EmailProvider getEmailProvider(Long id) {
        EmailProviderEntity emailProvider = emailProviderRepository.findById(id).orElseThrow(() -> ApiError.EMAIL_PROVIDER_NOT_FOUND.createException(id));
        return emailProviderMapper.toDTO(emailProvider);
    }

    public EmailProvider createEmailProvider(CreateEmailProviderRequest request) {
        boolean isFirstProvider = emailProviderRepository.count() == 0;
        EmailProviderEntity emailProviderEntity = emailProviderMapper.toEntity(request);
        emailProviderEntity.setDefaultProvider(isFirstProvider);
        EmailProviderEntity savedEntity = emailProviderRepository.save(emailProviderEntity);
        return emailProviderMapper.toDTO(savedEntity);
    }

    public EmailProvider updateEmailProvider(Long id, CreateEmailProviderRequest request) {
        EmailProviderEntity existingProvider = emailProviderRepository.findById(id).orElseThrow(() -> ApiError.EMAIL_PROVIDER_NOT_FOUND.createException(id));
        emailProviderMapper.updateEntityFromRequest(request, existingProvider);
        EmailProviderEntity updatedEntity = emailProviderRepository.save(existingProvider);
        return emailProviderMapper.toDTO(updatedEntity);
    }

    @Transactional
    public void setDefaultEmailProvider(Long id) {
        EmailProviderEntity emailProvider = emailProviderRepository.findById(id).orElseThrow(() -> ApiError.EMAIL_PROVIDER_NOT_FOUND.createException(id));
        emailProviderRepository.updateAllProvidersToNonDefault();
        emailProvider.setDefaultProvider(true);
        emailProviderRepository.save(emailProvider);
    }

    @Transactional
    public void deleteEmailProvider(Long id) {
        EmailProviderEntity emailProviderToDelete = emailProviderRepository.findById(id).orElseThrow(() -> ApiError.EMAIL_PROVIDER_NOT_FOUND.createException(id));
        boolean isDefaultProvider = emailProviderToDelete.isDefaultProvider();
        if (isDefaultProvider) {
            List<EmailProviderEntity> allProviders = emailProviderRepository.findAll();
            if (allProviders.size() > 1) {
                allProviders.remove(emailProviderToDelete);
                EmailProviderEntity newDefaultProvider = allProviders.get(ThreadLocalRandom.current().nextInt(allProviders.size()));
                newDefaultProvider.setDefaultProvider(true);
                emailProviderRepository.save(newDefaultProvider);
            }
        }
        emailProviderRepository.deleteById(id);
    }

    public List<EmailProvider> getEmailProviders() {
        return emailProviderRepository.findAll().stream().map(emailProviderMapper::toDTO).toList();
    }

    @Transactional
    public void assignProvidersToUser(Long userId, List<Long> providerIds) {
        BookLoreUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiError.USER_NOT_FOUND.createException(userId));

        userEmailProviderRepository.deleteByUserId(userId);

        List<UserEmailProviderEntity> newMappings = providerIds.stream()
                .map(providerId -> {
                    EmailProviderEntity provider = emailProviderRepository.findById(providerId)
                            .orElseThrow(() -> ApiError.EMAIL_PROVIDER_NOT_FOUND.createException(providerId));

                    return UserEmailProviderEntity.builder()
                            .user(user)
                            .provider(provider)
                            .build();
                })
                .collect(Collectors.toList());

        userEmailProviderRepository.saveAll(newMappings);
    }
}