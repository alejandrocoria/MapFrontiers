package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.CollectionCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersServerAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerCollectionService;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationService;

public class MapFrontiersServerAPIImpl implements InternalMapFrontiersServerAPI {
    private final PluginScopedServerFrontierService frontiers;
    private final PluginScopedServerCollectionService collections;
    private final SimpleEventBus eventBus;
    private final ServerFrontierEvents frontierEvents;
    private final ServerCollectionEvents collectionEvents;

    public MapFrontiersServerAPIImpl(ServerFrontierOperationService operationService, ServerFrontierEvents frontierEvents, ServerCollectionEvents collectionEvents) {
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ServerFrontierServiceImpl(operationService, frontierEvents);
        this.collections = new ServerCollectionServiceImpl(operationService, collectionEvents);

        frontierEvents.subscribeCreated(this, frontier -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEvents.subscribeUpdated(this, frontier -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier))));
        frontierEvents.subscribeDeleted(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
        collectionEvents.subscribeCreated(this, collection -> eventBus.post(new CollectionCreatedEvent(ApiConverters.fromCollection(collection))));
        collectionEvents.subscribeUpdated(this, collection -> eventBus.post(new CollectionUpdatedEvent(ApiConverters.fromCollection(collection))));
        collectionEvents.subscribeDeleted(this, collectionId -> eventBus.post(new CollectionDeletedEvent(new CollectionId(collectionId))));
    }

    public void close() {
        frontierEvents.unsubscribe(this);
        collectionEvents.unsubscribe(this);
    }

    @Override
    public PluginScopedServerFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public PluginScopedServerCollectionService collections() {
        return collections;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
