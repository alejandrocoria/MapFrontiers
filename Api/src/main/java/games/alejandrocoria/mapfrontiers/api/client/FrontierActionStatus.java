package games.alejandrocoria.mapfrontiers.api.client;

/**
 * Outcome category for a client-side frontier request.
 */
public enum FrontierActionStatus {
    /**
     * Applied immediately to client-managed state.
     */
    APPLIED_LOCAL,
    /**
     * Accepted and forwarded to the logical server; final result arrives via events/state updates.
     */
    ACCEPTED_ASYNC,
    /**
     * Target frontier id was not found in currently known state.
     */
    NOT_FOUND,
    /**
     * Request was rejected (for example by permissions or unsupported context).
     */
    REJECTED
}
