package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OverlayRefreshResult {
    public enum Operation {
        SHOW,
        REMOVE
    }

    public record FailureSummary(Operation operation, String layer, int count) {
    }

    private @Nullable Map<FailureKey, Integer> failureCounts;
    private @Nullable Throwable firstFailure;
    private boolean retryNeeded;

    void recordFailure(Operation operation, String layer, Throwable failure, boolean retryNeeded) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(failure, "failure");

        if (firstFailure == null) {
            firstFailure = failure;
        }
        if (failureCounts == null) {
            failureCounts = new LinkedHashMap<>();
        }
        failureCounts.merge(new FailureKey(operation, layer), 1, Integer::sum);
        this.retryNeeded |= retryNeeded;
    }

    public boolean hasFailures() {
        return firstFailure != null;
    }

    public boolean isRetryNeeded() {
        return retryNeeded;
    }

    public @Nullable Throwable getFirstFailure() {
        return firstFailure;
    }

    public int getFailureCount(Operation operation, String layer) {
        if (failureCounts == null) {
            return 0;
        }
        return failureCounts.getOrDefault(new FailureKey(operation, layer), 0);
    }

    public List<FailureSummary> getFailureSummaries() {
        if (failureCounts == null) {
            return List.of();
        }
        List<FailureSummary> summaries = new ArrayList<>(failureCounts.size());
        failureCounts.forEach((key, count) -> summaries.add(new FailureSummary(key.operation(), key.layer(), count)));
        return List.copyOf(summaries);
    }

    private record FailureKey(Operation operation, String layer) {
    }
}
