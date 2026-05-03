package com.sgarsgaya.codeatlas.unit.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgarsgaya.codeatlas.dto.SnapshotRequestDto;
import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.exception.ActiveSnapshotDeletionException;
import com.sgarsgaya.codeatlas.exception.DuplicateSnapshotException;
import com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException;
import com.sgarsgaya.codeatlas.repositories.SnapshotRepository;
import com.sgarsgaya.codeatlas.service.impl.SnapshotServiceImpl;
import com.sgarsgaya.codeatlas.util.TimeProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceImplTest {

    @Mock SnapshotRepository snapshotRepository;
    @Mock TimeProvider timeProvider;

    private SnapshotServiceImpl service;

    private static final String NOW_STR = "2026-05-03T00:00:00Z";
    private static final SnapshotResponseDto S1 = new SnapshotResponseDto(1L, "S1", NOW_STR);
    private static final SnapshotResponseDto S2 = new SnapshotResponseDto(2L, "S2", NOW_STR);

    @BeforeEach
    void setUp() {
        service = new SnapshotServiceImpl(snapshotRepository, timeProvider);
    }

    @Test
    void createSnapshot_delegatesToRepository() {
        when(snapshotRepository.findByName("S1")).thenReturn(Optional.empty());
        when(timeProvider.now()).thenReturn(java.time.Instant.parse(NOW_STR));
        when(snapshotRepository.save(eq("S1"), any())).thenReturn(S1);

        var result = service.createSnapshot(new SnapshotRequestDto("S1"));

        assertThat(result.snapshotName()).isEqualTo("S1");
        assertThat(result.createdAt()).isEqualTo(NOW_STR);
    }

    @Test
    void createSnapshot_throwsDuplicate_whenNameExists() {
        when(snapshotRepository.findByName("S1")).thenReturn(Optional.of(S1));

        assertThatThrownBy(() -> service.createSnapshot(new SnapshotRequestDto("S1")))
                .isInstanceOf(DuplicateSnapshotException.class);
    }

    @Test
    void listSnapshots_delegatesToRepository() {
        when(snapshotRepository.findAll()).thenReturn(List.of(S2));

        assertThat(service.listSnapshots()).containsExactly(S2);
    }

    @Test
    void getActiveSnapshot_returnsEmpty_whenNoneActive() {
        when(snapshotRepository.findActive()).thenReturn(Optional.empty());

        assertThat(service.getActiveSnapshot()).isEmpty();
    }

    @Test
    void getActiveSnapshot_returnsFreshStatus() {
        when(snapshotRepository.findActive()).thenReturn(Optional.of(S1));

        var result = service.getActiveSnapshot();

        assertThat(result).isPresent();
        assertThat(result.get().snapshotId()).isEqualTo("S1");
        assertThat(result.get().graphStatus()).isEqualTo("fresh");
    }

    @Test
    void deleteSnapshot_throws_whenSnapshotIsActive() {
        when(snapshotRepository.findActive()).thenReturn(Optional.of(S1));

        assertThatThrownBy(() -> service.deleteSnapshot("S1"))
                .isInstanceOf(ActiveSnapshotDeletionException.class);
    }

    @Test
    void deleteSnapshot_throws_whenSnapshotNotFound() {
        when(snapshotRepository.findActive()).thenReturn(Optional.empty());
        when(snapshotRepository.findByName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSnapshot("ghost"))
                .isInstanceOf(SnapshotNotFoundException.class);
    }

    @Test
    void deleteSnapshot_succeeds_forInactiveSnapshot() {
        when(snapshotRepository.findActive()).thenReturn(Optional.of(S1));
        when(snapshotRepository.findByName("S2")).thenReturn(Optional.of(S2));

        service.deleteSnapshot("S2");

        verify(snapshotRepository).deleteByName("S2");
    }
}
