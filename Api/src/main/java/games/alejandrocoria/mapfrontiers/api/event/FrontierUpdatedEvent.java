package games.alejandrocoria.mapfrontiers.api.event;

import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;

/**
 * Emitted after a frontier is updated or converted and visible through API state.
 */
public record FrontierUpdatedEvent(FrontierDataView frontier) {
}
