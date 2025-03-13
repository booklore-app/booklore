package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.EmailProvider;
import com.adityachandel.booklore.model.dto.request.CreateEmailProviderRequest;
import com.adityachandel.booklore.model.entity.EmailProviderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmailProviderMapper {

    EmailProvider toDTO(EmailProviderEntity emailProviderEntity);
    EmailProviderEntity toEntity(EmailProvider emailProvider);
    EmailProviderEntity toEntity(CreateEmailProviderRequest createRequest);
    void updateEntityFromRequest(CreateEmailProviderRequest request, @MappingTarget EmailProviderEntity entity);
}
