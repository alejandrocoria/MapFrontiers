package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerCollectionService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.DefaultValuesProfile;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerCollectionServiceImpl implements PluginScopedServerCollectionService {
    private final ServerTerritoryOperationService operationService;

    public ServerCollectionServiceImpl(ServerTerritoryOperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public CollectionDataView createGlobalCollection(String pluginModId, UserRef owner, CollectionCreateRequest request) {
        CollectionData collection = createCollectionData(pluginModId, owner, request);
        ServerTerritoryOperationResult result = operationService.createGlobalCollection(collection);
        if (!result.isSuccess()) {
            throw new IllegalArgumentException("Invalid global collection create request");
        }
        result.dispatchNetworkActions();
        CollectionData storedCollection = result.getCollection();
        if (storedCollection == null) {
            throw new IllegalStateException("Global collection creation succeeded without returning the created collection");
        }
        MapFrontiers.LOGGER.info("Created global collection via server API. pluginModId={}, collectionId={}, owner={}",
                pluginModId, storedCollection.getId(), storedCollection.getOwner().username);
        return ApiConverters.fromCollection(storedCollection);
    }

    @Override
    public Optional<CollectionDataView> updateGlobalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal() || !collection.isPersistent()) {
            return Optional.empty();
        }

        CollectionData payload = new CollectionData(collection);
        ApiConverters.applyCollectionMutation(payload, mutation);
        ServerTerritoryOperationResult result = operationService.updateGlobalCollection(collectionId.value(), payload);
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        result.dispatchNetworkActions();
        CollectionData updatedCollection = result.getCollection();
        if (updatedCollection == null) {
            throw new IllegalStateException("Global collection update succeeded without returning the updated collection");
        }
        return Optional.of(ApiConverters.fromCollection(updatedCollection));
    }

    @Override
    public boolean deleteGlobalCollection(String pluginModId, CollectionId collectionId) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal() || !collection.isPersistent()) {
            return false;
        }

        ServerTerritoryOperationResult result = operationService.deleteGlobalCollection(collectionId.value());
        if (!result.isSuccess()) {
            return false;
        }
        result.dispatchNetworkActions();
        return true;
    }

    @Override
    public List<CollectionDataView> listGlobalCollections(String pluginModId) {
        List<CollectionDataView> collections = new java.util.ArrayList<>();
        for (CollectionData collection : operationService.iterateGlobalCollections()) {
            collections.add(ApiConverters.fromCollection(collection));
        }
        return List.copyOf(collections);
    }

    @Override
    public Optional<CollectionDataView> getCollection(String pluginModId, CollectionId collectionId) {
        CollectionData collection = operationService.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal() || !collection.isPersistent()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromCollection(collection));
    }

    private static CollectionData createCollectionData(String pluginModId, UserRef owner, CollectionCreateRequest request) {
        if (request.defaultValuesProfile() == DefaultValuesProfile.CONFIGURED) {
            throw new IllegalArgumentException("CONFIGURED defaults are not supported by the server API");
        }

        CollectionData defaults = new CollectionData();
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setPersonal(false);
        collection.setOwner(ApiConverters.toUser(owner));
        collection.setSourcePluginId(pluginModId);
        collection.setName(request.name().orElse(defaults.getName()));
        collection.setColor(request.color().orElseGet(ColorHelper::getRandomColor));
        CollectionVisibilityData visibility = request.visibility()
                .map(ApiConverters::toCollectionVisibility)
                .orElseGet(defaults::getVisibilityData);
        BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(defaults::getBannerData);
        collection.setVisibilityData(visibility);
        collection.setBannerData(banner);
        Date now = new Date();
        collection.setCreated(now);
        collection.removeCopiedFromInfo();
        return collection;
    }
}
