package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.api.client.ClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.client.IMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;

public class MapFrontiersClientAPIImpl implements IMapFrontiersClientAPI {
    private final ClientFrontierService frontiers;
    private final SimpleEventBus eventBus;

    public MapFrontiersClientAPIImpl() {
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ClientFrontierServiceImpl(eventBus);

        ClientEventHandler.subscribeNewFrontierEvent(this, (frontier, playerId) -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier))));
        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontier, playerId) -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier))));
        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
    }

    public void close() {
        ClientEventHandler.unsubscribeAllEvents(this);
    }

    @Override
    public ClientFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
