package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.EmailProviderUser;
import com.adityachandel.booklore.model.entity.EmailProviderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailProviderUserMapper {

    EmailProviderUserMapper INSTANCE = Mappers.getMapper(EmailProviderUserMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "fromAddress", source = "fromAddress")
    @Mapping(target = "name", source = "name")
    EmailProviderUser toUserDto(EmailProviderEntity entity);

    List<EmailProviderUser> toUserDtoList(List<EmailProviderEntity> entities);
}
