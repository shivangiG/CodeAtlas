package com.sgarsgaya.codeatlas.controllers;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.SnapshotsApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.dto.SnapshotRequestDto;
import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;
import com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException;
import com.sgarsgaya.codeatlas.model.CreateSnapshotRequest;
import com.sgarsgaya.codeatlas.model.GetActiveSnapshot200Response;
import com.sgarsgaya.codeatlas.model.ListSnapshots200ResponseInner;
import com.sgarsgaya.codeatlas.service.SnapshotService;

@RestController
@RequestMapping(AppConstants.API_BASE)
public class SnapshotController implements SnapshotsApi {

    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public ResponseEntity<ListSnapshots200ResponseInner> createSnapshot(CreateSnapshotRequest createSnapshotRequest) {
        SnapshotResponseDto dto = snapshotService.createSnapshot(
                new SnapshotRequestDto(createSnapshotRequest.getSnapshotName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(dto));
    }

    @Override
    public ResponseEntity<List<ListSnapshots200ResponseInner>> listSnapshots() {
        return ResponseEntity.ok(
                snapshotService.listSnapshots().stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<GetActiveSnapshot200Response> getActiveSnapshot() {
        return snapshotService.getActiveSnapshot()
                .map(dto -> ResponseEntity.ok(new GetActiveSnapshot200Response()
                        .snapshotId(dto.snapshotId())
                        .graphStatus(GetActiveSnapshot200Response.GraphStatusEnum.fromValue(dto.graphStatus()))
                        .createdAt(OffsetDateTime.parse(dto.createdAt()))
                        .rebuildWorkerActive(dto.rebuildWorkerActive())))
                .orElseThrow(() -> new SnapshotNotFoundException("no active snapshot"));
    }

    @Override
    public ResponseEntity<Void> deleteSnapshot(String snapshotId) {
        snapshotService.deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    // ─── Controller-owned mapping: SnapshotResponseDto → API model ───────────
    // The controller is the adapter between the service DTO surface and the
    // OpenAPI contract. API contract changes are contained here.

    private ListSnapshots200ResponseInner toResponse(SnapshotResponseDto dto) {
        return new ListSnapshots200ResponseInner()
                .id(dto.id())
                .snapshotName(dto.snapshotName())
                .createdAt(OffsetDateTime.parse(dto.createdAt()));
    }
}
