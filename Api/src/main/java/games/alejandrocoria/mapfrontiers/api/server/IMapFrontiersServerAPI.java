package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

public interface IMapFrontiersServerAPI {
    ServerFrontierService frontiers();
    EventBus events();
}
