package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierCommandService;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierEventHub;

public class MapFrontiersServerAPIImpl implements InternalMapFrontiersServerAPI {
    private final PluginScopedServerFrontierService frontiers;
    private final SimpleEventBus eventBus;
    private final ServerFrontierEventHub eventHub;

    public MapFrontiersServerAPIImpl(ServerFrontierCommandService commandService, ServerFrontierEventHub eventHub) {
        this.eventHub = eventHub;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ServerFrontierServiceImpl(commandService, eventHub);

        eventHub.subscribeCreated(this, frontier -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier))));
        eventHub.subscribeUpdated(this, frontier -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier))));
        eventHub.subscribeDeleted(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
    }

    public void close() {
        eventHub.unsubscribe(this);
    }

    @Override
    public PluginScopedServerFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
