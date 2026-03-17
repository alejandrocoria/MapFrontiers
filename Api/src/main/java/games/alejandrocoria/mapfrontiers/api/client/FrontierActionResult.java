package games.alejandrocoria.mapfrontiers.api.client;

import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;

import java.util.Optional;

/**
 * Result envelope returned by client-side frontier operations.
 */
public record FrontierActionResult(FrontierActionStatus status,
                                   Optional<FrontierId> frontierId,
                                   Optional<FrontierDataView> frontier) {

    /**
     * Creates a result indicating the operation was applied immediately.
     */
    public static FrontierActionResult applied(FrontierDataView frontier) {
        return new FrontierActionResult(FrontierActionStatus.APPLIED_LOCAL, Optional.of(frontier.id()), Optional.of(frontier));
    }

    /**
     * Creates a result indicating the request was accepted for asynchronous server processing.
     */
    public static FrontierActionResult acceptedAsync(FrontierId frontierId) {
        return new FrontierActionResult(FrontierActionStatus.ACCEPTED_ASYNC, Optional.of(frontierId), Optional.empty());
    }

    /**
     * Creates a result indicating the target id was not found.
     */
    public static FrontierActionResult notFound(FrontierId frontierId) {
        return new FrontierActionResult(FrontierActionStatus.NOT_FOUND, Optional.of(frontierId), Optional.empty());
    }

    /**
     * Creates a result indicating the request was rejected.
     */
    public static FrontierActionResult rejected() {
        return new FrontierActionResult(FrontierActionStatus.REJECTED, Optional.empty(), Optional.empty());
    }
}
