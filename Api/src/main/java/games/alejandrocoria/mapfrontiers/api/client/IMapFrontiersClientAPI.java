package games.alejandrocoria.mapfrontiers.api.client;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

public interface IMapFrontiersClientAPI {
    ClientFrontierService frontiers();
    EventBus events();
}
