package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

@ParametersAreNonnullByDefault
final class PendingOptimisticCollectionUpdates {
    record Outbound(CollectionData snapshot, long baseRevision, long requestId) {
        Outbound {
            snapshot = new CollectionData(snapshot);
        }
    }

    record Reconciliation(boolean ignored, @Nullable CollectionData visibleSnapshot,
                          @Nullable Outbound nextOutbound) {
        static Reconciliation ignoredResult() {
            return new Reconciliation(true, null, null);
        }

        static Reconciliation apply(CollectionData visibleSnapshot, @Nullable Outbound nextOutbound) {
            return new Reconciliation(false, new CollectionData(visibleSnapshot), nextOutbound);
        }
    }

    private static final class State {
        private long requestId;
        private @Nullable CollectionData desiredAfterInFlight;

        private State(long requestId) {
            this.requestId = requestId;
        }
    }

    private final Map<UUID, State> states = new HashMap<>();

    @Nullable Outbound submit(CollectionData desiredSnapshot, LongSupplier requestIds) {
        UUID collectionId = desiredSnapshot.getId();
        State state = states.get(collectionId);
        if (state != null) {
            state.desiredAfterInFlight = new CollectionData(desiredSnapshot);
            return null;
        }

        long requestId = requestIds.getAsLong();
        states.put(collectionId, new State(requestId));
        return new Outbound(desiredSnapshot, desiredSnapshot.getCollectionRevision(), requestId);
    }

    Reconciliation reconcile(CollectionData authoritativeSnapshot, CollectionData currentVisibleSnapshot,
                             long requestId, OperationResolution resolution, boolean currentActor,
                             LongSupplier requestIds) {
        UUID collectionId = authoritativeSnapshot.getId();
        long authoritativeRevision = authoritativeSnapshot.getCollectionRevision();
        long currentRevision = currentVisibleSnapshot.getCollectionRevision();
        if (authoritativeRevision < currentRevision) {
            return Reconciliation.ignoredResult();
        }

        State state = states.get(collectionId);
        if (state == null) {
            return Reconciliation.apply(authoritativeSnapshot, null);
        }

        boolean knownResponse = currentActor && requestId == state.requestId;
        if (!knownResponse) {
            if (authoritativeRevision == currentRevision) {
                return Reconciliation.apply(restoreEditableState(authoritativeSnapshot, currentVisibleSnapshot), null);
            }

            states.remove(collectionId);
            return Reconciliation.apply(authoritativeSnapshot, null);
        }

        if (resolution == OperationResolution.Rejected) {
            states.remove(collectionId);
            return Reconciliation.apply(authoritativeSnapshot, null);
        }

        CollectionData desiredAfterInFlight = state.desiredAfterInFlight;
        if (desiredAfterInFlight == null) {
            states.remove(collectionId);
            return Reconciliation.apply(authoritativeSnapshot, null);
        }

        CollectionData visibleSnapshot = restoreEditableState(authoritativeSnapshot, desiredAfterInFlight);
        long nextRequestId = requestIds.getAsLong();
        state.requestId = nextRequestId;
        state.desiredAfterInFlight = null;
        Outbound nextOutbound = new Outbound(visibleSnapshot, authoritativeRevision, nextRequestId);
        return Reconciliation.apply(visibleSnapshot, nextOutbound);
    }

    void clear(UUID collectionId) {
        states.remove(collectionId);
    }

    void clearAll() {
        states.clear();
    }

    boolean hasPending(UUID collectionId) {
        return states.containsKey(collectionId);
    }

    private static CollectionData restoreEditableState(CollectionData authoritativeSnapshot,
                                                       CollectionData desiredSnapshot) {
        CollectionData restored = new CollectionData(authoritativeSnapshot);
        restored.setName(desiredSnapshot.getName());
        restored.setColor(desiredSnapshot.getColor());
        restored.setVisibilityData(desiredSnapshot.getVisibilityData());
        restored.setBannerData(desiredSnapshot.getBannerData());
        return restored;
    }
}
