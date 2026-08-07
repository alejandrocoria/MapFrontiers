package games.alejandrocoria.mapfrontiers.client.territory.overlay;

/**
 * Allows one automatic retry; a later functional invalidation starts a new retry cycle.
 */
public final class OverlayRetryLimiter {
    private boolean retryConsumed;

    public void resetForFunctionalInvalidation() {
        retryConsumed = false;
    }

    public boolean consumeRetry(OverlayRefreshResult result) {
        if (!result.isRetryNeeded() || retryConsumed) {
            return false;
        }

        retryConsumed = true;
        return true;
    }
}
