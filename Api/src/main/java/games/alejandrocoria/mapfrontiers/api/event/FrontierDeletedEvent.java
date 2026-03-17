package games.alejandrocoria.mapfrontiers.api.event;

import games.alejandrocoria.mapfrontiers.api.model.FrontierId;

/**
 * Emitted after a frontier is deleted.
 */
public record FrontierDeletedEvent(FrontierId frontierId) {
}
