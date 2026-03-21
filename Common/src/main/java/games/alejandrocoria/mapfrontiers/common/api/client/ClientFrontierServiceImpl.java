package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ClientFrontierServiceImpl implements PluginScopedClientFrontierService {
    @Override
    public FrontierActionResult createPersonalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape) {
        return MapFrontiersClient.getCommandService().createFrontierAction(true, pluginModId, dimension, shape);
    }

    @Override
    public FrontierActionResult createGlobalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape) {
        return MapFrontiersClient.getCommandService().createFrontierAction(false, pluginModId, dimension, shape);
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getCommandService().getFrontierAction(frontierId);
    }

    @Override
    public FrontierActionResult updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        return MapFrontiersClient.getCommandService().updateFrontierAction(false, frontierId, mutation);
    }

    @Override
    public FrontierActionResult deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getCommandService().deleteFrontierAction(false, frontierId);
    }

    @Override
    public FrontierActionResult updatePersonalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        return MapFrontiersClient.getCommandService().updateFrontierAction(true, frontierId, mutation);
    }

    @Override
    public FrontierActionResult deletePersonalFrontier(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getCommandService().deleteFrontierAction(true, frontierId);
    }

    @Override
    public List<FrontierDataView> listPersonalFrontiers(String pluginModId, DimensionId dimension) {
        return MapFrontiersClient.getCommandService().listFrontiersAction(true, dimension);
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        return MapFrontiersClient.getCommandService().listFrontiersAction(false, dimension);
    }

    @Override
    public FrontierActionResult changeToGlobal(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getCommandService().changeToGlobalAction(frontierId);
    }

    @Override
    public FrontierActionResult changeToPersonal(String pluginModId, FrontierId frontierId) {
        return MapFrontiersClient.getCommandService().changeToPersonalAction(frontierId);
    }

    @Override
    public FrontierActionResult sharePersonalFrontier(String pluginModId, FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions) {
        return MapFrontiersClient.getCommandService().sharePersonalFrontierAction(pluginModId, frontierId, user, permissions);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId, FrontierId frontierId, UserRef user,
                                                            Set<FrontierSharePermission> permissions) {
        return MapFrontiersClient.getCommandService().updateSharedUserPermissionsAction(pluginModId, frontierId, user, permissions);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId, FrontierId frontierId, UserRef user,
                                                            Set<FrontierSharePermission> permissionsToAdd,
                                                            Set<FrontierSharePermission> permissionsToRemove) {
        return MapFrontiersClient.getCommandService().updateSharedUserPermissionsPartialAction(pluginModId, frontierId, user,
                permissionsToAdd, permissionsToRemove);
    }

    @Override
    public FrontierActionResult removeSharedUser(String pluginModId, FrontierId frontierId, UserRef user) {
        return MapFrontiersClient.getCommandService().removeSharedUserAction(pluginModId, frontierId, user);
    }
}
