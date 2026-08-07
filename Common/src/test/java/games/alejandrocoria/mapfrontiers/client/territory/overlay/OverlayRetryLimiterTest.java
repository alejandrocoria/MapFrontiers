package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayRetryLimiterTest {
    @Test
    void consumeRetry_repeatedFailure_schedulesOnlyOneRetryUntilInvalidated() {
        OverlayRetryLimiter limiter = new OverlayRetryLimiter();
        OverlayRefreshResult failedResult = failedResult();

        assertTrue(limiter.consumeRetry(failedResult));
        assertFalse(limiter.consumeRetry(failedResult));

        limiter.resetForFunctionalInvalidation();
        assertTrue(limiter.consumeRetry(failedResult));
    }

    @Test
    void consumeRetry_successfulResult_doesNotConsumeRetryAllowance() {
        OverlayRetryLimiter limiter = new OverlayRetryLimiter();

        assertFalse(limiter.consumeRetry(new OverlayRefreshResult()));
        assertTrue(limiter.consumeRetry(failedResult()));
    }

    private static OverlayRefreshResult failedResult() {
        OverlayRefreshResult result = new OverlayRefreshResult();
        result.recordFailure(OverlayRefreshResult.Operation.SHOW, "markers",
                new RuntimeException("show failed"), true);
        return result;
    }
}
