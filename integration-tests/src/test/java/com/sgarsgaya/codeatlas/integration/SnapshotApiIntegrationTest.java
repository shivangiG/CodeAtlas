package com.sgarsgaya.codeatlas.integration;

import org.junit.jupiter.api.Test;

import com.sgarsgaya.codeatlas.client.ClientException;
import com.sgarsgaya.codeatlas.model.SnapshotRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotApiIntegrationTest extends IntegrationTestBase {

    // ─── POST /snapshots ──────────────────────────────────────────────────────

    @Test
    void createSnapshot_returns201_withPopulatedBody() {
        var request = new SnapshotRequest().snapshotName("S-01");

        var response = client.createSnapshot(request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getSnapshotName()).isEqualTo("S-01");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void createSnapshot_throws409_onDuplicateName() {
        var request = new SnapshotRequest().snapshotName("S-dup");
        client.createSnapshot(request);

        assertThatThrownBy(() -> client.createSnapshot(request))
                .isInstanceOf(ClientException.class)
                .satisfies(ex -> {
                    assertThat(((ClientException) ex).getStatusCode()).isEqualTo(409);
                    assertThat(((ClientException) ex).getErrorCode()).isEqualTo("DUPLICATE_SNAPSHOT");
                });
    }

    // ─── GET /snapshots ───────────────────────────────────────────────────────

    @Test
    void listSnapshots_returnsEmptyList_whenNoneCreated() {
        assertThat(client.listSnapshots()).isEmpty();
    }

    @Test
    void listSnapshots_includesAllCreatedSnapshots() {
        client.createSnapshot(new SnapshotRequest().snapshotName("A"));
        client.createSnapshot(new SnapshotRequest().snapshotName("B"));

        var snapshots = client.listSnapshots();

        assertThat(snapshots).hasSize(2);
        assertThat(snapshots).extracting("snapshotName").containsExactlyInAnyOrder("A", "B");
    }

    // ─── GET /snapshots/active ────────────────────────────────────────────────

    @Test
    void getActiveSnapshot_throws404_whenNoSnapshotsExist() {
        assertThatThrownBy(() -> client.getActiveSnapshot())
                .isInstanceOf(ClientException.class)
                .satisfies(ex -> assertThat(((ClientException) ex).getStatusCode()).isEqualTo(404));
    }

    @Test
    void getActiveSnapshot_returnsMostRecentSnapshot() {
        client.createSnapshot(new SnapshotRequest().snapshotName("first"));
        client.createSnapshot(new SnapshotRequest().snapshotName("latest"));

        var active = client.getActiveSnapshot();

        assertThat(active.getSnapshotId()).isEqualTo("latest");
        assertThat(active.getGraphStatus()).isNotNull();
    }

    // ─── DELETE /snapshots/{snapshotId} ───────────────────────────────────────

    @Test
    void deleteSnapshot_returns204_forInactiveSnapshot() {
        client.createSnapshot(new SnapshotRequest().snapshotName("older"));
        client.createSnapshot(new SnapshotRequest().snapshotName("newest")); // newest is now active

        client.deleteSnapshot("older"); // older is inactive — should succeed

        assertThat(client.listSnapshots()).extracting("snapshotName").containsExactly("newest");
    }

    @Test
    void deleteSnapshot_throws409_whenDeletingActiveSnapshot() {
        client.createSnapshot(new SnapshotRequest().snapshotName("active-one"));

        assertThatThrownBy(() -> client.deleteSnapshot("active-one"))
                .isInstanceOf(ClientException.class)
                .satisfies(ex -> {
                    assertThat(((ClientException) ex).getStatusCode()).isEqualTo(409);
                    assertThat(((ClientException) ex).getErrorCode()).isEqualTo("ACTIVE_SNAPSHOT");
                });
    }

    @Test
    void deleteSnapshot_throws404_whenSnapshotDoesNotExist() {
        assertThatThrownBy(() -> client.deleteSnapshot("ghost"))
                .isInstanceOf(ClientException.class)
                .satisfies(ex -> {
                    assertThat(((ClientException) ex).getStatusCode()).isEqualTo(404);
                    assertThat(((ClientException) ex).getErrorCode()).isEqualTo("NOT_FOUND");
                });
    }
}
