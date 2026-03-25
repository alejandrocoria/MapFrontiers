package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketCreateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ClientFrontierOperationService {
    private static final Minecraft minecraft = Minecraft.getInstance();

    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientLocalPersonalFrontierStore localPersonalStore;
    private final ClientFrontierEvents frontierEvents;

    private static class SharingActionContext {
        private final @Nullable FrontierOverlay frontier;
        private final @Nullable FrontierActionResult failure;

        private SharingActionContext(@Nullable FrontierOverlay frontier, @Nullable FrontierActionResult failure) {
            this.frontier = frontier;
            this.failure = failure;
        }
    }

    public ClientFrontierOperationService(FrontiersOverlayManager globalManager,
                                          FrontiersOverlayManager personalManager,
                                          ClientLocalPersonalFrontierStore localPersonalStore,
                                          ClientFrontierEvents frontierEvents) {
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.localPersonalStore = localPersonalStore;
        this.frontierEvents = frontierEvents;
    }

    public void createNewFrontier(boolean personal, ResourceKey<Level> dimension,
                                  @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        createNewFrontierAndReturn(personal, UUID.randomUUID(), dimension, null, vertices, chunks);
    }

    @Nullable
    public FrontierOverlay createNewFrontierAndReturn(boolean personal, UUID frontierId, ResourceKey<Level> dimension,
                                                      @Nullable String sourcePluginId, FrontierShape shape) {
        List<Point2i> shapeVertices = shape.vertices();
        List<ChunkCoord> shapeChunks = shape.chunks();
        List<BlockPos> vertices = shapeVertices == null || shapeVertices.isEmpty() ? null : shapeVertices.stream()
                .map(vertex -> new BlockPos(vertex.x(), 0, vertex.z())).toList();
        List<ChunkPos> chunks = shapeChunks == null || shapeChunks.isEmpty() ? null : shapeChunks.stream()
                .map(chunk -> new ChunkPos(chunk.x(), chunk.z())).toList();
        return createNewFrontierAndReturn(personal, frontierId, dimension, sourcePluginId, vertices, chunks);
    }

    @Nullable
    public FrontierOverlay createNewFrontierAndReturn(boolean personal, UUID frontierId, ResourceKey<Level> dimension,
                                                      @Nullable String sourcePluginId, @Nullable List<BlockPos> vertices,
                                                      @Nullable List<ChunkPos> chunks) {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketCreateFrontier(frontierId, dimension, personal, sourcePluginId, vertices, chunks));
            return null;
        }

        if (!personal || minecraft.player == null) {
            return null;
        }

        FrontierData frontier = FrontierCreationFactory.createFrontier(frontierId, new SettingsUser(minecraft.player), dimension,
                true, sourcePluginId, vertices, chunks);
        FrontierOverlay frontierOverlay = personalManager.addFrontier(frontier);
        persistLocalPersonalFrontiers();
        frontierEvents.postCreated(frontierOverlay, minecraft.player.getId());
        return frontierOverlay;
    }

    public void deleteFrontier(FrontierOverlay frontier) {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketDeleteFrontier(frontier.getId()));
            return;
        }

        if (!frontier.getPersonal() || minecraft.player == null || !frontier.getOwner().equals(new SettingsUser(minecraft.player))) {
            return;
        }

        personalManager.deleteFrontier(frontier.getDimension(), frontier.getId());
        persistLocalPersonalFrontiers();
        frontierEvents.postDeleted(frontier.getId());
    }

    public void updateFrontier(FrontierOverlay frontier) {
        updateFrontier(frontier, FrontierChange.fromFrontierData(frontier));
    }

    public void updateFrontier(FrontierOverlay frontier, FrontierChange change) {
        if (change.isEmpty()) {
            return;
        }

        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier.getId(), change));
            return;
        }

        if (!frontier.getPersonal() || minecraft.player == null || !frontier.getOwner().equals(new SettingsUser(minecraft.player))) {
            return;
        }

        persistLocalPersonalFrontiers();
        frontierEvents.postUpdated(frontier, minecraft.player.getId());
    }

    public void shareFrontier(UUID frontierId, SettingsUser targetUser) {
        if (!MapFrontiersClient.isModOnServer()) {
            return;
        }

        SettingsUserShared sharedUser = new SettingsUserShared(targetUser, false);
        sharedUser.setActions(EnumSet.noneOf(SettingsUserShared.Action.class));
        PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId, sharedUser));
    }

    public FrontierActionResult createFrontierAction(boolean personal, String pluginModId, DimensionId dimension, FrontierShape shape) {
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierId frontierId = new FrontierId(UUID.randomUUID());
        FrontierOverlay frontier = createNewFrontierAndReturn(personal, frontierId.value(), resourceKey, pluginModId, shape);
        if (frontier == null) {
            return MapFrontiersClient.isModOnServer() ? FrontierActionResult.acceptedAsync(frontierId) : FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    public Optional<FrontierDataView> getFrontierAction(FrontierId frontierId) {
        FrontierOverlay frontier = personalManager.getFrontier(frontierId.value());
        if (frontier == null) {
            frontier = globalManager.getFrontier(frontierId.value());
        }

        return frontier == null ? Optional.empty() : Optional.of(ApiConverters.fromFrontier(frontier));
    }

    public List<FrontierDataView> listFrontiersAction(boolean personal, DimensionId dimension) {
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        return getManager(personal).getAllFrontiers(resourceKey).stream().map(ApiConverters::fromFrontier).toList();
    }

    public FrontierActionResult updateFrontierAction(boolean personal, FrontierId frontierId, FrontierMutation mutation) {
        FrontierOverlay frontier = getManager(personal).getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() != personal) {
            return FrontierActionResult.notFound(frontierId);
        }

        if (MapFrontiersClient.isModOnServer()) {
            FrontierData payload = new FrontierData(frontier);
            ApiConverters.applyMutation(payload, mutation);
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontierId.value(), FrontierChange.fromFrontierData(payload)));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        ApiConverters.applyMutation(frontier, mutation);
        updateFrontier(frontier);
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    public FrontierActionResult deleteFrontierAction(boolean personal, FrontierId frontierId) {
        FrontierOverlay frontier = getManager(personal).getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() != personal) {
            return FrontierActionResult.notFound(frontierId);
        }

        deleteFrontier(frontier);
        if (MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    public FrontierActionResult changeToGlobalAction(FrontierId frontierId) {
        FrontierOverlay frontier = personalManager.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToGlobal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public FrontierActionResult changeToPersonalAction(FrontierId frontierId) {
        FrontierOverlay frontier = globalManager.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToPersonal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public FrontierActionResult sharePersonalFrontierAction(String pluginModId, FrontierId frontierId, UserRef user,
                                                            Set<FrontierSharePermission> permissions) {
        SharingActionContext context = resolveSharingActionContext("sharePersonalFrontier",
                "Could not share personal frontier because it was not found locally or is not personal.",
                pluginModId, frontierId, user);
        if (context.failure != null) {
            return context.failure;
        }

        PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId.value(), createSharedUser(user, permissions)));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public FrontierActionResult updateSharedUserPermissionsAction(String pluginModId, FrontierId frontierId, UserRef user,
                                                                 Set<FrontierSharePermission> permissions) {
        SharingActionContext context = resolveSharingActionContext("updateSharedUserPermissions",
                "Could not update shared user permissions because frontier was not found locally or is not personal.",
                pluginModId, frontierId, user);
        if (context.failure != null) {
            return context.failure;
        }

        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId.value(), createSharedUser(user, permissions)));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public FrontierActionResult updateSharedUserPermissionsPartialAction(String pluginModId, FrontierId frontierId, UserRef user,
                                                                        Set<FrontierSharePermission> permissionsToAdd,
                                                                        Set<FrontierSharePermission> permissionsToRemove) {
        SharingActionContext context = resolveSharingActionContext("partial shared-permission update",
                "Could not partially update shared user permissions because frontier was not found locally or is not personal.",
                pluginModId, frontierId, user);
        if (context.failure != null) {
            return context.failure;
        }

        SettingsUserShared currentSharedUser = context.frontier.getUserShared(ApiConverters.toUser(user));
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

        return updateSharedUserPermissionsAction(pluginModId, frontierId, user, permissions);
    }

    public FrontierActionResult removeSharedUserAction(String pluginModId, FrontierId frontierId, UserRef user) {
        SharingActionContext context = resolveSharingActionContext("removeSharedUser",
                "Could not remove shared user because frontier was not found locally or is not personal.",
                pluginModId, frontierId, user);
        if (context.failure != null) {
            return context.failure;
        }

        PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId.value(), ApiConverters.toUser(user)));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public void removeSharedUser(UUID frontierId, SettingsUser user) {
        PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId, user));
    }

    public void updateSharedUser(UUID frontierId, SettingsUserShared userShared) {
        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId, userShared));
    }

    public FrontierOverlay acceptCopiedFrontier(FrontierData receivedFrontier, @Nullable FrontierOverlay currentFrontier) {
        if (currentFrontier != null && minecraft.player != null) {
            currentFrontier.removeCopiedFromInfo();
            frontierEvents.postUpdated(currentFrontier, minecraft.player.getId());
        }

        FrontierOverlay frontierOverlay = personalManager.addFrontier(receivedFrontier);
        persistLocalPersonalFrontiers();
        if (minecraft.player != null) {
            frontierEvents.postCreated(frontierOverlay, minecraft.player.getId());
        }
        return frontierOverlay;
    }

    public FrontierOverlay acceptCopiedFrontierAndReplace(FrontierData receivedFrontier, FrontierOverlay currentFrontier) {
        personalManager.deleteFrontier(currentFrontier.getDimension(), currentFrontier.getId());
        frontierEvents.postDeleted(currentFrontier.getId());

        FrontierOverlay frontierOverlay = personalManager.addFrontier(receivedFrontier);
        persistLocalPersonalFrontiers();
        if (minecraft.player != null) {
            frontierEvents.postCreated(frontierOverlay, minecraft.player.getId());
        }
        return frontierOverlay;
    }

    public void applyFrontierCreated(FrontierData frontier, int playerId) {
        FrontierOverlay frontierOverlay = getManager(frontier.getPersonal()).addFrontier(frontier);
        if (frontier.getPersonal()) {
            persistLocalPersonalFrontiers();
        }
        frontierEvents.postCreated(frontierOverlay, playerId);
    }

    public void applyFrontierUpdated(ResourceKey<Level> dimension,
                                     UUID frontierId,
                                     boolean personal,
                                     FrontierChange change,
                                     int playerId) {
        FrontierOverlay frontierOverlay = getManager(personal).applyFrontierChange(dimension, frontierId, change);
        if (frontierOverlay != null) {
            if (personal) {
                persistLocalPersonalFrontiers();
            }
            frontierEvents.postUpdated(frontierOverlay, playerId);
        }
    }

    public void applyFrontierSharingUpdated(ResourceKey<Level> dimension,
                                            UUID frontierId,
                                            FrontierSharingChange sharingChange,
                                            int playerId) {
        FrontierOverlay updatedFrontier = personalManager.applyFrontierSharingChange(dimension, frontierId, sharingChange);
        if (updatedFrontier != null) {
            persistLocalPersonalFrontiers();
            frontierEvents.postUpdated(updatedFrontier, playerId);
        }
    }

    public void applyFrontierDeleted(ResourceKey<Level> dimension, UUID frontierId, boolean personal) {
        boolean deleted = getManager(personal).deleteFrontier(dimension, frontierId) != null;
        if (deleted) {
            if (personal) {
                persistLocalPersonalFrontiers();
            }
            frontierEvents.postDeleted(frontierId);
        }
    }

    public void applyFrontierChangeToGlobal(UUID frontierId, @Nullable Date modified) {
        FrontierOverlay frontierOverlay = personalManager.deleteFrontier(frontierId);
        if (frontierOverlay == null) {
            return;
        }
        frontierOverlay.setPersonal(false);
        if (modified != null) {
            frontierOverlay.setModified(modified);
        }
        frontierOverlay.removeAllUserShared();
        frontierOverlay.recreateBannerRenderer();
        globalManager.addFrontier(frontierOverlay);
        persistLocalPersonalFrontiers();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.updateOverlay();
    }

    public void applyFrontierChangeToPersonal(UUID frontierId, @Nullable Date modified) {
        FrontierOverlay frontierOverlay = globalManager.deleteFrontier(frontierId);
        if (frontierOverlay == null) {
            return;
        }
        frontierOverlay.setPersonal(true);
        if (modified != null) {
            frontierOverlay.setModified(modified);
        }
        frontierOverlay.setCurrentPlayerAsOwner();
        frontierOverlay.recreateBannerRenderer();
        personalManager.addFrontier(frontierOverlay);
        persistLocalPersonalFrontiers();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.updateOverlay();
    }

    public void notifyLocalFrontierUpdated(FrontierOverlay frontierOverlay) {
        frontierEvents.postUpdated(frontierOverlay, -1);
    }

    private void persistLocalPersonalFrontiers() {
        if (minecraft.isLocalServer() || minecraft.player == null) {
            return;
        }

        localPersonalStore.saveOwnedFrontierMirror(getAllPersonalFrontiers(), new SettingsUser(minecraft.player));
    }

    private Collection<FrontierOverlay> getAllPersonalFrontiers() {
        return personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private FrontiersOverlayManager getManager(boolean personal) {
        return personal ? personalManager : globalManager;
    }

    private SharingActionContext resolveSharingActionContext(String operationName,
                                                             String missingFrontierMessage,
                                                             String pluginModId,
                                                             FrontierId frontierId,
                                                             UserRef user) {
        if (!MapFrontiersClient.isModOnServer()) {
            MapFrontiers.LOGGER.debug("Rejected {} because MapFrontiers is not present on the server. pluginModId={}, frontierId={}, targetUser={}",
                    operationName, pluginModId, frontierId.value(), user.name());
            return new SharingActionContext(null, FrontierActionResult.rejected());
        }

        FrontierOverlay frontier = personalManager.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            MapFrontiers.LOGGER.debug("{} pluginModId={}, frontierId={}, targetUser={}",
                    missingFrontierMessage, pluginModId, frontierId.value(), user.name());
            return new SharingActionContext(null, FrontierActionResult.notFound(frontierId));
        }

        return new SharingActionContext(frontier, null);
    }

    private static SettingsUserShared createSharedUser(UserRef user, @Nullable Set<FrontierSharePermission> permissions) {
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
