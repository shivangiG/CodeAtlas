package com.sgarsgaya.codeatlas.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sgarsgaya.codeatlas.dto.GraphStatusResponseDto;
import com.sgarsgaya.codeatlas.dto.SnapshotRequestDto;
import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.exception.ActiveSnapshotDeletionException;
import com.sgarsgaya.codeatlas.exception.DuplicateSnapshotException;
import com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException;
import com.sgarsgaya.codeatlas.repositories.SnapshotRepository;
import com.sgarsgaya.codeatlas.service.SnapshotService;
import com.sgarsgaya.codeatlas.util.TimeProvider;

@Service
public class SnapshotServiceImpl implements SnapshotService {

    private final SnapshotRepository snapshotRepository;
    private final TimeProvider timeProvider;

    public SnapshotServiceImpl(SnapshotRepository snapshotRepository, TimeProvider timeProvider) {
        this.snapshotRepository = snapshotRepository;
        this.timeProvider = timeProvider;
    }

    @Override
    public SnapshotResponseDto createSnapshot(SnapshotRequestDto request) {
        if (snapshotRepository.findByName(request.snapshotName()).isPresent()) {
            throw new DuplicateSnapshotException(request.snapshotName());
        }
        return snapshotRepository.save(request.snapshotName(), timeProvider.now());
    }

    @Override
    public List<SnapshotResponseDto> listSnapshots() {
        return snapshotRepository.findAll();
    }

    @Override
    public Optional<GraphStatusResponseDto> getActiveSnapshot() {
        return snapshotRepository.findActive().map(dto ->
                new GraphStatusResponseDto(dto.snapshotName(), "fresh", dto.createdAt(), false));
    }

    @Override
    public void deleteSnapshot(String snapshotId) {
        snapshotRepository.findActive()
                .filter(dto -> dto.snapshotName().equals(snapshotId))
                .ifPresent(dto -> { throw new ActiveSnapshotDeletionException(snapshotId); });

        snapshotRepository.findByName(snapshotId)
                .orElseThrow(() -> new SnapshotNotFoundException(snapshotId));

        snapshotRepository.deleteByName(snapshotId);
    }
}
