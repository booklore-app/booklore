package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.LibraryPath;
import com.adityachandel.booklore.model.entity.LibraryPathEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LibraryPathMapper {

    LibraryPath toLibraryPath(LibraryPathEntity libraryPathEntity);
}
