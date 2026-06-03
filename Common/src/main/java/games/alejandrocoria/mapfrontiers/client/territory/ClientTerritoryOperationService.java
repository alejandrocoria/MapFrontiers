package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.CollectionActionResult;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionEvents;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlayManager;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketCreateCollection;
import games.alejandrocoria.mapfrontiers.common.network.PacketCreateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteCollection;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierResync;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateCollection;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ClientTerritoryOperationService {
    private static final Minecraft mc = Minecraft.getInstance();

    private final ClientTerritoryRuntime runtime;
    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientCollectionRuntime collectionRuntime;
    private final ClientFrontierEvents frontierEvents;
    private final ClientCollectionEvents collectionEvents;
    private final Set<UUID> collectionIdsWithPendingCreatedMembershipSync = new HashSet<>();

    private static class SharingActionContext {
        private final @Nullable FrontierOverlay frontier;
        private final @Nullable FrontierActionResult failure;

        private SharingActionContext(@Nullable FrontierOverlay frontier, @Nullable FrontierActionResult failure) {
            this.frontier = frontier;
            this.failure = failure;
        }
    }

    public ClientTerritoryOperationService(ClientTerritoryRuntime runtime,
                                           FrontiersOverlayManager globalManager,
                                           FrontiersOverlayManager personalManager,
                                           ClientCollectionRuntime collectionRuntime,
                                           ClientFrontierEvents frontierEvents,
                                           ClientCollectionEvents collectionEvents) {
        this.runtime = runtime;
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.collectionRuntime = collectionRuntime;
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
    }

    @Nullable
    public FrontierOverlay createNewFrontierAndReturn(FrontierCreateSpec createSpec) {
        if (!isValidLocalCollectionAssignment(createSpec.isPersonal(), createSpec.getLifetime(), createSpec.getOwner(),
                createSpec.getCollectionId())) {
            return null;
        }

        if (usesAuthoritativeCreateFlow(createSpec.getLifetime())) {
            PacketHandler.sendToServer(new PacketCreateFrontier(createSpec));
            return null;
        }

        if (!createSpec.isPersonal() || mc.player == null) {
            return null;
        }

        FrontierData frontier = FrontierCreationFactory.createFrontier(createSpec);
        FrontierOverlay frontierOverlay = personalManager.addFrontier(frontier);
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierMembershipDirty(frontierOverlay);
        postAffectedCollectionsUpdated(frontierOverlay.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontierOverlay);
        frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        return frontierOverlay;
    }

    public void createCollection(CollectionData collection) {
        if (usesAuthoritativeCollectionMutationFlow(collection)) {
            PacketHandler.sendToServer(new PacketCreateCollection(collection));
            return;
        }

        if (!canMutateLocalCollection(collection)) {
            return;
        }

        createLocalCollection(collection);
    }

    public void updateCollection(CollectionData collection) {
        if (usesAuthoritativeCollectionMutationFlow(collection)) {
            PacketHandler.sendToServer(new PacketUpdateCollection(collection));
            return;
        }

        if (!canMutateLocalCollection(collection)) {
            return;
        }

        updateLocalCollection(collection);
    }

    public void deleteCollection(CollectionData collection) {
        if (usesAuthoritativeCollectionMutationFlow(collection)) {
            PacketHandler.sendToServer(new PacketDeleteCollection(collection.getId()));
            return;
        }

        if (!canMutateLocalCollection(collection)) {
            return;
        }

        deleteLocalCollection(collection);
    }

    public void deleteFrontier(FrontierOverlay frontier) {
        if (usesAuthoritativeMutationFlow(frontier)) {
            PacketHandler.sendToServer(new PacketDeleteFrontier(frontier.getId()));
            return;
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new SettingsUser(mc.player))) {
            return;
        }

        FrontierOverlay deletedFrontier = personalManager.deleteFrontier(frontier.getDimension(), frontier.getId());
        if (deletedFrontier == null) {
            return;
        }

        collectionRuntime.onFrontierRemoved(deletedFrontier);
        notifyCollectionOverlayFrontierMembershipDirty(deletedFrontier);
        postAffectedCollectionsUpdated(deletedFrontier.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontier);
        frontierEvents.postDeleted(frontier.getId());
    }

    public void updateFrontier(FrontierOverlay frontier, FrontierChange change) {
        if (change.isEmpty()) {
            return;
        }
        if (!isValidLocalCollectionChange(frontier, change)) {
            return;
        }

        MapFrontiersClient.markFrontierActivationDirty();

        if (usesAuthoritativeMutationFlow(frontier)) {
            FrontierData expectedFrontier = new FrontierData(frontier);
            expectedFrontier.applyChange(change);
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier.getId(), change, expectedFrontier.computeSyncHash()));
            return;
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new SettingsUser(mc.player))) {
            return;
        }

        ClientCollectionRuntime.FrontierIndexState previousState = collectionRuntime.snapshotFrontier(frontier);
        frontier.applyChange(change);
        getManager(frontier.getPersonal()).refreshFrontierDerivedIndexes(frontier);
        collectionRuntime.onFrontierUpdated(previousState, frontier);
        notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), frontier);
        postAffectedCollectionsUpdated(previousState.collectionId(), frontier.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontier);
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

    public FrontierActionResult createFrontierAction(boolean personal, String pluginModId, FrontierCreateRequest request) {
        return createFrontierAction(personal, pluginModId, request, TerritoryLifetime.PERSISTENT);
    }

    public FrontierActionResult createTemporaryPersonalFrontierAction(String pluginModId, FrontierCreateRequest request) {
        return createFrontierAction(true, pluginModId, request, TerritoryLifetime.SESSION_ONLY);
    }

    private FrontierActionResult createFrontierAction(boolean personal,
                                                      String pluginModId,
                                                      FrontierCreateRequest request,
                                                      TerritoryLifetime lifetime) {
        FrontierCreateSpec createSpec = createFrontierSpec(UUID.randomUUID(), personal, pluginModId, request,
                lifetime);
        if (createSpec == null) {
            return FrontierActionResult.rejected();
        }

        FrontierOverlay frontier = createNewFrontierAndReturn(createSpec);
        FrontierId frontierId = new FrontierId(createSpec.getFrontierId());
        if (frontier == null) {
            return usesAuthoritativeCreateFlow(createSpec.getLifetime())
                    ? FrontierActionResult.acceptedAsync(frontierId)
                    : FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    public Optional<CollectionDataView> getCollectionAction(CollectionId collectionId) {
        CollectionData collection = collectionRuntime.getCollection(collectionId.value());
        return collection == null ? Optional.empty() : Optional.of(ApiConverters.fromCollection(collection));
    }

    public List<CollectionDataView> listCollectionsAction(boolean personal) {
        if (!personal) {
            return collectionRuntime.getCollections(CollectionScope.GLOBAL_PERSISTENT).stream()
                    .map(ApiConverters::fromCollection)
                    .toList();
        }

        ArrayList<CollectionDataView> collections = new ArrayList<>();
        collections.addAll(collectionRuntime.getCollections(CollectionScope.PERSONAL_PERSISTENT).stream()
                .map(ApiConverters::fromCollection)
                .toList());
        collections.addAll(collectionRuntime.getCollections(CollectionScope.PERSONAL_SESSION).stream()
                .map(ApiConverters::fromCollection)
                .toList());
        return List.copyOf(collections);
    }

    public CollectionActionResult createCollectionAction(boolean personal, String pluginModId, CollectionCreateRequest request) {
        if (mc.player == null) {
            return CollectionActionResult.rejected();
        }

        CollectionData collection = createCollectionData(personal, pluginModId, request);
        boolean authoritativeCreate = usesAuthoritativeCollectionMutationFlow(collection);
        createCollection(collection);
        if (authoritativeCreate) {
            return CollectionActionResult.acceptedAsync(new CollectionId(collection.getId()));
        }

        return collectionRuntime.hasCollection(collection.getId())
                ? CollectionActionResult.applied(ApiConverters.fromCollection(collection))
                : CollectionActionResult.rejected();
    }

    public CollectionActionResult createTemporaryPersonalCollectionAction(String pluginModId, CollectionCreateRequest request) {
        if (mc.player == null) {
            return CollectionActionResult.rejected();
        }

        CollectionData collection = createCollectionData(true, pluginModId, TerritoryLifetime.SESSION_ONLY, request);
        createCollection(collection);
        return collectionRuntime.hasCollection(collection.getId())
                ? CollectionActionResult.applied(ApiConverters.fromCollection(collection))
                : CollectionActionResult.rejected();
    }

    public CollectionActionResult updateCollectionAction(boolean personal, CollectionId collectionId, CollectionMutation mutation) {
        CollectionData collection = collectionRuntime.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal() != personal) {
            return CollectionActionResult.notFound(collectionId);
        }

        CollectionData updatedCollection = new CollectionData(collection);
        ApiConverters.applyCollectionMutation(updatedCollection, mutation);
        boolean authoritativeUpdate = usesAuthoritativeCollectionMutationFlow(collection);
        updateCollection(updatedCollection);
        if (authoritativeUpdate) {
            return CollectionActionResult.acceptedAsync(collectionId);
        }

        return collectionRuntime.hasCollection(collectionId.value())
                ? CollectionActionResult.applied(ApiConverters.fromCollection(updatedCollection))
                : CollectionActionResult.rejected();
    }

    public CollectionActionResult deleteCollectionAction(boolean personal, CollectionId collectionId) {
        CollectionData collection = collectionRuntime.getCollection(collectionId.value());
        if (collection == null || collection.getPersonal() != personal) {
            return CollectionActionResult.notFound(collectionId);
        }

        boolean authoritativeDelete = usesAuthoritativeCollectionMutationFlow(collection);
        deleteCollection(collection);
        if (authoritativeDelete) {
            return CollectionActionResult.acceptedAsync(collectionId);
        }

        return collectionRuntime.hasCollection(collectionId.value())
                ? CollectionActionResult.rejected()
                : CollectionActionResult.applied(ApiConverters.fromCollection(collection));
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

        FrontierChange change = FrontierChange.fromMutation(frontier, mutation);
        if (change.isEmpty()) {
            return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
        }
        UUID updatedCollectionId = change.hasCollectionIdChange()
                ? change.getCollectionIdChange().getCollectionId()
                : frontier.getCollectionId();

        if (usesAuthoritativeMutationFlow(frontier)) {
            if (!isValidLocalCollectionAssignment(frontier.getPersonal(), frontier.getLifetime(), frontier.getOwner(), updatedCollectionId)) {
                return FrontierActionResult.rejected();
            }
            FrontierData expectedFrontier = new FrontierData(frontier);
            expectedFrontier.applyChange(change);
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontierId.value(), change, expectedFrontier.computeSyncHash()));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        if (!isValidLocalCollectionAssignment(frontier.getPersonal(), frontier.getLifetime(), frontier.getOwner(), updatedCollectionId)) {
            return FrontierActionResult.rejected();
        }

        updateFrontier(frontier, change);
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
        String operationName = "updateSharedUserPermissionsPartial";
        SharingActionContext context = resolveSharingActionContext(operationName,
                "Could not partially update shared user permissions because frontier was not found locally or is not personal.",
                pluginModId, frontierId, user);
        if (context.failure != null) {
            return context.failure;
        }

        SettingsUserShared currentSharedUser = context.frontier.getUserShared(ApiConverters.toUser(user));
        if (currentSharedUser == null) {
            MapFrontiers.LOGGER.debug("Rejected {} because target user is not currently shared. pluginModId={}, frontierId={}, targetUser={}",
                    operationName, pluginModId, frontierId.value(), user.name());
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
        if (currentFrontier != null) {
            currentFrontier.removeCopiedFromInfo();
            personalManager.refreshFrontierDerivedIndexes(currentFrontier);
            if (mc.player != null) {
                frontierEvents.postUpdated(currentFrontier, mc.player.getId());
            }
        }

        FrontierOverlay frontierOverlay = personalManager.addFrontier(resolveCopiedFrontier(receivedFrontier, receivedCollection));
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierMembershipDirty(frontierOverlay);
        postAffectedCollectionsUpdated(frontierOverlay.getCollectionId());
        markLocalPersonalDataDirty();
        if (mc.player != null) {
            frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        }
        return frontierOverlay;
    }

    public FrontierOverlay acceptCopiedFrontierAndReplace(FrontierData receivedFrontier,
                                                          @Nullable CollectionData receivedCollection,
                                                          FrontierOverlay currentFrontier) {
        UUID previousCollectionId = currentFrontier.getCollectionId();
        FrontierOverlay deletedFrontier = personalManager.deleteFrontier(currentFrontier.getDimension(), currentFrontier.getId());
        if (deletedFrontier != null) {
            collectionRuntime.onFrontierRemoved(deletedFrontier);
            notifyCollectionOverlayFrontierMembershipDirty(deletedFrontier);
        }
        frontierEvents.postDeleted(currentFrontier.getId());

        FrontierOverlay frontierOverlay = personalManager.addFrontier(resolveCopiedFrontier(receivedFrontier, receivedCollection));
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierMembershipDirty(frontierOverlay);
        postAffectedCollectionsUpdated(previousCollectionId, frontierOverlay.getCollectionId());
        if (currentFrontier.isPersistent() || frontierOverlay.isPersistent() || receivedCollection != null) {
            markLocalPersonalDataDirty();
        }
        if (mc.player != null) {
            frontierEvents.postCreated(frontierOverlay, mc.player.getId());
        }
        return frontierOverlay;
    }

    public void applyFrontierCreated(FrontierData frontier, int playerId) {
        FrontierOverlay frontierOverlay = getManager(frontier.getPersonal()).addFrontier(frontier);
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierMembershipDirty(frontierOverlay);
        postAffectedCollectionsUpdated(frontierOverlay.getCollectionId());
        if (frontier.getPersonal() && frontier.isPersistent()) {
            markLocalPersonalDataDirty();
        }
        frontierEvents.postCreated(frontierOverlay, playerId);
    }

    public void applyFrontierUpdated(ResourceKey<Level> dimension,
                                     UUID frontierId,
                                     boolean personal,
                                     FrontierChange change,
                                     long authoritativeSyncHash,
                                     int playerId) {
        FrontiersOverlayManager manager = getManager(personal);
        FrontierOverlay existingFrontier = manager.getFrontier(frontierId);
        ClientCollectionRuntime.FrontierIndexState previousState = existingFrontier == null
                ? null
                : collectionRuntime.snapshotFrontier(existingFrontier);
        FrontierOverlay frontierOverlay = manager.applyFrontierChange(dimension, frontierId, change);
        if (frontierOverlay != null) {
            long localSyncHash = frontierOverlay.computeSyncHash();
            if (localSyncHash != authoritativeSyncHash) {
                MapFrontiers.LOGGER.warn(
                        "Frontier sync hash mismatch after client update apply. frontierId={}, personal={}, expectedHash={}, localHash={}",
                        frontierId, personal, authoritativeSyncHash, localSyncHash
                );
                PacketHandler.sendToServer(new PacketRequestFrontierResync(frontierId));
            }
            if (previousState != null) {
                collectionRuntime.onFrontierUpdated(previousState, frontierOverlay);
                notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), frontierOverlay);
                postAffectedCollectionsUpdated(previousState.collectionId(), frontierOverlay.getCollectionId());
            }
            if (personal && frontierOverlay.isPersistent()) {
                markLocalPersonalDataDirty();
            }
            frontierEvents.postUpdated(frontierOverlay, playerId);
        }
    }

    public void applyFrontierResync(FrontierData frontier) {
        FrontiersOverlayManager targetManager = getManager(frontier.getPersonal());
        FrontiersOverlayManager otherManager = getManager(!frontier.getPersonal());
        FrontierOverlay currentFrontier = targetManager.getFrontier(frontier.getId());
        boolean frontierExisted = currentFrontier != null;
        UUID previousCollectionId = currentFrontier == null ? null : currentFrontier.getCollectionId();

        if (!frontierExisted) {
            FrontierOverlay staleFrontier = otherManager.deleteFrontier(frontier.getId());
            if (staleFrontier != null) {
                previousCollectionId = staleFrontier.getCollectionId();
                collectionRuntime.onFrontierRemoved(staleFrontier);
                notifyCollectionOverlayFrontierMembershipDirty(staleFrontier);
                frontierExisted = true;
            }
        }

        FrontierOverlay appliedFrontier;
        if (currentFrontier != null && currentFrontier.getDimension().equals(frontier.getDimension())) {
            ClientCollectionRuntime.FrontierIndexState previousState = collectionRuntime.snapshotFrontier(currentFrontier);
            currentFrontier.updateFromData(frontier);
            targetManager.refreshFrontierDerivedIndexes(currentFrontier);
            collectionRuntime.onFrontierUpdated(previousState, currentFrontier);
            notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), currentFrontier);
            postAffectedCollectionsUpdated(previousState.collectionId(), currentFrontier.getCollectionId());
            appliedFrontier = currentFrontier;
        } else {
            if (currentFrontier != null) {
                targetManager.deleteFrontier(currentFrontier.getDimension(), currentFrontier.getId());
                collectionRuntime.onFrontierRemoved(currentFrontier);
                notifyCollectionOverlayFrontierMembershipDirty(currentFrontier);
            }

            appliedFrontier = targetManager.addFrontier(frontier);
            collectionRuntime.onFrontierAdded(appliedFrontier);
            notifyCollectionOverlayFrontierMembershipDirty(appliedFrontier);
            postAffectedCollectionsUpdated(previousCollectionId, appliedFrontier.getCollectionId());
        }

        if (frontier.getPersonal() && frontier.isPersistent()) {
            markLocalPersonalDataDirty();
        }

        if (frontierExisted) {
            frontierEvents.postUpdated(appliedFrontier, -1);
        } else {
            frontierEvents.postCreated(appliedFrontier, -1);
        }
    }

    public void applyFrontierSharingUpdated(ResourceKey<Level> dimension,
                                            UUID frontierId,
                                            FrontierSharingChange sharingChange,
                                            int playerId) {
        FrontierOverlay updatedFrontier = personalManager.applyFrontierSharingChange(dimension, frontierId, sharingChange);
        if (updatedFrontier != null) {
            if (updatedFrontier.isPersistent()) {
                markLocalPersonalDataDirty();
            }
            frontierEvents.postUpdated(updatedFrontier, playerId);
        }
    }

    public void applyFrontierDeleted(ResourceKey<Level> dimension, UUID frontierId, boolean personal) {
        FrontierOverlay deletedFrontier = getManager(personal).deleteFrontier(dimension, frontierId);
        if (deletedFrontier != null) {
            collectionRuntime.onFrontierRemoved(deletedFrontier);
            notifyCollectionOverlayFrontierMembershipDirty(deletedFrontier);
            postAffectedCollectionsUpdated(deletedFrontier.getCollectionId());
            if (personal && deletedFrontier.isPersistent()) {
                markLocalPersonalDataDirty();
            }
            frontierEvents.postDeleted(frontierId);
        }
    }

    public void applyFrontierChangeToGlobal(UUID frontierId, @Nullable Date modified) {
        FrontierOverlay frontierOverlay = personalManager.deleteFrontier(frontierId);
        if (frontierOverlay == null) {
            return;
        }
        UUID previousCollectionId = frontierOverlay.getCollectionId();
        collectionRuntime.onFrontierRemoved(frontierOverlay);
        frontierOverlay.setPersonal(false);
        if (modified != null) {
            frontierOverlay.setModified(modified);
        }
        frontierOverlay.removeAllUserShared();
        frontierOverlay.recreateBannerRenderer();
        globalManager.addFrontier(frontierOverlay);
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierUpdated(previousCollectionId, frontierOverlay);
        markLocalPersonalDataDirty();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.rebuildOverlayNow();
    }

    public void applyFrontierChangeToPersonal(UUID frontierId, @Nullable Date modified) {
        FrontierOverlay frontierOverlay = globalManager.deleteFrontier(frontierId);
        if (frontierOverlay == null) {
            return;
        }
        UUID previousCollectionId = frontierOverlay.getCollectionId();
        collectionRuntime.onFrontierRemoved(frontierOverlay);
        frontierOverlay.setPersonal(true);
        if (modified != null) {
            frontierOverlay.setModified(modified);
        }
        frontierOverlay.setCurrentPlayerAsOwner();
        frontierOverlay.recreateBannerRenderer();
        personalManager.addFrontier(frontierOverlay);
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierUpdated(previousCollectionId, frontierOverlay);
        markLocalPersonalDataDirty();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.rebuildOverlayNow();
    }

    public void applyCollectionCreated(CollectionData collection) {
        collectionRuntime.onCollectionUpserted(collection);
        if (mc.player == null || !collection.getOwner().equals(new SettingsUser(mc.player))) {
            collectionIdsWithPendingCreatedMembershipSync.add(collection.getId());
        }
        if (collection.getPersonal()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postCreated(collection);
    }

    public void applyCollectionUpdated(CollectionData collection) {
        collectionRuntime.onCollectionUpserted(collection);
        collectionIdsWithPendingCreatedMembershipSync.remove(collection.getId());
        if (collection.getPersonal()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postUpdated(collection);
    }

    public void applyCollectionDeleted(UUID collectionId) {
        CollectionData existing = collectionRuntime.getCollection(collectionId);
        collectionRuntime.onCollectionDeleted(collectionId);
        collectionIdsWithPendingCreatedMembershipSync.remove(collectionId);
        if (existing != null && existing.getPersonal()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postDeleted(collectionId);
    }

    public void notifyLocalFrontierUpdated(FrontierOverlay frontierOverlay) {
        frontierEvents.postUpdated(frontierOverlay, -1);
    }

    private void markLocalPersonalDataDirty() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        runtime.markDirty();
    }

    private void createLocalCollection(CollectionData collection) {
        CollectionData createdCollection = new CollectionData(collection);
        Date now = new Date();
        createdCollection.setOwner(new SettingsUser(mc.player));
        createdCollection.setCreated(now);
        createdCollection.setModified(now);
        collectionRuntime.onCollectionUpserted(createdCollection);
        if (createdCollection.isPersistent()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postCreated(createdCollection);
    }

    private void updateLocalCollection(CollectionData collection) {
        CollectionData updatedCollection = new CollectionData(collection);
        updatedCollection.setOwner(new SettingsUser(mc.player));
        updatedCollection.setModified(new Date());
        collectionRuntime.onCollectionUpserted(updatedCollection);
        if (updatedCollection.isPersistent()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postUpdated(updatedCollection);
    }

    private void deleteLocalCollection(CollectionData collection) {
        List<FrontierOverlay> affectedFrontiers = new ArrayList<>(collectionRuntime.getFrontiersInCollection(collection.getId()));
        for (FrontierOverlay frontier : affectedFrontiers) {
            ClientCollectionRuntime.FrontierIndexState previousState = collectionRuntime.snapshotFrontier(frontier);
            frontier.setCollectionId(null);
            getManager(frontier.getPersonal()).refreshFrontierDerivedIndexes(frontier);
            collectionRuntime.onFrontierUpdated(previousState, frontier);
            notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), frontier);
        }

        collectionRuntime.onCollectionDeleted(collection.getId());
        if (collection.isPersistent()) {
            markLocalPersonalDataDirty();
        }
        for (FrontierOverlay frontier : affectedFrontiers) {
            frontierEvents.postUpdated(frontier, -1);
        }
        collectionEvents.postDeleted(collection.getId());
    }

    private void markLocalPersonalDataDirtyIfPersistent(FrontierData frontier) {
        if (frontier.isPersistent()) {
            markLocalPersonalDataDirty();
        }
    }

    private void postAffectedCollectionsUpdated(@Nullable UUID collectionId) {
        if (collectionId == null) {
            return;
        }

        if (collectionIdsWithPendingCreatedMembershipSync.remove(collectionId)) {
            return;
        }

        CollectionData collection = collectionRuntime.getCollection(collectionId);
        if (collection != null) {
            collectionEvents.postUpdated(collection);
        }
    }

    private void postAffectedCollectionsUpdated(@Nullable UUID previousCollectionId, @Nullable UUID currentCollectionId) {
        if (previousCollectionId != null && previousCollectionId.equals(currentCollectionId)) {
            postAffectedCollectionsUpdated(previousCollectionId);
            return;
        }

        postAffectedCollectionsUpdated(previousCollectionId);
        postAffectedCollectionsUpdated(currentCollectionId);
    }

    private FrontiersOverlayManager getManager(boolean personal) {
        return personal ? personalManager : globalManager;
    }

    private CollectionOverlayManager getCollectionOverlayManager() {
        return runtime.getCollectionOverlayManager();
    }

    private void notifyCollectionOverlayFrontierMembershipDirty(FrontierOverlay frontier) {
        getCollectionOverlayManager().markFrontierMembershipDirty(frontier);
    }

    private void notifyCollectionOverlayFrontierUpdated(@Nullable UUID previousCollectionId, FrontierOverlay frontier) {
        getCollectionOverlayManager().markFrontierGeometryDirty(frontier, previousCollectionId);
    }

    private CollectionData createCollectionData(boolean personal, String pluginModId, CollectionCreateRequest request) {
        return createCollectionData(personal, pluginModId, TerritoryLifetime.PERSISTENT, request);
    }

    private CollectionData createCollectionData(boolean personal,
                                                String pluginModId,
                                                TerritoryLifetime lifetime,
                                                CollectionCreateRequest request) {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setPersonal(personal);
        collection.setLifetime(lifetime);
        collection.setOwner(new SettingsUser(mc.player));
        collection.setSourcePluginId(pluginModId);
        collection.removeCopiedFromInfo();
        request.name().ifPresent(collection::setName);
        request.color().ifPresent(collection::setColor);
        CollectionVisibilityData visibility = request.visibility()
                .map(ApiConverters::toCollectionVisibility)
                .orElseGet(ApiConverters::defaultCollectionVisibility);
        BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(ApiConverters::defaultCollectionBanner);
        collection.setVisibilityData(visibility);
        collection.setBannerData(banner);
        Date now = new Date();
        collection.setCreated(now);
        return collection;
    }

    private @Nullable FrontierCreateSpec createFrontierSpec(UUID frontierId,
                                                            boolean personal,
                                                            @Nullable String sourcePluginId,
                                                            FrontierCreateRequest request,
                                                            TerritoryLifetime lifetime) {
        if (mc.player == null) {
            return null;
        }

        FrontierData defaults = new FrontierData();
        SettingsUser owner = new SettingsUser(mc.player);
        UUID collectionId = request.collectionId().map(CollectionId::value).orElse(null);
        UUID validatedCollectionId = resolveValidLocalCollectionId(personal, lifetime, owner, collectionId);
        if (collectionId != null && validatedCollectionId == null) {
            return null;
        }
        String name1 = request.name1().orElse(defaults.getName1());
        String name2 = request.name2().orElse(defaults.getName2());
        int color = request.color().orElseGet(ColorHelper::getRandomColor);
        FrontierVisibilityData visibility = request.visibility()
                .map(ApiConverters::toFrontierVisibility)
                .orElseGet(defaults::getVisibilityData);
        BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(defaults::getBannerData);
        boolean pathShape = switch (request.shape().type()) {
            case PATH -> true;
            default -> false;
        };
        FrontierData.PathStyle pathStyle = pathShape
                ? request.pathStyle().map(ApiConverters::toPathStyle).orElseGet(FrontierData.PathStyle::new)
                : new FrontierData.PathStyle();

        return switch (request.shape().type()) {
            case VERTEX -> FrontierCreateSpec.vertex(frontierId, owner, personal, ApiConverters.toDimension(request.dimension()),
                    lifetime, validatedCollectionId, sourcePluginId, name1, name2, color, visibility, banner,
                    request.shape().vertices() == null ? List.of() : request.shape().vertices().stream()
                            .map(vertex -> new BlockPos(vertex.x(), 0, vertex.z()))
                            .toList(),
                    pathStyle);
            case CHUNK -> FrontierCreateSpec.chunk(frontierId, owner, personal, ApiConverters.toDimension(request.dimension()),
                    lifetime, validatedCollectionId, sourcePluginId, name1, name2, color, visibility, banner,
                    request.shape().chunks() == null ? Set.of() : request.shape().chunks().stream()
                            .map(chunk -> new ChunkPos(chunk.x(), chunk.z()))
                            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)),
                    pathStyle);
            case PATH -> FrontierCreateSpec.path(frontierId, owner, personal, ApiConverters.toDimension(request.dimension()),
                    lifetime, validatedCollectionId, sourcePluginId, name1, name2, color, visibility, banner,
                    request.shape().points() == null ? List.of() : request.shape().points().stream()
                            .map(point -> new BlockPos(point.x(), 0, point.z()))
                            .toList(),
                    pathStyle);
        };
    }

    private @Nullable UUID resolveValidLocalCollectionId(boolean personal,
                                                         TerritoryLifetime lifetime,
                                                         SettingsUser owner,
                                                         @Nullable UUID collectionId) {
        if (collectionId == null) {
            return null;
        }

        CollectionData collection = collectionRuntime.getCollection(collectionId);
        if (collection == null || collection.getPersonal() != personal) {
            return null;
        }
        if (collection.getLifetime() != lifetime) {
            return null;
        }
        if (personal && !collection.getOwner().equals(owner)) {
            return null;
        }
        return collectionId;
    }

    private boolean isValidLocalCollectionAssignment(boolean personal,
                                                     TerritoryLifetime lifetime,
                                                     SettingsUser owner,
                                                     @Nullable UUID collectionId) {
        return collectionId == null || resolveValidLocalCollectionId(personal, lifetime, owner, collectionId) != null;
    }

    private boolean isValidLocalCollectionChange(FrontierOverlay frontier, FrontierChange change) {
        if (!change.hasCollectionIdChange()) {
            return true;
        }

        return isValidLocalCollectionAssignment(frontier.getPersonal(), frontier.getLifetime(), frontier.getOwner(),
                change.getCollectionIdChange().getCollectionId());
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
            collectionRuntime.onCollectionUpserted(receivedCollection);
            return receivedCollection;
        }

        CollectionData updatedCollection = new CollectionData(receivedCollection);
        updatedCollection.setId(existing.getId());
        updatedCollection.setOwner(existing.getOwner());
        updatedCollection.setPersonal(true);
        collectionRuntime.onCollectionUpserted(updatedCollection);
        return updatedCollection;
    }

    private static boolean usesAuthoritativeCreateFlow(TerritoryLifetime lifetime) {
        return lifetime != TerritoryLifetime.SESSION_ONLY && MapFrontiersClient.isModOnServer();
    }

    private static boolean usesAuthoritativeCollectionMutationFlow(CollectionData collection) {
        return collection.isPersistent() && MapFrontiersClient.isModOnServer();
    }

    private boolean canMutateLocalCollection(CollectionData collection) {
        if (mc.player == null) {
            return false;
        }

        return collection.getPersonal() && collection.getOwner().equals(new SettingsUser(mc.player));
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
