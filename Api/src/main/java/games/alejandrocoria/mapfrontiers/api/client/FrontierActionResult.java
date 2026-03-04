package games.alejandrocoria.mapfrontiers.api.client;

import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;

import java.util.Optional;

public record FrontierActionResult(FrontierActionStatus status,
                                   Optional<FrontierId> frontierId,
                                   Optional<FrontierDataView> frontier) {

    public static FrontierActionResult applied(FrontierDataView frontier) {
        return new FrontierActionResult(FrontierActionStatus.APPLIED_LOCAL, Optional.of(frontier.id()), Optional.of(frontier));
    }

    public static FrontierActionResult acceptedAsync(FrontierId frontierId) {
        return new FrontierActionResult(FrontierActionStatus.ACCEPTED_ASYNC, Optional.of(frontierId), Optional.empty());
    }

    public static FrontierActionResult notFound(FrontierId frontierId) {
        return new FrontierActionResult(FrontierActionStatus.NOT_FOUND, Optional.of(frontierId), Optional.empty());
    }

    public static FrontierActionResult rejected() {
        return new FrontierActionResult(FrontierActionStatus.REJECTED, Optional.empty(), Optional.empty());
    }
}
