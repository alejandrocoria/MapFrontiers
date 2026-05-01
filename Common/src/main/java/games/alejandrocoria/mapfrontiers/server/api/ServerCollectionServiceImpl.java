package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerCollectionService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;

public class ServerCollectionServiceImpl implements PluginScopedServerCollectionService {
    @Override
    public CollectionDataView createGlobalCollection(String pluginModId, UserRef owner, CollectionCreateRequest request) {
        throw new UnsupportedOperationException("Server collection API service is not implemented yet.");
    }

    @Override
    public Optional<CollectionDataView> updateGlobalCollection(String pluginModId, CollectionId collectionId, CollectionMutation mutation) {
        return Optional.empty();
    }

    @Override
    public boolean deleteGlobalCollection(String pluginModId, CollectionId collectionId) {
        return false;
    }

    @Override
    public List<CollectionDataView> listGlobalCollections(String pluginModId) {
        return List.of();
    }

    @Override
    public Optional<CollectionDataView> getCollection(String pluginModId, CollectionId collectionId) {
        return Optional.empty();
    }
}
