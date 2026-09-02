package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingOptimisticCollectionUpdatesTest {
    @Test
    void coalescesToLatestDesiredSnapshotAndUsesConfirmedRevisionForNextRequest() {
        PendingOptimisticCollectionUpdates updates = new PendingOptimisticCollectionUpdates();
        AtomicLong requestIds = new AtomicLong();
        CollectionData first = collection("first", 5L, 100L);

        PendingOptimisticCollectionUpdates.Outbound firstOutbound = updates.submit(first,
                requestIds::incrementAndGet);
        CollectionData latest = new CollectionData(first);
        latest.setName("latest");
        assertNull(updates.submit(latest, requestIds::incrementAndGet));

        CollectionData confirmedFirst = collection(first.getId(), "first", 6L, 200L);
        PendingOptimisticCollectionUpdates.Reconciliation reconciliation = updates.reconcile(
                confirmedFirst, latest, firstOutbound.requestId(), OperationResolution.Accepted, true,
                requestIds::incrementAndGet);

        assertFalse(reconciliation.ignored());
        assertEquals("latest", reconciliation.visibleSnapshot().getName());
        assertEquals(6L, reconciliation.visibleSnapshot().getCollectionRevision());
        assertEquals(200L, reconciliation.visibleSnapshot().getModified().getTime());
        assertEquals(6L, reconciliation.nextOutbound().baseRevision());
        assertEquals(2L, reconciliation.nextOutbound().requestId());
        assertEquals("latest", reconciliation.nextOutbound().snapshot().getName());

        CollectionData confirmedLatest = collection(first.getId(), "latest", 7L, 300L);
        updates.reconcile(confirmedLatest, reconciliation.visibleSnapshot(), 2L, OperationResolution.Accepted,
                true, requestIds::incrementAndGet);
        assertFalse(updates.hasPending(first.getId()));
    }

    @Test
    void equalExternalSnapshotPreservesOptimisticFieldsAndUpdatesAuthoritativeMetadata() {
        PendingOptimisticCollectionUpdates updates = new PendingOptimisticCollectionUpdates();
        AtomicLong requestIds = new AtomicLong();
        CollectionData optimistic = collection("optimistic", 4L, 100L);
        updates.submit(optimistic, requestIds::incrementAndGet);
        CollectionData authoritative = collection(optimistic.getId(), "base", 4L, 200L);

        PendingOptimisticCollectionUpdates.Reconciliation reconciliation = updates.reconcile(
                authoritative, optimistic, 0L, OperationResolution.Accepted, false,
                requestIds::incrementAndGet);

        assertEquals("optimistic", reconciliation.visibleSnapshot().getName());
        assertEquals(200L, reconciliation.visibleSnapshot().getModified().getTime());
        assertTrue(updates.hasPending(optimistic.getId()));
    }

    @Test
    void rejectionAndNewerExternalUpdateClearPendingStateWhileOlderRevisionIsIgnored() {
        PendingOptimisticCollectionUpdates updates = new PendingOptimisticCollectionUpdates();
        AtomicLong requestIds = new AtomicLong();
        CollectionData optimistic = collection("optimistic", 3L, 100L);
        PendingOptimisticCollectionUpdates.Outbound outbound = updates.submit(optimistic,
                requestIds::incrementAndGet);

        CollectionData older = collection(optimistic.getId(), "older", 2L, 50L);
        assertTrue(updates.reconcile(older, optimistic, 0L, OperationResolution.Accepted, false,
                requestIds::incrementAndGet).ignored());
        assertTrue(updates.hasPending(optimistic.getId()));

        CollectionData rejected = collection(optimistic.getId(), "server", 3L, 100L);
        updates.reconcile(rejected, optimistic, outbound.requestId(), OperationResolution.Rejected, true,
                requestIds::incrementAndGet);
        assertFalse(updates.hasPending(optimistic.getId()));

        updates.submit(optimistic, requestIds::incrementAndGet);
        CollectionData external = collection(optimistic.getId(), "external", 4L, 200L);
        PendingOptimisticCollectionUpdates.Reconciliation externalResult = updates.reconcile(
                external, optimistic, 0L, OperationResolution.Accepted, false, requestIds::incrementAndGet);
        assertEquals("external", externalResult.visibleSnapshot().getName());
        assertFalse(updates.hasPending(optimistic.getId()));

        updates.submit(optimistic, requestIds::incrementAndGet);
        updates.clearAll();
        assertFalse(updates.hasPending(optimistic.getId()));
    }

    private static CollectionData collection(String name, long revision, long modified) {
        return collection(UUID.randomUUID(), name, revision, modified);
    }

    private static CollectionData collection(UUID id, String name, long revision, long modified) {
        CollectionData collection = new CollectionData();
        collection.setId(id);
        collection.setName(name);
        collection.setModified(new Date(modified));
        collection.setCollectionRevision(revision);
        return collection;
    }
}
