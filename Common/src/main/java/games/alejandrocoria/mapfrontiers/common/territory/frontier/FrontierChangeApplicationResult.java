package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class FrontierChangeApplicationResult {
    public enum Status {
        APPLIED,
        NO_CHANGE,
        REJECTED
    }

    private final Status status;
    private final @Nullable FrontierData frontier;
    private final FrontierChange effectiveChange;
    private final @Nullable String rejectionReason;

    private FrontierChangeApplicationResult(Status status, @Nullable FrontierData frontier, FrontierChange effectiveChange,
                                            @Nullable String rejectionReason) {
        this.status = status;
        this.frontier = frontier;
        this.effectiveChange = effectiveChange;
        this.rejectionReason = rejectionReason;
    }

    public static FrontierChangeApplicationResult applied(FrontierData frontier, FrontierChange effectiveChange) {
        return new FrontierChangeApplicationResult(Status.APPLIED, frontier, effectiveChange, null);
    }

    public static FrontierChangeApplicationResult noChange(FrontierData frontier) {
        return new FrontierChangeApplicationResult(Status.NO_CHANGE, frontier, new FrontierChange(), null);
    }

    public static FrontierChangeApplicationResult rejected(String reason) {
        return new FrontierChangeApplicationResult(Status.REJECTED, null, new FrontierChange(), reason);
    }

    public Status status() {
        return status;
    }

    public boolean isApplied() {
        return status == Status.APPLIED;
    }

    public boolean isNoChange() {
        return status == Status.NO_CHANGE;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    public @Nullable FrontierData frontier() {
        return frontier;
    }

    public FrontierChange effectiveChange() {
        return effectiveChange;
    }

    public @Nullable String rejectionReason() {
        return rejectionReason;
    }
}
