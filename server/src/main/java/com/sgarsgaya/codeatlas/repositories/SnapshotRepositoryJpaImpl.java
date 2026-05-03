package com.sgarsgaya.codeatlas.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.dto.entity.SnapshotJpaEntity;
import com.sgarsgaya.codeatlas.mappers.SnapshotMapper;

@Repository
public class SnapshotRepositoryJpaImpl implements SnapshotRepository {

    private final SnapshotJpaRepository jpaRepository;
    private final SnapshotMapper snapshotMapper;

    public SnapshotRepositoryJpaImpl(SnapshotJpaRepository jpaRepository, SnapshotMapper snapshotMapper) {
        this.jpaRepository = jpaRepository;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    @Transactional
    public SnapshotResponseDto save(String snapshotName, Instant createdAt) {
        jpaRepository.deactivateAll();
        return snapshotMapper.toDto(jpaRepository.save(new SnapshotJpaEntity(snapshotName, createdAt, true)));
    }

    @Override
    public List<SnapshotResponseDto> findAll() {
        return jpaRepository.findAll().stream().map(snapshotMapper::toDto).toList();
    }

    @Override
    public Optional<SnapshotResponseDto> findActive() {
        return jpaRepository.findByActiveTrue().map(snapshotMapper::toDto);
    }

    @Override
    public Optional<SnapshotResponseDto> findByName(String snapshotName) {
        return jpaRepository.findBySnapshotName(snapshotName).map(snapshotMapper::toDto);
    }

    @Override
    @Transactional
    public void deleteByName(String snapshotName) {
        jpaRepository.deleteBySnapshotName(snapshotName);
    }
}
