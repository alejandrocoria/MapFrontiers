package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.client.CollectionActionResult;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.CollectionCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.DefaultValuesProfile;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionEvents;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlayManager;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
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
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChangeApplicationResult;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
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
import java.util.function.Consumer;

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
    private final Set<UUID> awaitingFrontierResync = new HashSet<>();
    private final PendingOptimisticFrontierUpdates pendingOptimisticFrontierUpdates = new PendingOptimisticFrontierUpdates();
    private final PendingOptimisticCollectionUpdates pendingOptimisticCollectionUpdates = new PendingOptimisticCollectionUpdates();
    private final PendingOptimisticSharingUpdates pendingOptimisticSharingUpdates = new PendingOptimisticSharingUpdates();

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

    /**
     * Submits a collection whose live client object already contains the complete optimistic state. The caller must
     * mutate it on the client thread without changing its authoritative base revision or publishing another update.
     */
    public void submitOptimisticCollectionUpdate(CollectionData collection) {
        if (usesAuthoritativeCollectionMutationFlow(collection)) {
            PendingOptimisticCollectionUpdates.Outbound outbound = pendingOptimisticCollectionUpdates.submit(
                    collection, MapFrontiersClient::nextRequestId);
            collectionEvents.postUpdated(collection);
            if (outbound != null) {
                sendCollectionUpdate(outbound);
            }
            return;
        }

        if (canMutateLocalCollection(collection)) {
            updateLocalCollection(collection);
        }
    }

    public void submitCollectionUpdate(CollectionData collection) {
        if (usesAuthoritativeCollectionMutationFlow(collection)) {
            PacketHandler.sendToServer(new PacketUpdateCollection(collection, collection.getCollectionRevision(),
                    MapFrontiersClient.nextRequestId()));
            return;
        }

        if (!canMutateLocalCollection(collection)) {
            return;
        }

        updateLocalCollection(collection);
    }

    private void sendCollectionUpdate(PendingOptimisticCollectionUpdates.Outbound outbound) {
        PacketHandler.sendToServer(new PacketUpdateCollection(outbound.snapshot(), outbound.baseRevision(),
                outbound.requestId()));
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

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new PlayerId(mc.player.getUUID()))) {
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

    public FrontierChangeApplicationResult updateFrontier(FrontierOverlay frontier, FrontierChange change) {
        if (change.isEmpty()) {
            return FrontierChangeApplicationResult.noChange(frontier);
        }
        if (!isValidLocalCollectionChange(frontier, change)) {
            return FrontierChangeApplicationResult.rejected("Invalid collection assignment");
        }

        if (usesAuthoritativeMutationFlow(frontier)) {
            long baseSyncHash = frontier.computeSyncHash();
            FrontierChangeApplicationResult stagedResult = frontier.stageChange(change);
            if (!stagedResult.isApplied()) {
                return stagedResult;
            }
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier.getId(), stagedResult.effectiveChange(), baseSyncHash));
            return stagedResult;
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new PlayerId(mc.player.getUUID()))) {
            return FrontierChangeApplicationResult.rejected("Local frontier is not owned by the current player");
        }

        FrontierChangeApplicationResult stagedResult = frontier.stageChange(change);
        if (!stagedResult.isApplied()) {
            return stagedResult;
        }

        FrontierChange effectiveChange = stagedResult.effectiveChange();
        effectiveChange.setModifiedTime(new Date().getTime());
        ClientCollectionRuntime.FrontierIndexState previousState = collectionRuntime.snapshotFrontier(frontier);
        FrontierChangeApplicationResult appliedResult = frontier.applyChange(effectiveChange);
        if (!appliedResult.isApplied()) {
            return appliedResult;
        }
        getManager(frontier.getPersonal()).refreshFrontierDerivedIndexes(frontier);
        collectionRuntime.onFrontierUpdated(previousState, frontier);
        notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), frontier, appliedResult.effectiveChange());
        postAffectedCollectionsUpdated(previousState.collectionId(), frontier.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontier);
        frontierEvents.postUpdated(frontier, mc.player.getId());
        return appliedResult;
    }

    /**
     * Submits a GUI change that is already reflected in {@code frontier}. The base hash must be captured before the
     * first local mutation represented by {@code change}.
     */
    public FrontierChangeApplicationResult submitOptimisticFrontierChange(FrontierOverlay frontier, FrontierChange change,
                                                                           long baseSyncHash) {
        if (change.isEmpty()) {
            return FrontierChangeApplicationResult.noChange(frontier);
        }
        if (!isValidLocalCollectionChange(frontier, change)) {
            return FrontierChangeApplicationResult.rejected("Invalid collection assignment");
        }

        FrontierChangeApplicationResult currentStateResult = frontier.stageChange(change);
        if (currentStateResult.isRejected()) {
            return currentStateResult;
        }
        if (currentStateResult.isApplied()) {
            return FrontierChangeApplicationResult.rejected("Optimistic frontier change has not been applied locally");
        }
        if (baseSyncHash == frontier.computeSyncHash()) {
            return FrontierChangeApplicationResult.noChange(frontier);
        }

        FrontierChange effectiveChange = new FrontierChange(change);
        if (usesAuthoritativeMutationFlow(frontier)) {
            pendingOptimisticFrontierUpdates.expect(frontier.getId(), frontier.computeSyncHash());
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier.getId(), effectiveChange, baseSyncHash));
            return FrontierChangeApplicationResult.applied(frontier, effectiveChange);
        }

        if (!frontier.getPersonal() || mc.player == null || !frontier.getOwner().equals(new PlayerId(mc.player.getUUID()))) {
            return FrontierChangeApplicationResult.rejected("Local frontier is not owned by the current player");
        }

        effectiveChange.setModifiedTime(new Date().getTime());
        frontier.setModified(new Date(effectiveChange.getModifiedTime()));
        ClientCollectionRuntime.FrontierIndexState currentState = collectionRuntime.snapshotFrontier(frontier);
        getManager(frontier.getPersonal()).refreshFrontierDerivedIndexes(frontier);
        collectionRuntime.onFrontierUpdated(currentState, frontier);
        notifyCollectionOverlayFrontierUpdated(currentState.collectionId(), frontier, effectiveChange);
        postAffectedCollectionsUpdated(currentState.collectionId(), frontier.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontier);
        frontierEvents.postUpdated(frontier, mc.player.getId());
        return FrontierChangeApplicationResult.applied(frontier, effectiveChange);
    }

    public boolean submitOptimisticShareFrontier(UUID frontierId, SettingsUser targetUser) {
        FrontierOverlay frontier = resolveOptimisticSharingFrontier(frontierId);
        if (frontier == null || mc.player == null) {
            return false;
        }

        PlayerId currentUser = new PlayerId(mc.player.getUUID());
        PlayerId targetId = targetUser.toPlayerId();
        FrontierUserAccess desiredUserShared = new FrontierUserAccess(targetId, true);
        if (!applyOptimisticShareLocally(frontier, targetId, currentUser,
                () -> postOptimisticSharingUpdated(frontier))) {
            return false;
        }

        PendingOptimisticSharingUpdates.Outbound outbound = pendingOptimisticSharingUpdates.submit(frontierId,
                PendingOptimisticSharingUpdates.Intent.add(desiredUserShared), frontier.getSharingRevision(),
                MapFrontiersClient::nextRequestId);
        if (outbound != null) {
            sendSharingUpdate(outbound);
        }
        return true;
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
        submitCollectionUpdate(updatedCollection);
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

    public List<FrontierDataView> listFrontiersInCollectionAction(boolean personal, CollectionId collectionId) {
        return collectionRuntime.getFrontiersInCollection(collectionId.value()).stream()
                .filter(frontier -> frontier.getPersonal() == personal)
                .filter(frontier -> personal || frontier.isPersistent())
                .map(ApiConverters::fromFrontier)
                .toList();
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
            long baseSyncHash = frontier.computeSyncHash();
            FrontierChangeApplicationResult stagedResult = frontier.stageChange(change);
            if (stagedResult.isRejected()) {
                return FrontierActionResult.rejected();
            }
            if (stagedResult.isNoChange()) {
                return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
            }
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontierId.value(), stagedResult.effectiveChange(), baseSyncHash));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        if (!isValidLocalCollectionAssignment(frontier.getPersonal(), frontier.getLifetime(), frontier.getOwner(), updatedCollectionId)) {
            return FrontierActionResult.rejected();
        }

        FrontierChangeApplicationResult applicationResult = updateFrontier(frontier, change);
        if (applicationResult.isRejected()) {
            return FrontierActionResult.rejected();
        }
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

        PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId.value(),
                createSharedUser(user, permissions), context.frontier.getSharingRevision(),
                MapFrontiersClient.nextRequestId()));
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

        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId.value(),
                createSharedUser(user, permissions), context.frontier.getSharingRevision(),
                MapFrontiersClient.nextRequestId()));
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

        FrontierUserAccess currentSharedUser = context.frontier.getUserAccess(ApiConverters.toPlayerId(user));
        if (currentSharedUser == null) {
            MapFrontiers.LOGGER.debug("Rejected {} because target user is not currently shared. pluginModId={}, frontierId={}, targetUser={}",
                    operationName, pluginModId, frontierId.value(), user.name());
            return FrontierActionResult.rejected();
        }

        EnumSet<FrontierSharePermission> permissions = EnumSet.noneOf(FrontierSharePermission.class);
        for (FrontierUserAccess.Action action : currentSharedUser.getActions()) {
            permissions.add(ApiConverters.toFrontierSharePermission(action));
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

        PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId.value(), ApiConverters.toUser(user),
                context.frontier.getSharingRevision(), MapFrontiersClient.nextRequestId()));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    public boolean submitOptimisticRemoveSharedUser(UUID frontierId, SettingsUser targetUser) {
        FrontierOverlay frontier = resolveOptimisticSharingFrontier(frontierId);
        if (frontier == null || mc.player == null) {
            return false;
        }

        if (!applyOptimisticRemoveSharedUserLocally(frontier, targetUser.toPlayerId(), new PlayerId(mc.player.getUUID()),
                () -> postOptimisticSharingUpdated(frontier))) {
            return false;
        }

        PendingOptimisticSharingUpdates.Outbound outbound = pendingOptimisticSharingUpdates.submit(frontierId,
                PendingOptimisticSharingUpdates.Intent.remove(targetUser.toPlayerId()), frontier.getSharingRevision(),
                MapFrontiersClient::nextRequestId);
        if (outbound != null) {
            sendSharingUpdate(outbound);
        }
        return true;
    }

    public boolean submitOptimisticUpdateSharedUser(UUID frontierId, FrontierUserAccess desiredUserShared) {
        FrontierOverlay frontier = resolveOptimisticSharingFrontier(frontierId);
        if (frontier == null || mc.player == null) {
            return false;
        }

        if (!applyOptimisticSharedUserUpdateLocally(frontier, desiredUserShared, new PlayerId(mc.player.getUUID()),
                () -> postOptimisticSharingUpdated(frontier))) {
            return false;
        }

        PendingOptimisticSharingUpdates.Outbound outbound = pendingOptimisticSharingUpdates.submit(frontierId,
                PendingOptimisticSharingUpdates.Intent.update(desiredUserShared), frontier.getSharingRevision(),
                MapFrontiersClient::nextRequestId);
        if (outbound != null) {
            sendSharingUpdate(outbound);
        }
        return true;
    }

    private void sendSharingUpdate(PendingOptimisticSharingUpdates.Outbound outbound) {
        FrontierUserAccess userShared = outbound.intent().userShared();
        switch (outbound.intent().type()) {
            case Add -> PacketHandler.sendToServer(new PacketSharePersonalFrontier(outbound.frontierId(), userShared,
                    outbound.baseRevision(), outbound.requestId()));
            case Remove -> PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(outbound.frontierId(),
                    new SettingsUser(userShared.getPlayerId()), outbound.baseRevision(), outbound.requestId()));
            case Update -> PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(outbound.frontierId(),
                    userShared, outbound.baseRevision(), outbound.requestId()));
        }
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
        pendingOptimisticFrontierUpdates.clear(frontier.getId());
        pendingOptimisticSharingUpdates.clear(frontier.getId());
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
        if (awaitingFrontierResync.contains(frontierId)) {
            return;
        }

        FrontiersOverlayManager manager = getManager(personal);
        FrontierOverlay existingFrontier = manager.getFrontier(frontierId);
        if (existingFrontier == null || !existingFrontier.getDimension().equals(dimension)) {
            requestFrontierResync(frontierId, "frontier is missing or in a different dimension");
            return;
        }

        if (acknowledgeOptimisticFrontierUpdate(existingFrontier, change, authoritativeSyncHash, playerId)) {
            return;
        }

        FrontierChangeApplicationResult stagedResult = existingFrontier.stageChange(change);
        if (stagedResult.isRejected()) {
            requestFrontierResync(frontierId, stagedResult.rejectionReason());
            return;
        }

        FrontierData stagedFrontier = stagedResult.frontier();
        long stagedSyncHash = stagedFrontier == null ? existingFrontier.computeSyncHash() : stagedFrontier.computeSyncHash();
        if (stagedSyncHash != authoritativeSyncHash) {
            MapFrontiers.LOGGER.warn(
                    "Frontier sync hash mismatch before client update commit. frontierId={}, personal={}, authoritativeHash={}, stagedHash={}",
                    frontierId, personal, authoritativeSyncHash, stagedSyncHash
            );
            requestFrontierResync(frontierId, "authoritative sync hash mismatch");
            return;
        }
        if (stagedResult.isNoChange()) {
            return;
        }

        ClientCollectionRuntime.FrontierIndexState previousState = collectionRuntime.snapshotFrontier(existingFrontier);
        FrontierChangeApplicationResult appliedResult = manager.applyFrontierChange(dimension, frontierId, stagedResult.effectiveChange());
        if (appliedResult == null || !appliedResult.isApplied()) {
            requestFrontierResync(frontierId, appliedResult == null ? "frontier disappeared before commit" : appliedResult.rejectionReason());
            return;
        }

        FrontierOverlay frontierOverlay = manager.getFrontier(frontierId);
        if (frontierOverlay != null) {
            FrontierChange effectiveChange = appliedResult.effectiveChange();
            collectionRuntime.onFrontierUpdated(previousState, frontierOverlay);
            notifyCollectionOverlayFrontierUpdated(previousState.collectionId(), frontierOverlay, effectiveChange);
            postAffectedCollectionsUpdated(previousState.collectionId(), frontierOverlay.getCollectionId());
            if (personal && frontierOverlay.isPersistent()) {
                markLocalPersonalDataDirty();
            }
            frontierEvents.postUpdated(frontierOverlay, playerId);
        }
    }

    public void applyFrontierResync(FrontierData frontier) {
        pendingOptimisticFrontierUpdates.clear(frontier.getId());
        pendingOptimisticSharingUpdates.clear(frontier.getId());
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
        }

        awaitingFrontierResync.remove(frontier.getId());
        postAffectedCollectionsUpdated(previousCollectionId, appliedFrontier.getCollectionId());

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
                                            int playerId,
                                            long requestId,
                                            OperationResolution resolution) {
        FrontierOverlay currentFrontier = personalManager.getFrontier(frontierId);
        if (currentFrontier == null || !currentFrontier.getDimension().equals(dimension)) {
            pendingOptimisticSharingUpdates.clear(frontierId);
            return;
        }

        int currentPlayerId = mc.player == null ? -1 : mc.player.getId();
        PendingOptimisticSharingUpdates.Reconciliation reconciliation = pendingOptimisticSharingUpdates.reconcile(
                frontierId, sharingChange, currentFrontier.getSharingRevision(), playerId, currentPlayerId, requestId,
                resolution, MapFrontiersClient::nextRequestId);
        if (reconciliation.ignored()) {
            return;
        }

        FrontierSharingChange currentChange = FrontierSharingChange.fromFrontierData(currentFrontier);
        boolean changed = !currentChange.hasSameFunctionalState(reconciliation.visibleChange());
        FrontierOverlay updatedFrontier = personalManager.applyFrontierSharingChange(dimension, frontierId,
                reconciliation.visibleChange());
        if (updatedFrontier != null) {
            if (updatedFrontier.isPersistent()) {
                markLocalPersonalDataDirty();
            }
            if (changed) {
                frontierEvents.postUpdated(updatedFrontier, playerId);
            }
        }

        if (reconciliation.nextOutbound() != null) {
            sendSharingUpdate(reconciliation.nextOutbound());
        }
    }

    public void applyFrontierDeleted(UUID frontierId, boolean personal) {
        awaitingFrontierResync.remove(frontierId);
        pendingOptimisticFrontierUpdates.clear(frontierId);
        pendingOptimisticSharingUpdates.clear(frontierId);
        FrontierOverlay deletedFrontier = getManager(personal).deleteFrontier(frontierId);
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
        pendingOptimisticFrontierUpdates.clear(frontierId);
        pendingOptimisticSharingUpdates.clear(frontierId);
        applyFrontierScopeChange(personalManager, globalManager, frontierId, modified, false, frontierOverlay -> {
            frontierOverlay.removeAllUserAccesses();
            frontierOverlay.recreateBannerRenderer();
        });
    }

    public void applyFrontierChangeToPersonal(UUID frontierId, @Nullable Date modified) {
        pendingOptimisticFrontierUpdates.clear(frontierId);
        pendingOptimisticSharingUpdates.clear(frontierId);
        applyFrontierScopeChange(globalManager, personalManager, frontierId, modified, true, frontierOverlay -> {
            frontierOverlay.setCurrentPlayerAsOwner();
            frontierOverlay.recreateBannerRenderer();
        });
    }

    public void applyCollectionCreated(CollectionData collection) {
        pendingOptimisticCollectionUpdates.clear(collection.getId());
        collectionRuntime.onCollectionUpserted(collection);
        if (mc.player == null || !collection.getOwner().equals(new PlayerId(mc.player.getUUID()))) {
            collectionIdsWithPendingCreatedMembershipSync.add(collection.getId());
        }
        if (collection.getPersonal()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postCreated(collection);
    }

    public void applyCollectionUpdated(CollectionData collection, int playerId, long requestId,
                                       OperationResolution resolution) {
        CollectionData currentCollection = collectionRuntime.getCollection(collection.getId());
        if (currentCollection == null) {
            pendingOptimisticCollectionUpdates.clear(collection.getId());
            collectionRuntime.onCollectionUpserted(collection);
            collectionIdsWithPendingCreatedMembershipSync.remove(collection.getId());
            if (collection.getPersonal()) {
                markLocalPersonalDataDirty();
            }
            collectionEvents.postUpdated(collection);
            return;
        }

        boolean currentActor = mc.player != null && playerId == mc.player.getId();
        PendingOptimisticCollectionUpdates.Reconciliation reconciliation = pendingOptimisticCollectionUpdates.reconcile(
                collection, currentCollection, requestId, resolution, currentActor, MapFrontiersClient::nextRequestId);
        if (reconciliation.ignored()) {
            return;
        }

        CollectionData visibleCollection = reconciliation.visibleSnapshot();
        boolean changed = !currentCollection.hasSameSynchronizedState(visibleCollection);
        collectionIdsWithPendingCreatedMembershipSync.remove(collection.getId());
        if (changed) {
            collectionRuntime.onCollectionUpserted(visibleCollection);
            if (visibleCollection.getPersonal()) {
                markLocalPersonalDataDirty();
            }
            collectionEvents.postUpdated(visibleCollection);
        }

        if (reconciliation.nextOutbound() != null) {
            sendCollectionUpdate(reconciliation.nextOutbound());
        }
    }

    public void applyCollectionDeleted(UUID collectionId) {
        pendingOptimisticCollectionUpdates.clear(collectionId);
        CollectionData existing = collectionRuntime.getCollection(collectionId);
        collectionRuntime.onCollectionDeleted(collectionId);
        collectionIdsWithPendingCreatedMembershipSync.remove(collectionId);
        if (existing != null && existing.getPersonal()) {
            markLocalPersonalDataDirty();
        }
        collectionEvents.postDeleted(collectionId);
    }

    public void clearPendingOptimisticUpdates() {
        pendingOptimisticFrontierUpdates.clearAll();
        pendingOptimisticCollectionUpdates.clearAll();
        pendingOptimisticSharingUpdates.clearAll();
    }

    private void markLocalPersonalDataDirty() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        runtime.markDirty();
    }

    private void applyFrontierScopeChange(FrontiersOverlayManager sourceManager,
                                          FrontiersOverlayManager targetManager,
                                          UUID frontierId,
                                          @Nullable Date modified,
                                          boolean targetPersonal,
                                          Consumer<FrontierOverlay> afterScopeChange) {
        FrontierOverlay frontierOverlay = sourceManager.deleteFrontier(frontierId);
        if (frontierOverlay == null) {
            return;
        }

        UUID previousCollectionId = frontierOverlay.getCollectionId();
        collectionRuntime.onFrontierRemoved(frontierOverlay);
        // Remove the old collection membership before changing scope and re-adding the frontier so
        // collection indexes, overlays, and affected collection pages all refresh from the new state.
        frontierOverlay.setCollectionId(null);
        frontierOverlay.setPersonal(targetPersonal);
        if (modified != null) {
            frontierOverlay.setModified(modified);
        }
        afterScopeChange.accept(frontierOverlay);
        targetManager.addFrontier(frontierOverlay);
        collectionRuntime.onFrontierAdded(frontierOverlay);
        notifyCollectionOverlayFrontierUpdated(previousCollectionId, frontierOverlay);
        postAffectedCollectionsUpdated(previousCollectionId, frontierOverlay.getCollectionId());
        markLocalPersonalDataDirty();
        frontierEvents.postUpdated(frontierOverlay, -1);
        frontierOverlay.rebuildOverlayNow();
    }

    private void createLocalCollection(CollectionData collection) {
        CollectionData createdCollection = new CollectionData(collection);
        Date now = new Date();
        createdCollection.setOwner(new PlayerId(mc.player.getUUID()));
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
        updatedCollection.setOwner(new PlayerId(mc.player.getUUID()));
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

    private void requestFrontierResync(UUID frontierId, @Nullable String reason) {
        if (!awaitingFrontierResync.add(frontierId)) {
            return;
        }
        MapFrontiers.LOGGER.warn("Requesting frontier resync. frontierId={}, reason={}", frontierId, reason);
        PacketHandler.sendToServer(new PacketRequestFrontierResync(frontierId));
    }

    private boolean acknowledgeOptimisticFrontierUpdate(FrontierOverlay frontier, FrontierChange change,
                                                        long authoritativeSyncHash, int playerId) {
        if (mc.player == null || playerId != mc.player.getId()) {
            return false;
        }

        if (!pendingOptimisticFrontierUpdates.acknowledge(frontier.getId(), authoritativeSyncHash)) {
            return false;
        }

        if (change.hasModifiedTime()) {
            frontier.setModified(new Date(change.getModifiedTime()));
        }
        ClientCollectionRuntime.FrontierIndexState currentState = collectionRuntime.snapshotFrontier(frontier);
        getManager(frontier.getPersonal()).refreshFrontierDerivedIndexes(frontier);
        collectionRuntime.onFrontierUpdated(currentState, frontier);
        notifyCollectionOverlayFrontierUpdated(currentState.collectionId(), frontier, change);
        postAffectedCollectionsUpdated(currentState.collectionId(), frontier.getCollectionId());
        markLocalPersonalDataDirtyIfPersistent(frontier);
        frontierEvents.postUpdated(frontier, playerId);
        return true;
    }

    private void notifyCollectionOverlayFrontierMembershipDirty(FrontierOverlay frontier) {
        getCollectionOverlayManager().markFrontierMembershipDirty(frontier);
    }

    private void notifyCollectionOverlayFrontierUpdated(@Nullable UUID previousCollectionId, FrontierOverlay frontier) {
        getCollectionOverlayManager().markFrontierMembershipDirty(frontier);
        getCollectionOverlayManager().markFrontierGeometryDirty(frontier, previousCollectionId);
    }

    private void notifyCollectionOverlayFrontierUpdated(@Nullable UUID previousCollectionId,
                                                        FrontierOverlay frontier,
                                                        FrontierChange change) {
        if (affectsCollectionMembership(change)) {
            notifyCollectionOverlayFrontierUpdated(previousCollectionId, frontier);
        } else if (affectsCollectionVariants(change)) {
            getCollectionOverlayManager().markFrontierGeometryDirty(frontier, previousCollectionId);
        }
    }

    static boolean affectsCollectionMembership(FrontierChange change) {
        return change.affectsGeometry() || change.hasCollectionIdChange();
    }

    static boolean affectsCollectionVariants(FrontierChange change) {
        return affectsCollectionMembership(change) || change.hasVisibilityChange();
    }

    private CollectionData createCollectionData(boolean personal, String pluginModId, CollectionCreateRequest request) {
        return createCollectionData(personal, pluginModId, TerritoryLifetime.PERSISTENT, request);
    }

    private CollectionData createCollectionData(boolean personal,
                                                String pluginModId,
                                                TerritoryLifetime lifetime,
                                                CollectionCreateRequest request) {
        PlayerId owner = new PlayerId(mc.player.getUUID());
        CollectionData defaults = request.defaultValuesProfile() == DefaultValuesProfile.CONFIGURED
                ? ClientConfig.createConfiguredCollectionDefaults(owner)
                : ClientConfig.createBuiltinCollectionDefaults(owner);
        CollectionData collection = new CollectionData(defaults);
        collection.setId(UUID.randomUUID());
        collection.setPersonal(personal);
        collection.setLifetime(lifetime);
        collection.setSourcePluginId(pluginModId);
        collection.removeCopiedFromInfo();
        request.name().ifPresent(collection::setName);
        request.color().ifPresent(collection::setColor);
        request.visibility()
                .map(ApiConverters::toCollectionVisibility)
                .ifPresent(collection::setVisibilityData);
        request.banner()
                .map(ApiConverters::toBanner)
                .ifPresent(collection::setBannerData);
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

        FrontierShape frontierShape = switch (request.shape().type()) {
            case VERTEX -> FrontierShape.Vertex;
            case CHUNK -> FrontierShape.Chunk;
            case PATH -> FrontierShape.Path;
        };
        PlayerId owner = new PlayerId(mc.player.getUUID());
        FrontierData defaults = request.defaultValuesProfile() == DefaultValuesProfile.CONFIGURED
                ? ClientConfig.createConfiguredFrontierDefaults(frontierShape, owner)
                : ClientConfig.createBuiltinFrontierDefaults(frontierShape, owner);
        UUID collectionId = request.collectionId().map(CollectionId::value).orElse(null);
        UUID validatedCollectionId = resolveValidLocalCollectionId(personal, lifetime, owner, collectionId);
        if (collectionId != null && validatedCollectionId == null) {
            return null;
        }
        String name1 = request.name1().orElse(defaults.getName1());
        String name2 = request.name2().orElse(defaults.getName2());
        int color = request.color().orElse(defaults.getColor());
        FrontierVisibilityData visibility = request.visibility()
                .map(ApiConverters::toFrontierVisibility)
                .orElseGet(defaults::getVisibilityData);
        BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(defaults::getBannerData);
        FrontierData.PathStyle pathStyle = frontierShape == FrontierShape.Path
                ? request.pathStyle().map(ApiConverters::toPathStyle).orElseGet(defaults::getPathStyle)
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
                                                         PlayerId owner,
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
                                                     PlayerId owner,
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
        return usesAuthoritativeCreateFlow(lifetime, MapFrontiersClient.isModOnServer());
    }

    static boolean usesAuthoritativeCreateFlow(TerritoryLifetime lifetime, boolean modOnServer) {
        return lifetime != TerritoryLifetime.SESSION_ONLY && modOnServer;
    }

    private static boolean usesAuthoritativeCollectionMutationFlow(CollectionData collection) {
        return usesAuthoritativeCollectionMutationFlow(collection, MapFrontiersClient.isModOnServer());
    }

    static boolean usesAuthoritativeCollectionMutationFlow(CollectionData collection, boolean modOnServer) {
        return collection.isPersistent() && modOnServer;
    }

    private boolean canMutateLocalCollection(CollectionData collection) {
        if (mc.player == null) {
            return false;
        }

        return collection.getPersonal() && collection.getOwner().equals(new PlayerId(mc.player.getUUID()));
    }

    private static boolean usesAuthoritativeMutationFlow(FrontierData frontier) {
        return usesAuthoritativeMutationFlow(frontier, MapFrontiersClient.isModOnServer());
    }

    static boolean usesAuthoritativeMutationFlow(FrontierData frontier, boolean modOnServer) {
        return frontier.isPersistent() && modOnServer;
    }

    static boolean applyOptimisticShareLocally(FrontierData frontier, PlayerId targetUser,
                                               PlayerId currentUser, Runnable postUpdated) {
        if (targetUser.equals(currentUser) || targetUser.equals(frontier.getOwner())
                || frontier.hasUserAccess(targetUser)) {
            return false;
        }

        frontier.addUserAccess(new FrontierUserAccess(targetUser, true));
        postUpdated.run();
        return true;
    }

    static boolean applyOptimisticRemoveSharedUserLocally(FrontierData frontier, PlayerId targetUser,
                                                          PlayerId currentUser, Runnable postUpdated) {
        if (targetUser.equals(currentUser) || !frontier.hasUserAccess(targetUser)) {
            return false;
        }

        frontier.removeUserAccess(targetUser);
        postUpdated.run();
        return true;
    }

    static boolean applyOptimisticSharedUserUpdateLocally(FrontierData frontier,
                                                          FrontierUserAccess desiredUserShared,
                                                          PlayerId currentUser,
                                                          Runnable postUpdated) {
        FrontierUserAccess currentSharedUser = frontier.getUserAccess(desiredUserShared.getPlayerId());
        if (currentSharedUser == null || currentSharedUser.getActions().equals(desiredUserShared.getActions())) {
            return false;
        }

        currentSharedUser.setActions(desiredUserShared.getActions());
        if (currentSharedUser.getPlayerId().equals(currentUser)) {
            frontier.setModified(new Date());
        }
        postUpdated.run();
        return true;
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

    private @Nullable FrontierOverlay resolveOptimisticSharingFrontier(UUID frontierId) {
        if (!MapFrontiersClient.isModOnServer() || mc.player == null) {
            return null;
        }

        FrontierOverlay frontier = personalManager.getFrontier(frontierId);
        if (frontier == null || !frontier.getPersonal() || frontier.isSessionOnly()) {
            return null;
        }

        PlayerId currentUser = new PlayerId(mc.player.getUUID());
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(
                MapFrontiersClient.getSettingsProfile(), frontier, currentUser);
        if (!actions.canShare || !frontier.checkUserAccess(currentUser, FrontierUserAccess.Action.UpdateSettings)) {
            return null;
        }

        return frontier;
    }

    private void postOptimisticSharingUpdated(FrontierOverlay frontier) {
        frontierEvents.postUpdated(frontier, mc.player.getId());
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

    private static FrontierUserAccess createSharedUser(UserRef user, @Nullable Set<FrontierSharePermission> permissions) {
        FrontierUserAccess sharedUser = new FrontierUserAccess(ApiConverters.toPlayerId(user), false);
        EnumSet<FrontierUserAccess.Action> actions = EnumSet.noneOf(FrontierUserAccess.Action.class);
        if (permissions != null) {
            for (FrontierSharePermission permission : permissions) {
                actions.add(ApiConverters.toSharedUserAction(permission));
            }
        }
        sharedUser.setActions(actions);
        return sharedUser;
    }
}
