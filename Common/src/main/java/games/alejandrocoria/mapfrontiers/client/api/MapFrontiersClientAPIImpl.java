package games.alejandrocoria.mapfrontiers.client.api;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.client.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;

public class MapFrontiersClientAPIImpl implements InternalMapFrontiersClientAPI {
    private final PluginScopedClientFrontierService frontiers;
    private final SimpleEventBus eventBus;
    private final ClientFrontierEvents frontierEvents;

    public MapFrontiersClientAPIImpl(ClientFrontierEvents frontierEvents) {
        this.frontierEvents = frontierEvents;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ClientFrontierServiceImpl();

        frontierEvents.subscribeCreated(this, (frontier, playerId) -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEvents.subscribeUpdated(this, (frontier, playerId) -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEvents.subscribeDeleted(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
    }

    public void close() {
        frontierEvents.unsubscribe(this);
    }

    @Override
    public PluginScopedClientFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
