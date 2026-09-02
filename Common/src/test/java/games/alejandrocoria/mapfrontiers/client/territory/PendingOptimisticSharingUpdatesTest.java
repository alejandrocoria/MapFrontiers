package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingOptimisticSharingUpdatesTest {
    @Test
    void replaysQueuedIntentsInOrderAndSendsOnlyOneAtATime() {
        PendingOptimisticSharingUpdates updates = new PendingOptimisticSharingUpdates();
        AtomicLong requestIds = new AtomicLong();
        UUID frontierId = UUID.randomUUID();
        SettingsUser target = user("Target");
        SettingsUserShared addedUser = new SettingsUserShared(target, true);
        SettingsUserShared updatedUser = new SettingsUserShared(target, true);
        updatedUser.addAction(SettingsUserShared.Action.UpdateFrontier);

        PendingOptimisticSharingUpdates.Outbound add = updates.submit(frontierId,
                PendingOptimisticSharingUpdates.Intent.add(addedUser), 0L, requestIds::incrementAndGet);
        assertNull(updates.submit(frontierId, PendingOptimisticSharingUpdates.Intent.update(updatedUser), 0L,
                requestIds::incrementAndGet));
        assertNull(updates.submit(frontierId, PendingOptimisticSharingUpdates.Intent.remove(target), 0L,
                requestIds::incrementAndGet));
        FrontierSharingChange confirmedAdd = change(1L, addedUser);
        PendingOptimisticSharingUpdates.Reconciliation afterAdd = updates.reconcile(frontierId, confirmedAdd,
                0L, 7, 7, add.requestId(), OperationResolution.Accepted, requestIds::incrementAndGet);
        assertNull(afterAdd.visibleChange().getUsersShared());
        assertEquals(PendingOptimisticSharingUpdates.Type.Update, afterAdd.nextOutbound().intent().type());
        assertEquals(1L, afterAdd.nextOutbound().baseRevision());

        FrontierSharingChange confirmedUpdate = change(2L, updatedUser);
        PendingOptimisticSharingUpdates.Reconciliation afterUpdate = updates.reconcile(frontierId, confirmedUpdate,
                1L, 7, 7, afterAdd.nextOutbound().requestId(), OperationResolution.Accepted,
                requestIds::incrementAndGet);
        assertNull(afterUpdate.visibleChange().getUsersShared());
        assertEquals(PendingOptimisticSharingUpdates.Type.Remove, afterUpdate.nextOutbound().intent().type());
        assertEquals(2L, afterUpdate.nextOutbound().baseRevision());

        FrontierSharingChange confirmedRemove = change(3L);
        PendingOptimisticSharingUpdates.Reconciliation afterRemove = updates.reconcile(frontierId, confirmedRemove,
                2L, 7, 7, afterUpdate.nextOutbound().requestId(), OperationResolution.Accepted,
                requestIds::incrementAndGet);
        assertNull(afterRemove.nextOutbound());
        assertFalse(updates.hasPending(frontierId));
    }

    @Test
    void rejectionAndExternalActorClearQueueWhileOlderRevisionIsIgnored() {
        PendingOptimisticSharingUpdates updates = new PendingOptimisticSharingUpdates();
        AtomicLong requestIds = new AtomicLong();
        UUID frontierId = UUID.randomUUID();
        SettingsUserShared sharedUser = new SettingsUserShared(user("Target"), true);
        PendingOptimisticSharingUpdates.Outbound outbound = updates.submit(frontierId,
                PendingOptimisticSharingUpdates.Intent.add(sharedUser), 4L, requestIds::incrementAndGet);

        assertTrue(updates.reconcile(frontierId, change(3L), 4L, 8, 7, 0L,
                OperationResolution.Accepted, requestIds::incrementAndGet).ignored());
        assertTrue(updates.hasPending(frontierId));

        updates.reconcile(frontierId, change(4L), 4L, 7, 7, outbound.requestId(),
                OperationResolution.Rejected, requestIds::incrementAndGet);
        assertFalse(updates.hasPending(frontierId));

        updates.submit(frontierId, PendingOptimisticSharingUpdates.Intent.add(sharedUser), 4L,
                requestIds::incrementAndGet);
        PendingOptimisticSharingUpdates.Reconciliation external = updates.reconcile(frontierId, change(5L),
                4L, 8, 7, 0L, OperationResolution.Accepted, requestIds::incrementAndGet);
        assertFalse(external.ignored());
        assertFalse(updates.hasPending(frontierId));

        updates.submit(frontierId, PendingOptimisticSharingUpdates.Intent.add(sharedUser), 5L,
                requestIds::incrementAndGet);
        updates.clearAll();
        assertFalse(updates.hasPending(frontierId));
    }

    private static FrontierSharingChange change(long revision, SettingsUserShared... users) {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setSharingRevision(revision);
        change.setUsersShared(users.length == 0 ? null : java.util.List.of(users));
        return change;
    }

    private static SettingsUser user(String username) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = UUID.randomUUID();
        return user;
    }
}
