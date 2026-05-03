package com.sgarsgaya.codeatlas.unit.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.dto.entity.SnapshotJpaEntity;
import com.sgarsgaya.codeatlas.mappers.SnapshotMapper;
import com.sgarsgaya.codeatlas.repositories.SnapshotJpaRepository;
import com.sgarsgaya.codeatlas.repositories.SnapshotRepositoryJpaImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotRepositoryJpaImplTest {

    @Mock SnapshotJpaRepository jpaRepository;
    @Mock SnapshotMapper snapshotMapper;

    private SnapshotRepositoryJpaImpl repository;

    private static final Instant NOW = Instant.parse("2026-05-03T00:00:00Z");
    private static final SnapshotJpaEntity ENTITY = new SnapshotJpaEntity("S1", NOW, true);
    private static final SnapshotResponseDto DTO = new SnapshotResponseDto(1L, "S1", "2026-05-03T00:00:00Z");

    @BeforeEach
    void setUp() {
        repository = new SnapshotRepositoryJpaImpl(jpaRepository, snapshotMapper);
    }

    @Test
    void save_deactivatesAll_thenSavesActiveEntity_andReturnsMappedDto() {
        when(jpaRepository.save(any())).thenReturn(ENTITY);
        when(snapshotMapper.toDto(ENTITY)).thenReturn(DTO);

        var result = repository.save("S1", NOW);

        verify(jpaRepository).deactivateAll();
        verify(jpaRepository).save(any(SnapshotJpaEntity.class));
        assertThat(result.snapshotName()).isEqualTo("S1");
    }

    @Test
    void findAll_returnsMappedDtos() {
        when(jpaRepository.findAll()).thenReturn(List.of(ENTITY));
        when(snapshotMapper.toDto(ENTITY)).thenReturn(DTO);

        assertThat(repository.findAll()).containsExactly(DTO);
    }

    @Test
    void findActive_returnsEmpty_whenNoActiveSnapshot() {
        when(jpaRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThat(repository.findActive()).isEmpty();
    }

    @Test
    void findActive_returnsMappedDto_whenActiveExists() {
        when(jpaRepository.findByActiveTrue()).thenReturn(Optional.of(ENTITY));
        when(snapshotMapper.toDto(ENTITY)).thenReturn(DTO);

        assertThat(repository.findActive()).contains(DTO);
    }

    @Test
    void findByName_returnsEmpty_whenNotFound() {
        when(jpaRepository.findBySnapshotName("ghost")).thenReturn(Optional.empty());

        assertThat(repository.findByName("ghost")).isEmpty();
    }

    @Test
    void findByName_returnsMappedDto_whenFound() {
        when(jpaRepository.findBySnapshotName("S1")).thenReturn(Optional.of(ENTITY));
        when(snapshotMapper.toDto(ENTITY)).thenReturn(DTO);

        assertThat(repository.findByName("S1")).contains(DTO);
    }

    @Test
    void deleteByName_delegatesToJpaRepository() {
        repository.deleteByName("S1");

        verify(jpaRepository).deleteBySnapshotName("S1");
    }
}
