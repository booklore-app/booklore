package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.BookNote;
import com.adityachandel.booklore.model.entity.BookNoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookNoteMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "bookId", source = "book.id")
    BookNote toDto(BookNoteEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BookNoteEntity toEntity(BookNote dto);
}

