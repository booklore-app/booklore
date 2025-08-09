package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.KoreaderUser;
import com.adityachandel.booklore.model.entity.KoreaderUserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KoreaderUserMapper {

    KoreaderUserMapper INSTANCE = Mappers.getMapper(KoreaderUserMapper.class);

    KoreaderUser toDto(KoreaderUserEntity entity);

    KoreaderUserEntity toEntity(KoreaderUser dto);

    List<KoreaderUser> toDtoList(List<KoreaderUserEntity> entities);
}
