package games.alejandrocoria.mapfrontiers.client.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierLifetime;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ClientFrontierServiceImpl implements PluginScopedClientFrontierService {
    @Override
    public FrontierActionResult createPersonalFrontier(String pluginModId, FrontierCreateRequest request) {
        return MapFrontiersClient.getOperationService().createFrontierAction(true, pluginModId, request);
    }

    @Override
    public FrontierActionResult createTemporaryPersonalFrontier(String pluginModId, FrontierCreateRequest request) {
        return createFrontier(pluginModId, request, true, FrontierLifetime.SESSION_ONLY);
    }

    @Override
    public FrontierActionResult createGlobalFrontier(String pluginModId, FrontierCreateRequest request) {
        return MapFrontiersClient.getOperationService().createFrontierAction(false, pluginModId, request);
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getOperationService().getFrontierAction(frontierId);
    }

    @Override
    public FrontierActionResult updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        return MapFrontiersClient.getOperationService().updateFrontierAction(false, frontierId, mutation);
    }

    @Override
    public FrontierActionResult deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getOperationService().deleteFrontierAction(false, frontierId);
    }

    @Override
    public FrontierActionResult updatePersonalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        return MapFrontiersClient.getOperationService().updateFrontierAction(true, frontierId, mutation);
    }

    @Override
    public FrontierActionResult deletePersonalFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getOperationService().deleteFrontierAction(true, frontierId);
    }

    @Override
    public List<FrontierDataView> listPersonalFrontiers(String pluginModId, DimensionId dimension) {
        return MapFrontiersClient.getOperationService().listFrontiersAction(true, dimension);
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        return MapFrontiersClient.getOperationService().listFrontiersAction(false, dimension);
    }

    @Override
    public FrontierActionResult changeToGlobal(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getOperationService().changeToGlobalAction(frontierId);
    }

    @Override
    public FrontierActionResult changeToPersonal(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getOperationService().changeToPersonalAction(frontierId);
    }

    @Override
    public FrontierActionResult sharePersonalFrontier(String pluginModId, FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions) {
        return MapFrontiersClient.getOperationService().sharePersonalFrontierAction(pluginModId, frontierId, user, permissions);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId, FrontierId frontierId, UserRef user,
                                                            Set<FrontierSharePermission> permissions) {
        return MapFrontiersClient.getOperationService().updateSharedUserPermissionsAction(pluginModId, frontierId, user, permissions);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId, FrontierId frontierId, UserRef user,
                                                            Set<FrontierSharePermission> permissionsToAdd,
                                                            Set<FrontierSharePermission> permissionsToRemove) {
        return MapFrontiersClient.getOperationService().updateSharedUserPermissionsPartialAction(pluginModId, frontierId, user,
                permissionsToAdd, permissionsToRemove);
    }

    @Override
    public FrontierActionResult removeSharedUser(String pluginModId, FrontierId frontierId, UserRef user) {
        return MapFrontiersClient.getOperationService().removeSharedUserAction(pluginModId, frontierId, user);
    }

    private FrontierActionResult createFrontier(String pluginModId, FrontierCreateRequest request, boolean personal, FrontierLifetime lifetime) {
        if (hasUnsupportedCreateFields(request)) {
            MapFrontiers.LOGGER.debug("Rejected frontier create request because enriched create fields are not implemented yet. pluginModId={}, personal={}, request={}",
                    pluginModId, personal, request);
            return FrontierActionResult.rejected();
        }

        return MapFrontiersClient.getOperationService().createFrontierAction(personal, pluginModId, request.dimension(), request.shape(), lifetime);
    }

    private static boolean hasUnsupportedCreateFields(FrontierCreateRequest request) {
        return request.collectionId().isPresent()
                || request.name1().isPresent()
                || request.name2().isPresent()
                || request.color().isPresent()
                || request.visibility().isPresent()
                || request.banner().isPresent()
                || request.pathStyle().isPresent();
    }
}
