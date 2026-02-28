package games.alejandrocoria.mapfrontiers.api.event;

import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;

public record FrontierCreatedEvent(FrontierDataView frontier) {
}
