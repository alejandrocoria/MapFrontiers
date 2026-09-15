package games.alejandrocoria.mapfrontiers.client.settings;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingOptimisticSettingsUpdatesTest {
    @Test
    void coalescesLatestSnapshotAndUsesConfirmedRevisionForNextRequest() {
        PendingOptimisticSettingsUpdates updates = new PendingOptimisticSettingsUpdates();
        AtomicLong requestIds = new AtomicLong();
        FrontierSettings first = settingsWith(FrontierSettings.Action.CreateGlobalFrontier);
        PendingOptimisticSettingsUpdates.Outbound outbound = updates.submit(first, 3L,
                requestIds::incrementAndGet);
        FrontierSettings latest = settingsWith(FrontierSettings.Action.DeleteGlobalFrontier);
        assertNull(updates.submit(latest, 3L, requestIds::incrementAndGet));

        FrontierSettings confirmed = new FrontierSettings(first);
        PendingOptimisticSettingsUpdates.Reconciliation reconciliation = updates.reconcile(confirmed, 4L,
                outbound.requestId(), OperationResolution.Accepted, latest, 3L, requestIds::incrementAndGet);

        assertTrue(reconciliation.visibleSnapshot().getEveryoneGroup()
                .hasAction(FrontierSettings.Action.DeleteGlobalFrontier));
        assertEquals(4L, reconciliation.authoritativeRevision());
        assertEquals(4L, reconciliation.nextOutbound().baseRevision());
        assertEquals(2L, reconciliation.nextOutbound().requestId());
        assertFalse(reconciliation.discardedLocalChanges());
    }

    @Test
    void equalPollingSnapshotAndOlderRevisionDoNotReplaceOptimisticState() {
        PendingOptimisticSettingsUpdates updates = new PendingOptimisticSettingsUpdates();
        AtomicLong requestIds = new AtomicLong();
        FrontierSettings optimistic = settingsWith(FrontierSettings.Action.UpdateGlobalFrontier);
        updates.submit(optimistic, 5L, requestIds::incrementAndGet);

        assertTrue(updates.reconcile(new FrontierSettings(), 5L, 0L, OperationResolution.Accepted,
                optimistic, 5L, requestIds::incrementAndGet).ignored());
        assertTrue(updates.reconcile(new FrontierSettings(), 4L, 0L, OperationResolution.Accepted,
                optimistic, 5L, requestIds::incrementAndGet).ignored());
        assertTrue(updates.hasPending());
    }

    @Test
    void rejectionOrNewerUnknownResponseDiscardsDifferentLocalStateAndClearResetsTracker() {
        PendingOptimisticSettingsUpdates updates = new PendingOptimisticSettingsUpdates();
        AtomicLong requestIds = new AtomicLong();
        FrontierSettings optimistic = settingsWith(FrontierSettings.Action.UpdateSettings);
        PendingOptimisticSettingsUpdates.Outbound outbound = updates.submit(optimistic, 0L,
                requestIds::incrementAndGet);

        PendingOptimisticSettingsUpdates.Reconciliation rejected = updates.reconcile(new FrontierSettings(), 0L,
                outbound.requestId(), OperationResolution.Rejected, optimistic, 0L,
                requestIds::incrementAndGet);
        assertTrue(rejected.discardedLocalChanges());
        assertFalse(updates.hasPending());

        updates.submit(optimistic, 0L, requestIds::incrementAndGet);
        PendingOptimisticSettingsUpdates.Reconciliation external = updates.reconcile(new FrontierSettings(), 1L,
                0L, OperationResolution.Accepted, optimistic, 0L, requestIds::incrementAndGet);
        assertTrue(external.discardedLocalChanges());
        assertFalse(updates.hasPending());

        updates.submit(optimistic, 1L, requestIds::incrementAndGet);
        updates.clear();
        assertFalse(updates.hasPending());
    }

    @Test
    void unknownResponseFromPreviousScreenOnlyAppliesWhenNewer() {
        PendingOptimisticSettingsUpdates updates = new PendingOptimisticSettingsUpdates();
        AtomicLong requestIds = new AtomicLong();
        FrontierSettings current = new FrontierSettings();

        assertTrue(updates.reconcile(current, 2L, 99L, OperationResolution.Accepted, current, 2L,
                requestIds::incrementAndGet).ignored());
        PendingOptimisticSettingsUpdates.Reconciliation newer = updates.reconcile(current, 3L, 99L,
                OperationResolution.Accepted, current, 2L, requestIds::incrementAndGet);
        assertFalse(newer.ignored());
        assertFalse(newer.discardedLocalChanges());
    }

    private static FrontierSettings settingsWith(FrontierSettings.Action action) {
        FrontierSettings settings = new FrontierSettings();
        settings.getEveryoneGroup().addAction(action);
        return settings;
    }
}
