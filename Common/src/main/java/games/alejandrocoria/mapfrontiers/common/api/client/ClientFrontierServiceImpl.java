package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ClientFrontierServiceImpl implements PluginScopedClientFrontierService {
    public ClientFrontierServiceImpl() {
    }

    @Override
    public FrontierActionResult createPersonalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierId frontierId = new FrontierId(UUID.randomUUID());
        FrontierOverlay frontier = manager.clientCreateNewFrontierAndReturn(frontierId.value(), resourceKey, pluginModId, shape);
        if (frontier == null) {
            if (MapFrontiersClient.isModOnServer()) {
                return FrontierActionResult.acceptedAsync(frontierId);
            }
            return FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult createGlobalFrontier(String pluginModId, DimensionId dimension, FrontierShape shape) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierId frontierId = new FrontierId(UUID.randomUUID());
        FrontierOverlay frontier = manager.clientCreateNewFrontierAndReturn(frontierId.value(), resourceKey, pluginModId, shape);
        if (frontier == null) {
            if (MapFrontiersClient.isModOnServer()) {
                return FrontierActionResult.acceptedAsync(frontierId);
            }
            return FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null) {
            frontier = MapFrontiersClient.getFrontiersOverlayManager(false).getFrontier(frontierId.value());
        }

        return frontier == null ? Optional.empty() : Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return updateFrontierInManager(frontierId, mutation, manager, frontier);
    }

    @Override
    public FrontierActionResult deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return deleteFrontierInManager(frontierId, manager, frontier);
    }

    @Override
    public FrontierActionResult updatePersonalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return updateFrontierInManager(frontierId, mutation, manager, frontier);
    }

    @Override
    public FrontierActionResult deletePersonalFrontier(String pluginModId, FrontierId frontierId) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return deleteFrontierInManager(frontierId, manager, frontier);
    }

    @Override
    public List<FrontierDataView> listPersonalFrontiers(String pluginModId, DimensionId dimension) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        return manager.getAllFrontiers(resourceKey).stream().map(ApiConverters::fromFrontier).toList();
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        return manager.getAllFrontiers(resourceKey).stream().map(ApiConverters::fromFrontier).toList();
    }

    @Override
    public FrontierActionResult changeToGlobal(String pluginModId, FrontierId frontierId) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToGlobal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult changeToPersonal(String pluginModId, FrontierId frontierId) {
        FrontiersOverlayManager global = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = global.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToPersonal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult sharePersonalFrontier(String pluginModId, FrontierId frontierId, UserRef user, Set<FrontierSharePermission> permissions) {
        if (!MapFrontiersClient.isModOnServer()) {
            MapFrontiers.LOGGER.debug("Rejected sharePersonalFrontier because MapFrontiers is not present on the server. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            MapFrontiers.LOGGER.debug("Could not share personal frontier because it was not found locally or is not personal. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.notFound(frontierId);
        }

        PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId.value(), createSharedUser(user, permissions)));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId,
                                                            FrontierId frontierId,
                                                            UserRef user,
                                                            Set<FrontierSharePermission> permissions) {
        if (!MapFrontiersClient.isModOnServer()) {
            MapFrontiers.LOGGER.debug("Rejected updateSharedUserPermissions because MapFrontiers is not present on the server. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            MapFrontiers.LOGGER.debug("Could not update shared user permissions because frontier was not found locally or is not personal. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.notFound(frontierId);
        }

        SettingsUserShared updatedSharedUser = createSharedUser(user, permissions);
        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId.value(), updatedSharedUser));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult updateSharedUserPermissions(String pluginModId,
                                                            FrontierId frontierId,
                                                            UserRef user,
                                                            Set<FrontierSharePermission> permissionsToAdd,
                                                            Set<FrontierSharePermission> permissionsToRemove) {
        return updateSharedUserPermissionsPartial(pluginModId, frontierId, user, permissionsToAdd, permissionsToRemove);
    }

    private FrontierActionResult updateSharedUserPermissionsPartial(String pluginModId,
                                                                    FrontierId frontierId,
                                                                    UserRef user,
                                                                    Set<FrontierSharePermission> permissionsToAdd,
                                                                    Set<FrontierSharePermission> permissionsToRemove) {
        if (!MapFrontiersClient.isModOnServer()) {
            MapFrontiers.LOGGER.debug("Rejected partial shared-permission update because MapFrontiers is not present on the server. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            MapFrontiers.LOGGER.debug("Could not partially update shared user permissions because frontier was not found locally or is not personal. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.notFound(frontierId);
        }

        SettingsUserShared currentSharedUser = frontier.getUserShared(ApiConverters.toUser(user));
        if (currentSharedUser == null) {
            MapFrontiers.LOGGER.debug("Rejected partial shared-permission update because target user is not currently shared. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        EnumSet<FrontierSharePermission> permissions = EnumSet.noneOf(FrontierSharePermission.class);
        for (SettingsUserShared.Action action : currentSharedUser.getActions()) {
            permissions.add(FrontierSharePermission.valueOf(action.name()));
        }
        if (permissionsToAdd != null) {
            permissions.addAll(permissionsToAdd);
        }
        if (permissionsToRemove != null) {
            permissions.removeAll(permissionsToRemove);
        }

        return updateSharedUserPermissions(pluginModId, frontierId, user, permissions);
    }

    @Override
    public FrontierActionResult removeSharedUser(String pluginModId, FrontierId frontierId, UserRef user) {
        if (!MapFrontiersClient.isModOnServer()) {
            MapFrontiers.LOGGER.debug("Rejected removeSharedUser because MapFrontiers is not present on the server. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            MapFrontiers.LOGGER.debug("Could not remove shared user because frontier was not found locally or is not personal. pluginModId={}, frontierId={}, targetUser={}",
                    pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.notFound(frontierId);
        }

        var targetUser = ApiConverters.toUser(user);
        PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId.value(), targetUser));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    private FrontierActionResult updateFrontierInManager(FrontierId frontierId,
                                                         FrontierMutation mutation,
                                                         FrontiersOverlayManager manager,
                                                         FrontierOverlay frontier) {
        if (MapFrontiersClient.isModOnServer()) {
            FrontierData payload = new FrontierData(frontier);
            ApiConverters.applyMutation(payload, mutation);
            PacketHandler.sendToServer(new PacketUpdateFrontier(payload));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        ApiConverters.applyMutation(frontier, mutation);
        manager.clientUpdateFrontier(frontier);
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    private FrontierActionResult deleteFrontierInManager(FrontierId frontierId,
                                                         FrontiersOverlayManager manager,
                                                         FrontierOverlay frontier) {
        manager.clientDeleteFrontier(frontier);
        if (MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.acceptedAsync(frontierId);
        }
        FrontierDataView deleted = ApiConverters.fromFrontier(frontier);
        return FrontierActionResult.applied(deleted);
    }

    private static SettingsUserShared createSharedUser(UserRef user, Set<FrontierSharePermission> permissions) {
        SettingsUserShared sharedUser = new SettingsUserShared(ApiConverters.toUser(user), false);
        EnumSet<SettingsUserShared.Action> actions = EnumSet.noneOf(SettingsUserShared.Action.class);
        if (permissions != null) {
            for (FrontierSharePermission permission : permissions) {
                actions.add(SettingsUserShared.Action.valueOf(permission.name()));
            }
        }
        sharedUser.setActions(actions);
        return sharedUser;
    }
}
