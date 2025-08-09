package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.Library;
import com.adityachandel.booklore.model.entity.LibraryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LibraryMapper {

    @Mapping(target = "paths", source = "libraryPaths")
    Library toLibrary(LibraryEntity libraryEntity);
}
