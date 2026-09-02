package games.alejandrocoria.mapfrontiers.client.settings;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.LongSupplier;

@ParametersAreNonnullByDefault
public final class PendingOptimisticSettingsUpdates {
    public record Outbound(FrontierSettings snapshot, long baseRevision, long requestId) {
        public Outbound {
            snapshot = new FrontierSettings(snapshot);
        }
    }

    public record Reconciliation(boolean ignored, @Nullable FrontierSettings visibleSnapshot,
                                 long authoritativeRevision, @Nullable Outbound nextOutbound,
                                 boolean discardedLocalChanges) {
        private static Reconciliation ignoredResult(long authoritativeRevision) {
            return new Reconciliation(true, null, authoritativeRevision, null, false);
        }

        private static Reconciliation apply(FrontierSettings visibleSnapshot, long authoritativeRevision,
                                            @Nullable Outbound nextOutbound, boolean discardedLocalChanges) {
            return new Reconciliation(false, new FrontierSettings(visibleSnapshot), authoritativeRevision,
                    nextOutbound, discardedLocalChanges);
        }
    }

    private long requestId;
    private boolean inFlight;
    private @Nullable FrontierSettings desiredAfterInFlight;

    public @Nullable Outbound submit(FrontierSettings desiredSnapshot, long baseRevision, LongSupplier requestIds) {
        if (inFlight) {
            desiredAfterInFlight = new FrontierSettings(desiredSnapshot);
            return null;
        }

        requestId = requestIds.getAsLong();
        inFlight = true;
        return new Outbound(desiredSnapshot, baseRevision, requestId);
    }

    public Reconciliation reconcile(FrontierSettings authoritativeSnapshot, long authoritativeRevision,
                                    long requestId, OperationResolution resolution,
                                    @Nullable FrontierSettings currentVisibleSnapshot, long currentRevision,
                                    LongSupplier requestIds) {
        if (authoritativeRevision < currentRevision) {
            return Reconciliation.ignoredResult(authoritativeRevision);
        }

        if (!inFlight) {
            return authoritativeRevision == currentRevision
                    ? Reconciliation.ignoredResult(authoritativeRevision)
                    : Reconciliation.apply(authoritativeSnapshot, authoritativeRevision, null, false);
        }

        if (requestId != this.requestId) {
            if (authoritativeRevision == currentRevision) {
                return Reconciliation.ignoredResult(authoritativeRevision);
            }

            boolean discarded = currentVisibleSnapshot != null
                    && !authoritativeSnapshot.hasSameFunctionalState(currentVisibleSnapshot);
            clear();
            return Reconciliation.apply(authoritativeSnapshot, authoritativeRevision, null, discarded);
        }

        if (resolution == OperationResolution.Rejected) {
            boolean discarded = currentVisibleSnapshot != null
                    && !authoritativeSnapshot.hasSameFunctionalState(currentVisibleSnapshot);
            clear();
            return Reconciliation.apply(authoritativeSnapshot, authoritativeRevision, null, discarded);
        }

        FrontierSettings desired = desiredAfterInFlight;
        if (desired == null) {
            clear();
            return Reconciliation.apply(authoritativeSnapshot, authoritativeRevision, null, false);
        }

        long nextRequestId = requestIds.getAsLong();
        this.requestId = nextRequestId;
        desiredAfterInFlight = null;
        Outbound nextOutbound = new Outbound(desired, authoritativeRevision, nextRequestId);
        return Reconciliation.apply(desired, authoritativeRevision, nextOutbound, false);
    }

    public void clear() {
        requestId = 0L;
        inFlight = false;
        desiredAfterInFlight = null;
    }

    boolean hasPending() {
        return inFlight;
    }
}
