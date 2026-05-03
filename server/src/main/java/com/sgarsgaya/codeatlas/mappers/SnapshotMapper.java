package com.sgarsgaya.codeatlas.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.dto.entity.SnapshotJpaEntity;

/**
 * MapStruct mapper for the snapshot persistence boundary.
 * Converts {@link SnapshotJpaEntity} (JPA type) into {@link SnapshotResponseDto}
 * so no JPA type escapes through the {@code SnapshotRepository} seam.
 */
@Mapper(componentModel = "spring")
public interface SnapshotMapper {

    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toString())")
    SnapshotResponseDto toDto(SnapshotJpaEntity entity);
}
