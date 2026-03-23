package games.alejandrocoria.mapfrontiers.api.internal;

import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
public interface PluginScopedClientFrontierService {
    Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId);
    FrontierActionResult createGlobalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape);
    FrontierActionResult updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation);
    FrontierActionResult deleteGlobalFrontier(String pluginModId, FrontierId frontierId);
    FrontierActionResult changeToPersonal(String pluginModId, FrontierId frontierId);
    List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension);
    FrontierActionResult createPersonalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape);
    FrontierActionResult updatePersonalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation);
    FrontierActionResult deletePersonalFrontier(String pluginModId, FrontierId frontierId);
    FrontierActionResult changeToGlobal(String pluginModId, FrontierId frontierId);
    List<FrontierDataView> listPersonalFrontiers(String pluginModId, DimensionId dimension);
    FrontierActionResult sharePersonalFrontier(String pluginModId, FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions);
    FrontierActionResult updateSharedUserPermissions(String pluginModId, FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions);
    FrontierActionResult updateSharedUserPermissions(String pluginModId,
                                                     FrontierId frontierId,
                                                     UserRef user,
                                                     Set<FrontierSharePermission> permissionsToAdd,
                                                     Set<FrontierSharePermission> permissionsToRemove);
    FrontierActionResult removeSharedUser(String pluginModId, FrontierId frontierId, UserRef user);
}
