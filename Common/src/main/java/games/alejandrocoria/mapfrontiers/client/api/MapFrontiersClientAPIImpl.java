package games.alejandrocoria.mapfrontiers.client.api;

import games.alejandrocoria.mapfrontiers.api.event.CollectionCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.InternalMapFrontiersClientAPI;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientCollectionService;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;

public class MapFrontiersClientAPIImpl implements InternalMapFrontiersClientAPI {
    private final PluginScopedClientFrontierService frontiers;
    private final PluginScopedClientCollectionService collections;
    private final SimpleEventBus eventBus;
    private final ClientFrontierEvents frontierEvents;
    private final ClientCollectionEvents collectionEvents;

    public MapFrontiersClientAPIImpl(ClientFrontierEvents frontierEvents, ClientCollectionEvents collectionEvents,
                                    PlayerNameResolver playerNameResolver) {
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ClientFrontierServiceImpl();
        this.collections = new ClientCollectionServiceImpl();

        frontierEvents.subscribeCreated(this, (frontier, playerId) -> eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier, playerNameResolver))));
        frontierEvents.subscribeUpdated(this, (frontier, playerId) -> eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier, playerNameResolver))));
        frontierEvents.subscribeDeleted(this, frontierId -> eventBus.post(new FrontierDeletedEvent(new FrontierId(frontierId))));
        collectionEvents.subscribeCreated(this, collection -> eventBus.post(new CollectionCreatedEvent(ApiConverters.fromCollection(collection, playerNameResolver))));
        collectionEvents.subscribeUpdated(this, collection -> eventBus.post(new CollectionUpdatedEvent(ApiConverters.fromCollection(collection, playerNameResolver))));
        collectionEvents.subscribeDeleted(this, collectionId -> eventBus.post(new CollectionDeletedEvent(new CollectionId(collectionId))));
    }

    public void close() {
        frontierEvents.unsubscribe(this);
        collectionEvents.unsubscribe(this);
    }

    @Override
    public PluginScopedClientFrontierService frontiers() {
        return frontiers;
    }

    @Override
    public PluginScopedClientCollectionService collections() {
        return collections;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }
}
