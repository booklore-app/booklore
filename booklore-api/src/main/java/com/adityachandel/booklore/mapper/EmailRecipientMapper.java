package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.EmailRecipient;
import com.adityachandel.booklore.model.dto.request.CreateEmailRecipientRequest;
import com.adityachandel.booklore.model.entity.EmailRecipientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmailRecipientMapper {

    EmailRecipient toDTO(EmailRecipientEntity emailRecipientEntity);

    EmailRecipientEntity toEntity(EmailRecipient emailRecipient);

    EmailRecipientEntity toEntity(CreateEmailRecipientRequest createRequest);

    void updateEntityFromRequest(CreateEmailRecipientRequest request, @MappingTarget EmailRecipientEntity entity);
}