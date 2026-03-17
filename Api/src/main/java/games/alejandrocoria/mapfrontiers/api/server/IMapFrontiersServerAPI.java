package games.alejandrocoria.mapfrontiers.api.server;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;

/**
 * Root entry point for server-side API access.
 */
@SuppressWarnings("unused")
public interface IMapFrontiersServerAPI {
    /**
     * Frontier operations available on the server side.
     *
     * @return server frontier service
     */
    ServerFrontierService frontiers();

    /**
     * Event bus scoped to server-side API events.
     *
     * @return server event bus
     */
    EventBus events();
}
