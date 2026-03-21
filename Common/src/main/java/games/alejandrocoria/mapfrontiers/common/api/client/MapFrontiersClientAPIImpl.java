package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.frontier.client.ClientFrontierEventBridge;

public class MapFrontiersClientAPIImpl implements InternalMapFrontiersClientAPI {
    private final PluginScopedClientFrontierService frontiers;
    private final SimpleEventBus eventBus;
    private final ClientFrontierEventBridge frontierEventBridge;

    public MapFrontiersClientAPIImpl(ClientFrontierEventBridge frontierEventBridge) {
        this.frontierEventBridge = frontierEventBridge;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ClientFrontierServiceImpl();

        frontierEventBridge.subscribeCreated(this, (frontier, playerId) -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEventBridge.subscribeUpdated(this, (frontier, playerId) -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEventBridge.subscribeDeleted(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
    }

    public void close() {
        frontierEventBridge.unsubscribe(this);
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
