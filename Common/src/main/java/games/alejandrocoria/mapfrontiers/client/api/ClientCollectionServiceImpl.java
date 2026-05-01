package games.alejandrocoria.mapfrontiers.client.api;

import games.alejandrocoria.mapfrontiers.api.client.CollectionActionResult;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientCollectionService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;

import java.util.List;
import java.util.Optional;

public class ClientCollectionServiceImpl implements PluginScopedClientCollectionService {
    @Override
    public Optional<CollectionDataView> getCollection(String pluginModId, CollectionId collectionId) {
        return Optional.empty();
    }

    @Override
    public List<CollectionDataView> listGlobalCollections(String pluginModId) {
        return List.of();
    }

    @Override
    public List<CollectionDataView> listPersonalCollections(String pluginModId) {
        return List.of();
    }

    @Override
    public CollectionActionResult createGlobalCollection(String pluginModId, CollectionCreateRequest request) {
        return CollectionActionResult.rejected();
    }

    @Override
    public CollectionActionResult createPersonalCollection(String pluginModId, CollectionCreateRequest request) {
        return CollectionActionResult.rejected();
    }

    @Override
    public CollectionActionResult updateGlobalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        return CollectionActionResult.rejected();
    }

    @Override
    public CollectionActionResult updatePersonalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        return CollectionActionResult.rejected();
    }

    @Override
    public CollectionActionResult deleteGlobalCollection(String pluginModId, CollectionId collectionId) {
        return CollectionActionResult.rejected();
    }

    @Override
    public CollectionActionResult deletePersonalCollection(String pluginModId, CollectionId collectionId) {
        return CollectionActionResult.rejected();
    }
}
