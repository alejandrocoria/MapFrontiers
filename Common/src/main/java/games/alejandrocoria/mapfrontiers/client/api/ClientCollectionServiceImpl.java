package games.alejandrocoria.mapfrontiers.client.api;

import games.alejandrocoria.mapfrontiers.api.client.CollectionActionResult;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientCollectionService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;

import java.util.List;
import java.util.Optional;

public class ClientCollectionServiceImpl implements PluginScopedClientCollectionService {
    @Override
    public Optional<CollectionDataView> getCollection(String pluginModId, CollectionId collectionId) {
        return MapFrontiersClient.getOperationService().getCollectionAction(collectionId);
    }

    @Override
    public List<CollectionDataView> listGlobalCollections(String pluginModId) {
        return MapFrontiersClient.getOperationService().listCollectionsAction(false);
    }

    @Override
    public List<CollectionDataView> listPersonalCollections(String pluginModId) {
        return MapFrontiersClient.getOperationService().listCollectionsAction(true);
    }

    @Override
    public CollectionActionResult createGlobalCollection(String pluginModId, CollectionCreateRequest request) {
        return MapFrontiersClient.getOperationService().createCollectionAction(false, pluginModId, request);
    }

    @Override
    public CollectionActionResult createPersonalCollection(String pluginModId, CollectionCreateRequest request) {
        return MapFrontiersClient.getOperationService().createCollectionAction(true, pluginModId, request);
    }

    @Override
    public CollectionActionResult createTemporaryPersonalCollection(String pluginModId, CollectionCreateRequest request) {
        return MapFrontiersClient.getOperationService().createTemporaryPersonalCollectionAction(pluginModId, request);
    }

    @Override
    public CollectionActionResult updateGlobalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        return MapFrontiersClient.getOperationService().updateCollectionAction(false, collectionId, mutation);
    }

    @Override
    public CollectionActionResult updatePersonalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        return MapFrontiersClient.getOperationService().updateCollectionAction(true, collectionId, mutation);
    }

    @Override
    public CollectionActionResult deleteGlobalCollection(String pluginModId, CollectionId collectionId) {
        return MapFrontiersClient.getOperationService().deleteCollectionAction(false, collectionId);
    }

    @Override
    public CollectionActionResult deletePersonalCollection(String pluginModId, CollectionId collectionId) {
        return MapFrontiersClient.getOperationService().deleteCollectionAction(true, collectionId);
    }
}
