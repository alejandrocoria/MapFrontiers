package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.api.event.CollectionCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.CollectionUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.EventBus;
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
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationService;
import games.alejandrocoria.mapfrontiers.server.territory.collection.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierEvents;

public class MapFrontiersServerAPIImpl implements InternalMapFrontiersServerAPI {
    private final PluginScopedServerFrontierService frontiers;
    private final PluginScopedServerCollectionService collections;
    private final SimpleEventBus eventBus;
    private final ServerFrontierEvents frontierEvents;
    private final ServerCollectionEvents collectionEvents;

    public MapFrontiersServerAPIImpl(ServerTerritoryOperationService operationService, ServerFrontierEvents frontierEvents,
                                    ServerCollectionEvents collectionEvents, PlayerNameRepository playerNameRepository) {
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
        this.eventBus = new SimpleEventBus();
        this.frontiers = new ServerFrontierServiceImpl(operationService, playerNameRepository);
        this.collections = new ServerCollectionServiceImpl(operationService, playerNameRepository);

        // Server API exposes only global territories, so personal/global conversions surface as created/deleted here.
        frontierEvents.subscribeCreated(this, frontier -> {
            if (!frontier.getPersonal() && frontier.isPersistent()) {
                eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(frontier, playerNameRepository)));
            }
        });
        frontierEvents.subscribeUpdated(this, frontier -> {
            if (!frontier.getPersonal() && frontier.isPersistent()) {
                eventBus.post(new FrontierUpdatedEvent(ApiConverters.fromFrontier(frontier, playerNameRepository)));
            }
        });
        frontierEvents.subscribeDeleted(this, frontier -> {
            if (!frontier.getPersonal() && frontier.isPersistent()) {
                eventBus.post(new FrontierDeletedEvent(new FrontierId(frontier.getId())));
            }
        });
        collectionEvents.subscribeCreated(this, collection -> {
            if (!collection.getPersonal() && collection.isPersistent()) {
                eventBus.post(new CollectionCreatedEvent(ApiConverters.fromCollection(collection, playerNameRepository)));
            }
        });
        collectionEvents.subscribeUpdated(this, collection -> {
            if (!collection.getPersonal() && collection.isPersistent()) {
                eventBus.post(new CollectionUpdatedEvent(ApiConverters.fromCollection(collection, playerNameRepository)));
            }
        });
        collectionEvents.subscribeDeleted(this, collection -> {
            if (!collection.getPersonal() && collection.isPersistent()) {
                eventBus.post(new CollectionDeletedEvent(new CollectionId(collection.getId())));
            }
        });
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
