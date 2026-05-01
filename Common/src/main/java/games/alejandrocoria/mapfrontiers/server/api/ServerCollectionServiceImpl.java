package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerCollectionService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerCollectionServiceImpl implements PluginScopedServerCollectionService {
    private final ServerFrontierOperationService operationService;
    private final ServerCollectionEvents collectionEvents;

    public ServerCollectionServiceImpl(ServerFrontierOperationService operationService, ServerCollectionEvents collectionEvents) {
        this.operationService = operationService;
        this.collectionEvents = collectionEvents;
    }

    @Override
    public CollectionDataView createGlobalCollection(String pluginModId, UserRef owner, CollectionCreateRequest request) {
        CollectionData collection = createCollectionData(pluginModId, owner, request);
        ServerFrontierOperationResult result = operationService.createGlobalCollection(collection);
        result.dispatchNetworkActions();
        MapFrontiers.LOGGER.info("Created global collection via server API. pluginModId={}, collectionId={}, owner={}",
                pluginModId, collection.getId(), collection.getOwner().username);
        collectionEvents.postCreated(collection);
        return ApiConverters.fromCollection(collection);
    }

    @Override
    public Optional<CollectionDataView> updateGlobalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal()) {
            return Optional.empty();
        }

        CollectionData payload = new CollectionData(collection);
        ApiConverters.applyCollectionMutation(payload, mutation);
        ServerFrontierOperationResult result = operationService.updateGlobalCollection(collectionId.value(), payload);
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        result.dispatchNetworkActions();
        collectionEvents.postUpdated(collection);
        return Optional.of(ApiConverters.fromCollection(collection));
    }

    @Override
    public boolean deleteGlobalCollection(String pluginModId, CollectionId collectionId) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal()) {
            return false;
        }

        ServerFrontierOperationResult result = operationService.deleteGlobalCollection(collectionId.value());
        if (!result.isSuccess()) {
            return false;
        }
        result.dispatchNetworkActions();
        collectionEvents.postDeleted(collectionId.value());
        return true;
    }

    @Override
    public List<CollectionDataView> listGlobalCollections(String pluginModId) {
        return operationService.getAllGlobalCollections().stream()
                .map(ApiConverters::fromCollection)
                .toList();
    }

    @Override
    public Optional<CollectionDataView> getCollection(String pluginModId, CollectionId collectionId) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromCollection(collection));
    }

    private static CollectionData createCollectionData(String pluginModId, UserRef owner, CollectionCreateRequest request) {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setPersonal(false);
        collection.setOwner(ApiConverters.toUser(owner));
        collection.setSourcePluginId(pluginModId);
        request.name().ifPresent(collection::setName);
        request.color().ifPresent(collection::setColor);
        Date now = new Date();
        collection.setCreated(now);
        collection.setModified(now);
        collection.removeCopiedFromInfo();
        return collection;
    }
}
