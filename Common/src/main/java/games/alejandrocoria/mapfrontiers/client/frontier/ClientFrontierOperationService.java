package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierLifetime;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
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
    private static final Minecraft mc = Minecraft.getInstance();

    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientCollectionRuntime collectionRuntime;
    private final ClientLocalPersonalFrontierStore localPersonalStore;
    private final ClientLocalPersonalCollectionStore localPersonalCollectionStore;
    private final ClientFrontierEvents frontierEvents;
    private final ClientCollectionEvents collectionEvents;

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
                                          ClientCollectionRuntime collectionRuntime,
                                          ClientLocalPersonalFrontierStore localPersonalStore,
                                          ClientLocalPersonalCollectionStore localPersonalCollectionStore,
                                          ClientFrontierEvents frontierEvents,
                                          ClientCollectionEvents collectionEvents) {
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.collectionRuntime = collectionRuntime;
        this.localPersonalStore = localPersonalStore;
        this.localPersonalCollectionStore = localPersonalCollectionStore;
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
    }

    public void createNewFrontier(boolean personal,
                                  ResourceKey<Level> dimension,
                                  @Nullable List<BlockPos> vertices,
                                  @Nullable List<ChunkPos> chunks,
                                  @Nullable List<BlockPos> points,
                                  @Nullable FrontierData.PathStyle pathStyle) {
        createNewFrontierAndReturn(personal, UUID.randomUUID(), dimension, FrontierData.FrontierLifetime.PERSISTENT, null,
                vertices, chunks, points, pathStyle);
    }

    @Nullable
    public FrontierOverlay createNewFrontierAndReturn(boolean personal, UUID frontierId, ResourceKey<Level> dimension,
                                                      FrontierData.FrontierLifetime lifetime, @Nullable String sourcePluginId, FrontierShape shape) {
        List<BlockPos> vertices = null;
        List<ChunkPos> chunks = null;
        List<BlockPos> points = null;

        switch (shape.type()) {
            case VERTEX -> {
                List<Point2i> shapeVertices = shape.vertices();
                vertices = shapeVertices == null ? List.of() : shapeVertices.stream()
                        .map(vertex -> new BlockPos(vertex.x(), 0, vertex.z())).toList();
            }
            case CHUNK -> {
                List<ChunkCoord> shapeChunks = shape.chunks();
                chunks = shapeChunks == null ? List.of() : shapeChunks.stream()
                        .map(chunk -> new ChunkPos(chunk.x(), chunk.z())).toList();
            }
            case PATH -> {
                List<Point2i> shapePoints = shape.points();
                points = shapePoints == null ? List.of() : shapePoints.stream()
                        .map(point -> new BlockPos(point.x(), 0, point.z())).toList();
            }
        }

        return createNewFrontierAndReturn(personal, frontierId, dimension, lifetime, sourcePluginId, vertices, chunks, points, null);
    }

    @Nullable
    public FrontierOverlay createNewFrontierAndReturn(boolean personal, UUID frontierId, ResourceKey<Level> dimension,
                                                      FrontierData.FrontierLifetime lifetime, @Nullable String sourcePluginId,
                                                      @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks,
                                                      @Nullable List<BlockPos> points, @Nullable FrontierData.PathStyle pathStyle) {
        if (usesAuthoritativeCreateFlow(lifetime)) {
            PacketHandler.sendToServer(new PacketCreateFrontier(frontierId, dimension, personal, null, sourcePluginId,
                    vertices, chunks, points, pathStyle));
            return null;
        }

        if (!personal || mc.player == null) {
            return null;
        }

        FrontierData frontier = FrontierCreationFactory.createFrontier(frontierId, new SettingsUser(mc.player), dimension,
                true, lifetime, sourcePluginId, vertices, chunks, points, pathStyle);
        FrontierOverlay frontierOverlay = personalManager.addFrontier(frontier);
        refreshCollectionRuntime();
        persistLocalPersonalDataIfPersistent(frontierOverlay);
        frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        return frontierOverlay;
    }

    public void deleteFrontier(FrontierOverlay frontier) {
        if (usesAuthoritativeMutationFlow(frontier)) {
            PacketHandler.sendToServer(new PacketDeleteFrontier(frontier.getId()));
            return;
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new SettingsUser(mc.player))) {
            return;
        }

        personalManager.deleteFrontier(frontier.getDimension(), frontier.getId());
        refreshCollectionRuntime();
        persistLocalPersonalDataIfPersistent(frontier);
        frontierEvents.postDeleted(frontier.getId());
    }

    public void updateFrontier(FrontierOverlay frontier) {
        updateFrontier(frontier, FrontierChange.fromFrontierData(frontier));
    }

    public void updateFrontier(FrontierOverlay frontier, FrontierChange change) {
        if (change.isEmpty()) {
            return;
        }

        MapFrontiersClient.markFrontierActivationDirty();

        if (usesAuthoritativeMutationFlow(frontier)) {
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier.getId(), change));
            return;
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new SettingsUser(mc.player))) {
            return;
        }

        frontier.applyChange(change);
        refreshCollectionRuntime();
        persistLocalPersonalDataIfPersistent(frontier);
        frontierEvents.postUpdated(frontier, mc.player.getId());
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
        return createFrontierAction(personal, pluginModId, dimension, shape, FrontierLifetime.PERSISTENT);
    }

    public FrontierActionResult createFrontierAction(boolean personal, String pluginModId, DimensionId dimension, FrontierShape shape,
                                                     @Nullable FrontierLifetime lifetime) {
        FrontierData.FrontierLifetime internalLifetime = ApiConverters.toLifetime(lifetime);
        if (!personal && internalLifetime == FrontierData.FrontierLifetime.SESSION_ONLY) {
            MapFrontiers.LOGGER.debug("Rejected frontier creation because SESSION_ONLY frontiers must be personal. pluginModId={}, dimension={}",
                    pluginModId, dimension.value());
            return FrontierActionResult.rejected();
        }

        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierId frontierId = new FrontierId(UUID.randomUUID());
        FrontierOverlay frontier = createNewFrontierAndReturn(personal, frontierId.value(), resourceKey, internalLifetime, pluginModId, shape);
        if (frontier == null) {
            return usesAuthoritativeCreateFlow(internalLifetime) ? FrontierActionResult.acceptedAsync(frontierId) : FrontierActionResult.rejected();
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

        if (usesAuthoritativeMutationFlow(frontier)) {
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

        boolean authoritativeDelete = usesAuthoritativeMutationFlow(frontier);
        deleteFrontier(frontier);
        if (authoritativeDelete) {
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    public FrontierActionResult changeToGlobalAction(FrontierId frontierId) {
        FrontierOverlay frontier = personalManager.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (frontier.isSessionOnly()) {
            return rejectSessionOnlyFrontierAction("changeToGlobal", frontierId, null, null);
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

    public FrontierOverlay acceptCopiedFrontier(FrontierData receivedFrontier,
                                                @Nullable CollectionData receivedCollection,
                                                @Nullable FrontierOverlay currentFrontier) {
        if (currentFrontier != null && mc.player != null) {
            currentFrontier.removeCopiedFromInfo();
            frontierEvents.postUpdated(currentFrontier, mc.player.getId());
        }

        FrontierOverlay frontierOverlay = personalManager.addFrontier(resolveCopiedFrontier(receivedFrontier, receivedCollection));
        refreshCollectionRuntime();
        persistLocalPersonalData();
        if (mc.player != null) {
            frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        }
        return frontierOverlay;
    }

    public FrontierOverlay acceptCopiedFrontierAndReplace(FrontierData receivedFrontier,
                                                          @Nullable CollectionData receivedCollection,
                                                          FrontierOverlay currentFrontier) {
        personalManager.deleteFrontier(currentFrontier.getDimension(), currentFrontier.getId());
        frontierEvents.postDeleted(currentFrontier.getId());

        FrontierOverlay frontierOverlay = personalManager.addFrontier(resolveCopiedFrontier(receivedFrontier, receivedCollection));
        refreshCollectionRuntime();
        if (currentFrontier.isPersistent() || frontierOverlay.isPersistent() || receivedCollection != null) {
            persistLocalPersonalData();
        }
        if (mc.player != null) {
            frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        }
        return frontierOverlay;
    }

    public void applyFrontierCreated(FrontierData frontier, int playerId) {
        FrontierOverlay frontierOverlay = getManager(frontier.getPersonal()).addFrontier(frontier);
        refreshCollectionRuntime();
        if (frontier.getPersonal() && frontier.isPersistent()) {
            persistLocalPersonalData();
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
            refreshCollectionRuntime();
            if (personal && frontierOverlay.isPersistent()) {
                persistLocalPersonalData();
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
            if (updatedFrontier.isPersistent()) {
                persistLocalPersonalData();
            }
            frontierEvents.postUpdated(updatedFrontier, playerId);
        }
    }

    public void applyFrontierDeleted(ResourceKey<Level> dimension, UUID frontierId, boolean personal) {
        FrontierOverlay deletedFrontier = getManager(personal).deleteFrontier(dimension, frontierId);
        if (deletedFrontier != null) {
            refreshCollectionRuntime();
            if (personal && deletedFrontier.isPersistent()) {
                persistLocalPersonalData();
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
        refreshCollectionRuntime();
        persistLocalPersonalData();
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
        refreshCollectionRuntime();
        persistLocalPersonalData();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.updateOverlay();
    }

    public void applyCollectionCreated(CollectionData collection) {
        collectionRuntime.addOrUpdateCollection(collection);
        if (collection.getPersonal()) {
            persistLocalPersonalCollections();
        }
        collectionEvents.postCreated(collection);
    }

    public void applyCollectionUpdated(CollectionData collection) {
        collectionRuntime.addOrUpdateCollection(collection);
        if (collection.getPersonal()) {
            persistLocalPersonalCollections();
        }
        collectionEvents.postUpdated(collection);
    }

    public void applyCollectionDeleted(UUID collectionId) {
        CollectionData existing = collectionRuntime.getCollection(collectionId);
        collectionRuntime.deleteCollection(collectionId);
        refreshCollectionRuntime();
        if (existing != null && existing.getPersonal()) {
            persistLocalPersonalCollections();
        }
        collectionEvents.postDeleted(collectionId);
    }

    public void notifyLocalFrontierUpdated(FrontierOverlay frontierOverlay) {
        frontierEvents.postUpdated(frontierOverlay, -1);
    }

    private void persistLocalPersonalData() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        SettingsUser currentPlayer = new SettingsUser(mc.player);
        localPersonalStore.saveOwnedFrontierMirror(getAllPersonalFrontiers(), currentPlayer);
        localPersonalCollectionStore.saveOwnedCollectionMirror(collectionRuntime.getCollections(true), currentPlayer);
    }

    private void persistLocalPersonalCollections() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        localPersonalCollectionStore.saveOwnedCollectionMirror(collectionRuntime.getCollections(true), new SettingsUser(mc.player));
    }

    private void persistLocalPersonalDataIfPersistent(FrontierData frontier) {
        if (frontier.isPersistent()) {
            persistLocalPersonalData();
        }
    }

    private Collection<FrontierOverlay> getAllPersonalFrontiers() {
        return personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private FrontiersOverlayManager getManager(boolean personal) {
        return personal ? personalManager : globalManager;
    }

    private void refreshCollectionRuntime() {
        collectionRuntime.refreshFromFrontiers(globalManager, personalManager);
    }

    private FrontierData resolveCopiedFrontier(FrontierData receivedFrontier, @Nullable CollectionData receivedCollection) {
        FrontierData resolvedFrontier = new FrontierData(receivedFrontier);
        CollectionData resolvedCollection = resolveCopiedCollection(receivedCollection);
        if (resolvedCollection == null) {
            resolvedFrontier.setCollectionId(null);
        } else {
            resolvedFrontier.setCollectionId(resolvedCollection.getId());
        }
        return resolvedFrontier;
    }

    private @Nullable CollectionData resolveCopiedCollection(@Nullable CollectionData receivedCollection) {
        if (receivedCollection == null || mc.player == null) {
            return null;
        }

        CollectionData existing = collectionRuntime.getPersonalCollectionCopiedFrom(receivedCollection.getCopiedFromId());
        if (existing == null) {
            collectionRuntime.addOrUpdateCollection(receivedCollection);
            return receivedCollection;
        }

        CollectionData updatedCollection = new CollectionData(receivedCollection);
        updatedCollection.setId(existing.getId());
        updatedCollection.setOwner(existing.getOwner());
        updatedCollection.setPersonal(true);
        collectionRuntime.addOrUpdateCollection(updatedCollection);
        return updatedCollection;
    }

    private static boolean usesAuthoritativeCreateFlow(FrontierData.FrontierLifetime lifetime) {
        return lifetime != FrontierData.FrontierLifetime.SESSION_ONLY && MapFrontiersClient.isModOnServer();
    }

    private static boolean usesAuthoritativeMutationFlow(FrontierData frontier) {
        return frontier.isPersistent() && MapFrontiersClient.isModOnServer();
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
        if (frontier.isSessionOnly()) {
            return new SharingActionContext(null, rejectSessionOnlyFrontierAction(operationName, frontierId, pluginModId, user));
        }

        return new SharingActionContext(frontier, null);
    }

    private FrontierActionResult rejectSessionOnlyFrontierAction(String operationName,
                                                                 FrontierId frontierId,
                                                                 @Nullable String pluginModId,
                                                                 @Nullable UserRef user) {
        if (pluginModId == null) {
            MapFrontiers.LOGGER.debug("Rejected {} because SESSION_ONLY personal frontiers are not shareable and cannot be converted. frontierId={}",
                    operationName, frontierId.value());
        } else {
            MapFrontiers.LOGGER.debug("Rejected {} because SESSION_ONLY personal frontiers are not shareable and cannot be converted. pluginModId={}, frontierId={}, targetUser={}",
                    operationName, pluginModId, frontierId.value(), user == null ? null : user.name());
        }
        return FrontierActionResult.rejected();
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
