package games.alejandrocoria.mapfrontiers.api.event;

import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;

/**
 * Emitted after a frontier is created and visible through API state.
 */
public record FrontierCreatedEvent(FrontierDataView frontier) {
}
