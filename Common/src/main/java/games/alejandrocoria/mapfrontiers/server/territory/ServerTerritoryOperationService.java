package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSharingUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFullFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.server.territory.collection.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerTerritoryOperationService {
    private static final int SYSTEM_ACTOR_ID = -1;

    private final MinecraftServer server;
    private final TerritoriesManager territoriesManager;
    private final TerritoryPermissionEvaluator permissionEvaluator;
    private final ServerFrontierEvents frontierEvents;
    private final ServerCollectionEvents collectionEvents;

    public ServerTerritoryOperationService(MinecraftServer server, TerritoriesManager territoriesManager,
                                           TerritoryPermissionEvaluator permissionEvaluator,
                                           ServerFrontierEvents frontierEvents,
                                           ServerCollectionEvents collectionEvents) {
        this.server = server;
        this.territoriesManager = territoriesManager;
        this.permissionEvaluator = permissionEvaluator;
        this.frontierEvents = frontierEvents;
        this.collectionEvents = collectionEvents;
    }

    public @Nullable FrontierData getFrontier(UUID frontierId) {
        return territoriesManager.getFrontierFromID(frontierId);
    }

    public @Nullable CollectionData getCollection(UUID collectionId) {
        return territoriesManager.getCollectionFromID(collectionId);
    }

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return territoriesManager.getAllGlobalFrontiers(dimension);
    }

    public Iterable<CollectionData> iterateGlobalCollections() {
        return territoriesManager.iterateGlobalCollections();
    }

    public ServerTerritoryOperationResult createCollection(ServerPlayer player, CollectionData collectionData) {
        if (territoriesManager.getCollectionFromID(collectionData.getId()) != null) {
            return ServerTerritoryOperationResult.ignored(null);
        }
        if (!collectionData.isPersistent()) {
            MapFrontiers.LOGGER.warn(
                    "Rejected collection creation because SESSION_ONLY collections are client-only. collectionId={}, personal={}, lifetime={}",
                    collectionData.getId(), collectionData.getPersonal(), collectionData.getLifetime()
            );
            return ServerTerritoryOperationResult.rejected(null);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        CollectionData collection = new CollectionData(collectionData);
        collection.setOwner(playerUser);
        collection.removeCopiedFromInfo();

        if (collection.getPersonal()) {
            Date now = new Date();
            collection.setCreated(now);
            collection.setModified(now);
            territoriesManager.addPersonalCollection(collection);
            return createdCollection(collection);
        }

        if (!permissionEvaluator.canCreateGlobalCollection(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        Date now = new Date();
        collection.setCreated(now);
        collection.setModified(now);
        territoriesManager.addGlobalCollection(collection);
        return createdCollection(collection);
    }

    public ServerTerritoryOperationResult createGlobalCollection(CollectionData collectionData) {
        if (territoriesManager.getCollectionFromID(collectionData.getId()) != null) {
            return ServerTerritoryOperationResult.ignored(null);
        }
        if (collectionData.getPersonal() || !collectionData.isPersistent()) {
            if (!collectionData.isPersistent()) {
                MapFrontiers.LOGGER.warn(
                        "Rejected global collection creation because SESSION_ONLY collections are client-only. collectionId={}, personal={}, lifetime={}",
                        collectionData.getId(), collectionData.getPersonal(), collectionData.getLifetime()
                );
            }
            return ServerTerritoryOperationResult.rejected(null);
        }

        CollectionData collection = new CollectionData(collectionData);
        collection.setPersonal(false);
        collection.removeCopiedFromInfo();
        Date now = new Date();
        collection.setCreated(now);
        collection.setModified(now);
        territoriesManager.addGlobalCollection(collection);
        return createdCollection(collection);
    }

    public ServerTerritoryOperationResult updateCollection(ServerPlayer player, UUID collectionId, CollectionData collectionData) {
        CollectionData collection = territoriesManager.getCollectionFromID(collectionId);
        if (collection == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        if (collection.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalCollection(player, collection)) {
                return ServerTerritoryOperationResult.ignored(null);
            }
        } else if (!permissionEvaluator.canUpdateGlobalCollection(player, collection)) {
            return rejectedWithProfileRefresh(player, null);
        }

        collection.setName(collectionData.getName());
        collection.setColor(collectionData.getColor());
        collection.setModified(new Date());
        territoriesManager.markDirty();
        return updatedCollection(collection, null, true);
    }

    public ServerTerritoryOperationResult updateGlobalCollection(UUID collectionId, CollectionData collectionData) {
        CollectionData collection = territoriesManager.getCollectionFromID(collectionId);
        if (collection == null || collection.getPersonal()) {
            return ServerTerritoryOperationResult.notFound();
        }

        collection.setName(collectionData.getName());
        collection.setColor(collectionData.getColor());
        collection.setModified(new Date());
        territoriesManager.markDirty();
        return updatedCollection(collection, null, true);
    }

    public ServerTerritoryOperationResult requestFrontierResync(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        if (frontier.getPersonal() && !canReceivePersonalFrontier(player, frontier)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFullFrontier(frontier), player));
        return result;
    }

    public ServerTerritoryOperationResult deleteCollection(ServerPlayer player, UUID collectionId) {
        CollectionData collection = territoriesManager.getCollectionFromID(collectionId);
        if (collection == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        if (collection.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalCollection(player, collection)) {
                return ServerTerritoryOperationResult.ignored(null);
            }
        } else if (!permissionEvaluator.canDeleteGlobalCollection(player, collection)) {
            return rejectedWithProfileRefresh(player, null);
        }

        Set<UUID> collectionRecipientsBefore = getCollectionRecipientIds(collection);
        List<FrontierData> affectedFrontiers = new ArrayList<>(territoriesManager.getFrontiersInCollection(collectionId));
        Date now = new Date();
        ArrayList<FrontierUpdateEmission> frontierUpdates = new ArrayList<>();
        for (FrontierData frontier : affectedFrontiers) {
            FrontierChange change = touchFrontierMembership(frontier, null, now);
            frontierUpdates.add(new FrontierUpdateEmission(frontier, change, player.getId()));
        }

        CollectionData removedCollection = territoriesManager.removeCollection(collectionId);
        if (removedCollection == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(null);
        enqueueFrontierUpdates(result, frontierUpdates);
        enqueueCollectionDeleted(result, removedCollection, collectionRecipientsBefore);
        return result;
    }

    public ServerTerritoryOperationResult deleteGlobalCollection(UUID collectionId) {
        CollectionData collection = territoriesManager.getCollectionFromID(collectionId);
        if (collection == null || collection.getPersonal()) {
            return ServerTerritoryOperationResult.notFound();
        }

        Set<UUID> collectionRecipientsBefore = getCollectionRecipientIds(collection);
        List<FrontierData> affectedFrontiers = new ArrayList<>(territoriesManager.getFrontiersInCollection(collectionId));
        Date now = new Date();
        ArrayList<FrontierUpdateEmission> frontierUpdates = new ArrayList<>();
        for (FrontierData frontier : affectedFrontiers) {
            FrontierChange change = touchFrontierMembership(frontier, null, now);
            frontierUpdates.add(new FrontierUpdateEmission(frontier, change, SYSTEM_ACTOR_ID));
        }

        CollectionData removedCollection = territoriesManager.removeCollection(collectionId);
        if (removedCollection == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(null);
        enqueueFrontierUpdates(result, frontierUpdates);
        enqueueCollectionDeleted(result, removedCollection, collectionRecipientsBefore);
        return result;
    }

    public ServerTerritoryOperationResult createFrontier(ServerPlayer player, FrontierCreateSpec createSpec) {
        FrontierCreateSpec serverSpec = createSpec.withOwner(permissionEvaluator.getPlayerUser(player));
        UUID frontierId = serverSpec.getFrontierId();
        boolean personal = serverSpec.isPersonal();
        TerritoryLifetime lifetime = serverSpec.getLifetime();

        if (lifetime != TerritoryLifetime.PERSISTENT) {
            return rejectInvalidAuthoritativeFrontier(player, null,
                    "Rejected authoritative frontier creation because only PERSISTENT lifetime is supported on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, personal, lifetime);
        }

        if (personal) {
            CollectionData targetCollection = validateTargetCollectionForCreateSpec(serverSpec);
            if (serverSpec.getCollectionId() != null && targetCollection == null) {
                return ServerTerritoryOperationResult.rejected(null);
            }
            FrontierData frontier = territoriesManager.createNewPersonalFrontier(serverSpec);
            if (targetCollection != null) {
                touchCollection(targetCollection, frontier.getModified());
            }
            return createdPersonalFrontier(frontier, player.getId(), null, targetCollection);
        }

        if (!permissionEvaluator.canCreateGlobalFrontier(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        CollectionData targetCollection = validateTargetCollectionForCreateSpec(serverSpec);
        if (serverSpec.getCollectionId() != null && targetCollection == null) {
            return ServerTerritoryOperationResult.rejected(null);
        }
        FrontierData frontier = territoriesManager.createNewGlobalFrontier(serverSpec);
        if (targetCollection != null) {
            touchCollection(targetCollection, frontier.getModified());
        }
        return createdGlobalFrontier(frontier, player.getId(), null, targetCollection);
    }

    public ServerTerritoryOperationResult importPersonalFrontier(ServerPlayer player, FrontierData frontier) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        FrontierData currentFrontier = territoriesManager.getFrontierFromID(frontier.getId());

        if (!isAuthoritativePersonalFrontier(frontier)) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected personal frontier import because only persistent personal frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontier.getId(), frontier.getPersonal(), frontier.getLifetime());
        }

        if (currentFrontier != null) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!frontier.getOwner().equals(playerUser)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        frontier.removeAllUserShared();
        territoriesManager.importPersonalFrontier(frontier);
        return createdImportedPersonalFrontier(frontier);
    }

    public ServerTerritoryOperationResult importPersonalCollection(ServerPlayer player, CollectionData collection) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        CollectionData currentCollection = territoriesManager.getCollectionFromID(collection.getId());

        if (!collection.getPersonal() || !collection.isPersistent()) {
            if (!collection.getPersonal()) {
                MapFrontiers.LOGGER.warn("Rejected personal collection import because only personal collections can be imported. collectionId={}",
                        collection.getId());
            } else {
                MapFrontiers.LOGGER.warn(
                        "Rejected personal collection import because SESSION_ONLY collections are client-only. collectionId={}, lifetime={}",
                        collection.getId(), collection.getLifetime()
                );
            }
            return ServerTerritoryOperationResult.rejected(null);
        }

        if (currentCollection != null) {
            return ServerTerritoryOperationResult.ignored(null);
        }

        if (!collection.getOwner().equals(playerUser)) {
            return ServerTerritoryOperationResult.ignored(null);
        }

        territoriesManager.importPersonalCollection(collection);
        return createdCollection(collection);
    }

    public ServerTerritoryOperationResult createGlobalFrontier(FrontierCreateSpec createSpec) {
        if (createSpec.isPersonal() || createSpec.getLifetime() != TerritoryLifetime.PERSISTENT) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier creation because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    createSpec.getFrontierId(), createSpec.isPersonal(), createSpec.getLifetime()
            );
            return ServerTerritoryOperationResult.rejected(null);
        }

        CollectionData targetCollection = validateTargetCollectionForCreateSpec(createSpec);
        if (createSpec.getCollectionId() != null && targetCollection == null) {
            return ServerTerritoryOperationResult.rejected(null);
        }
        FrontierData frontier = territoriesManager.createNewGlobalFrontier(createSpec);
        if (targetCollection != null) {
            touchCollection(targetCollection, frontier.getModified());
        }
        return createdGlobalFrontier(frontier, SYSTEM_ACTOR_ID, null, targetCollection);
    }

    public ServerTerritoryOperationResult updateFrontier(ServerPlayer player, UUID frontierId, FrontierChange change, long expectedSyncHash) {
        FrontierData currentFrontier = territoriesManager.getFrontierFromID(frontierId);
        if (currentFrontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        CollectionData sourceCollection = currentFrontier.hasCollection()
                ? territoriesManager.getCollectionFromID(currentFrontier.getCollectionId())
                : null;
        Set<UUID> sourceCollectionRecipientsBefore = sourceCollection == null ? null : getCollectionRecipientIds(sourceCollection);
        CollectionData targetCollection = sourceCollection;
        Set<UUID> targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
        boolean collectionMembershipChanged = change.hasCollectionIdChange()
                && !equalsNullableUuid(currentFrontier.getCollectionId(), change.getCollectionIdChange().getCollectionId());

        if (currentFrontier.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalFrontier(player, currentFrontier)) {
                return ServerTerritoryOperationResult.ignored(currentFrontier);
            }

            if (collectionMembershipChanged && !permissionEvaluator.canUpdatePersonalCollection(player, sourceCollection != null ? sourceCollection : buildPersonalCollectionContext(currentFrontier))) {
                return ServerTerritoryOperationResult.ignored(currentFrontier);
            }

            if (collectionMembershipChanged) {
                targetCollection = validateTargetCollectionAssignment(currentFrontier.getId(), currentFrontier.getPersonal(),
                        currentFrontier.getLifetime(), currentFrontier.getOwner(), change.getCollectionIdChange().getCollectionId());
                if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                    return ServerTerritoryOperationResult.ignored(currentFrontier);
                }
                if (targetCollection != null && sourceCollection != null && targetCollection.getId().equals(sourceCollection.getId())) {
                    targetCollection = sourceCollection;
                    targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
                    collectionMembershipChanged = false;
                } else if (targetCollection != null) {
                    targetCollectionRecipientsBefore = getCollectionRecipientIds(targetCollection);
                } else {
                    targetCollectionRecipientsBefore = null;
                }
            }

            boolean updated = territoriesManager.applyPersonalFrontierChange(currentFrontier.getOwner(), frontierId, change);
            if (!updated) {
                return ServerTerritoryOperationResult.notFound();
            }

            long authoritativeSyncHash = currentFrontier.computeSyncHash();
            boolean syncHashMismatch = logSyncHashMismatchIfNeeded(player, currentFrontier, expectedSyncHash, authoritativeSyncHash);
            PacketFrontierUpdated frontierUpdatedPacket = new PacketFrontierUpdated(frontierId, currentFrontier.getDimension(),
                    true, new FrontierChange(change), authoritativeSyncHash, player.getId());
            ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(currentFrontier);
            if (syncHashMismatch) {
                result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFullFrontier(currentFrontier), player));
            }
            if (collectionMembershipChanged) {
                Date modified = currentFrontier.getModified();
                if (sourceCollection != null) {
                    touchCollection(sourceCollection, modified);
                }
                if (targetCollection != null && targetCollection != sourceCollection) {
                    touchCollection(targetCollection, modified);
                }
                if (sourceCollection != null) {
                    enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore, false);
                }
                if (targetCollection != null && targetCollection != sourceCollection) {
                    enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore, false);
                }
            }
            frontierEvents.postUpdated(currentFrontier);
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierUpdatedPacket, currentFrontier, server));
            return result;
        }

        if (!permissionEvaluator.canUpdateGlobalFrontier(player, currentFrontier)) {
            return rejectedWithProfileRefresh(player, currentFrontier);
        }

        if (collectionMembershipChanged) {
            targetCollection = validateTargetCollectionAssignment(currentFrontier.getId(), currentFrontier.getPersonal(),
                    currentFrontier.getLifetime(), currentFrontier.getOwner(), change.getCollectionIdChange().getCollectionId());
            if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                return ServerTerritoryOperationResult.ignored(currentFrontier);
            }
            if (targetCollection != null && sourceCollection != null && targetCollection.getId().equals(sourceCollection.getId())) {
                targetCollection = sourceCollection;
                targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
                collectionMembershipChanged = false;
            } else if (targetCollection != null) {
                targetCollectionRecipientsBefore = getCollectionRecipientIds(targetCollection);
            } else {
                targetCollectionRecipientsBefore = null;
            }
        }

        boolean updated = territoriesManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerTerritoryOperationResult.notFound();
        }

        long authoritativeSyncHash = currentFrontier.computeSyncHash();
        boolean syncHashMismatch = logSyncHashMismatchIfNeeded(player, currentFrontier, expectedSyncHash, authoritativeSyncHash);
        ServerTerritoryOperationResult result = updatedGlobalFrontier(currentFrontier, new FrontierChange(change), authoritativeSyncHash, player.getId());
        if (syncHashMismatch) {
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFullFrontier(currentFrontier), player));
        }
        if (collectionMembershipChanged) {
            Date modified = currentFrontier.getModified();
            if (sourceCollection != null) {
                touchCollection(sourceCollection, modified);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                touchCollection(targetCollection, modified);
            }
            if (sourceCollection != null) {
                enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore, false);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore, false);
            }
        }
        return result;
    }

    public ServerTerritoryOperationResult updateGlobalFrontier(UUID frontierId, FrontierChange change) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }
        if (!isAuthoritativeGlobalFrontier(frontier)) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier update because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, frontier.getPersonal(), frontier.getLifetime()
            );
            return ServerTerritoryOperationResult.rejected(frontier);
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? territoriesManager.getCollectionFromID(frontier.getCollectionId())
                : null;
        Set<UUID> sourceCollectionRecipientsBefore = sourceCollection == null ? null : getCollectionRecipientIds(sourceCollection);
        CollectionData targetCollection = sourceCollection;
        Set<UUID> targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
        boolean collectionMembershipChanged = change.hasCollectionIdChange()
                && !equalsNullableUuid(frontier.getCollectionId(), change.getCollectionIdChange().getCollectionId());

        if (collectionMembershipChanged) {
            targetCollection = validateTargetCollectionAssignment(frontier.getId(), frontier.getPersonal(),
                    frontier.getLifetime(), frontier.getOwner(), change.getCollectionIdChange().getCollectionId());
            if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                return ServerTerritoryOperationResult.rejected(frontier);
            }
            if (targetCollection != null && sourceCollection != null && targetCollection.getId().equals(sourceCollection.getId())) {
                targetCollection = sourceCollection;
                targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
                collectionMembershipChanged = false;
            } else if (targetCollection != null) {
                targetCollectionRecipientsBefore = getCollectionRecipientIds(targetCollection);
            } else {
                targetCollectionRecipientsBefore = null;
            }
        }

        boolean updated = territoriesManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerTerritoryOperationResult.notFound();
        }

        ServerTerritoryOperationResult result = updatedGlobalFrontier(frontier, new FrontierChange(change), frontier.computeSyncHash(), SYSTEM_ACTOR_ID);
        if (collectionMembershipChanged) {
            Date modified = frontier.getModified();
            if (sourceCollection != null) {
                touchCollection(sourceCollection, modified);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                touchCollection(targetCollection, modified);
            }
            if (sourceCollection != null) {
                enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore, false);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore, false);
            }
        }
        return result;
    }

    public ServerTerritoryOperationResult deleteFrontier(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getPersonal()) {
            if (frontier.getOwner().equals(playerUser)) {
                CollectionData collection = frontier.hasCollection() ? territoriesManager.getCollectionFromID(frontier.getCollectionId()) : null;
                Set<UUID> collectionRecipientsBefore = collection == null ? null : getCollectionRecipientIds(collection);
                boolean deleted = territoriesManager.deleteOwnedPersonalFrontier(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                if (!deleted) {
                    return ServerTerritoryOperationResult.notFound();
                }

                if (collection != null) {
                    touchCollection(collection, new Date());
                }

                ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
                if (collection != null) {
                    enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore, false);
                }
                result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierDeleted(frontier.getDimension(),
                        frontier.getId(), frontier.getPersonal(), player.getId()), frontier, server));
                frontierEvents.postDeleted(frontier);
                return result;
            }

            if (!territoriesManager.removePersonalFrontierShare(frontierId, playerUser)) {
                return ServerTerritoryOperationResult.ignored(frontier);
            }

            PacketFrontierSharingUpdated frontierSharingUpdatedPacket = new PacketFrontierSharingUpdated(frontier.getId(), frontier.getDimension(),
                    FrontierSharingChange.fromFrontierData(frontier), player.getId());

            ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                    frontier.getPersonal(), player.getId()), player));
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierSharingUpdatedPacket, frontier, server));
            frontierEvents.postUpdated(frontier);
            return result;
        }

        if (!permissionEvaluator.canDeleteGlobalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        boolean deleted = territoriesManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerTerritoryOperationResult.notFound();
        }

        CollectionData collection = frontier.hasCollection() ? territoriesManager.getCollectionFromID(frontier.getCollectionId()) : null;
        ServerTerritoryOperationResult result = deletedGlobalFrontier(frontier, player.getId());
        if (collection != null) {
            touchCollection(collection, new Date());
            enqueueCollectionVisibilityChange(result, collection, null, false);
        }
        return result;
    }

    public ServerTerritoryOperationResult deleteGlobalFrontier(UUID frontierId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }
        if (!isAuthoritativeGlobalFrontier(frontier)) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier deletion because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, frontier.getPersonal(), frontier.getLifetime()
            );
            return ServerTerritoryOperationResult.rejected(frontier);
        }

        boolean deleted = territoriesManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerTerritoryOperationResult.notFound();
        }

        CollectionData collection = frontier.hasCollection() ? territoriesManager.getCollectionFromID(frontier.getCollectionId()) : null;
        ServerTerritoryOperationResult result = deletedGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
        if (collection != null) {
            touchCollection(collection, new Date());
            enqueueCollectionVisibilityChange(result, collection, null, false);
        }
        return result;
    }

    public ServerTerritoryOperationResult changeFrontierToGlobal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? territoriesManager.getCollectionFromID(frontier.getCollectionId())
                : null;
        Set<UUID> sourceCollectionRecipientsBefore = sourceCollection == null ? null : getCollectionRecipientIds(sourceCollection);

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (!frontier.getPersonal() || !frontier.getOwner().equals(playerUser)) {
            return rejectedWithProfileRefresh(player, frontier);
        }
        if (frontier.isSessionOnly()) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected changeFrontierToGlobal because session-only frontiers cannot exist on the server. frontierId={}, lifetime={}",
                    frontierId, frontier.getLifetime());
        }
        if (sourceCollection != null) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected changeFrontierToGlobal because frontiers in a collection cannot change scope without clearing collection membership first. frontierId={}, collectionId={}",
                    frontierId, sourceCollection.getId());
        }

        List<ServerPlayer> relevantPlayers = new ArrayList<>();
        relevantPlayers.add(player);
        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                if (!userShared.isPending()) {
                    ServerPlayer otherPlayer = server.getPlayerList().getPlayer(userShared.getUser().uuid);
                    if (otherPlayer != null) {
                        relevantPlayers.add(otherPlayer);
                    }
                }
            }
        }

        boolean changed = territoriesManager.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return ServerTerritoryOperationResult.notFound();
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (sourceCollection != null) {
            touchCollection(sourceCollection, frontier.getModified());
            enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore, false);
        }
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToGlobal(frontier.getId(), frontier.getModified()), relevantPlayers));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierCreated(frontier, player.getId()), server, relevantPlayers));
        frontierEvents.postCreated(frontier);
        return result;
    }

    public ServerTerritoryOperationResult changeFrontierToPersonal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerTerritoryOperationResult.notFound();
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? territoriesManager.getCollectionFromID(frontier.getCollectionId())
                : null;
        FrontierData previousGlobalFrontier = new FrontierData(frontier);

        if (frontier.getPersonal()) {
            return rejectedWithProfileRefresh(player, frontier);
        }
        if (!frontier.isPersistent()) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected changeFrontierToPersonal because only persistent global frontiers can exist on the server. frontierId={}, lifetime={}",
                    frontierId, frontier.getLifetime());
        }

        if (!permissionEvaluator.canDeleteGlobalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }
        if (sourceCollection != null) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected changeFrontierToPersonal because frontiers in a collection cannot change scope without clearing collection membership first. frontierId={}, collectionId={}",
                    frontierId, sourceCollection.getId());
        }

        boolean changed = territoriesManager.changeGlobalFrontierToPersonal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return ServerTerritoryOperationResult.notFound();
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (sourceCollection != null) {
            touchCollection(sourceCollection, frontier.getModified());
            enqueueCollectionVisibilityChange(result, sourceCollection, null, false);
        }
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToPersonal(frontier.getId(), frontier.getModified()), player));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, player.getId()), server, player));
        frontierEvents.postDeleted(previousGlobalFrontier);
        return result;
    }

    private ServerTerritoryOperationResult createdPersonalFrontier(FrontierData frontier,
                                                                   int actorId,
                                                                   @Nullable Set<UUID> collectionRecipientsBefore,
                                                                   @Nullable CollectionData collection) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (collection != null) {
            enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore, false);
        }
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, actorId), frontier, server));
        frontierEvents.postCreated(frontier);
        return result;
    }

    private ServerTerritoryOperationResult createdGlobalFrontier(FrontierData frontier,
                                                                 int actorId,
                                                                 @Nullable Set<UUID> collectionRecipientsBefore,
                                                                 @Nullable CollectionData collection) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (collection != null) {
            enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore, false);
        }
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierCreated(frontier, actorId), server));
        frontierEvents.postCreated(frontier);
        return result;
    }

    private ServerTerritoryOperationResult createdImportedPersonalFrontier(FrontierData frontier) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        frontierEvents.postCreated(frontier);
        return result;
    }

    private ServerTerritoryOperationResult updatedGlobalFrontier(FrontierData frontier, FrontierChange change, long authoritativeSyncHash, int actorId) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierUpdated(frontier.getId(), frontier.getDimension(),
                false, change, authoritativeSyncHash, actorId), server));
        frontierEvents.postUpdated(frontier);
        return result;
    }

    private ServerTerritoryOperationResult deletedGlobalFrontier(FrontierData frontier, int actorId) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, actorId), server));
        frontierEvents.postDeleted(frontier);
        return result;
    }

    private ServerTerritoryOperationResult createdCollection(CollectionData collection) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.successCollection(collection);
        if (collection.getPersonal()) {
            Set<UUID> recipients = getCollectionRecipientIds(collection);
            CollectionData payload = new CollectionData(collection);
            result.addNetworkAction(() -> sendCollectionCreatedToUsers(payload, recipients));
        } else {
            CollectionData payload = new CollectionData(collection);
            result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionCreated(payload), server));
        }
        collectionEvents.postCreated(collection);
        return result;
    }

    private ServerTerritoryOperationResult updatedCollection(CollectionData collection, @Nullable Set<UUID> recipientsBefore, boolean metadataChanged) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.successCollection(collection);
        enqueueCollectionVisibilityChange(result, collection, recipientsBefore, metadataChanged);
        return result;
    }

    private void enqueueFrontierUpdates(ServerTerritoryOperationResult result, List<FrontierUpdateEmission> updates) {
        for (FrontierUpdateEmission update : updates) {
            FrontierData frontier = update.frontier();
            frontierEvents.postUpdated(frontier);
            PacketFrontierUpdated packet = new PacketFrontierUpdated(frontier.getId(), frontier.getDimension(),
                    frontier.getPersonal(), new FrontierChange(update.change()), frontier.computeSyncHash(), update.actorId());
            result.addNetworkAction(() -> {
                if (frontier.getPersonal()) {
                    PacketHandler.sendToUsersWithAccess(packet, frontier, server);
                } else {
                    PacketHandler.sendToAll(packet, server);
                }
            });
        }
    }

    private void enqueueCollectionVisibilityChange(ServerTerritoryOperationResult result,
                                                   CollectionData collection,
                                                   @Nullable Set<UUID> recipientsBefore,
                                                   boolean metadataChanged) {
        collectionEvents.postUpdated(collection);
        if (!collection.getPersonal()) {
            if (metadataChanged) {
                CollectionData payload = new CollectionData(collection);
                result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionUpdated(payload), server));
            }
            return;
        }

        Set<UUID> recipientsAfter = getCollectionRecipientIds(collection);
        LinkedHashSet<UUID> recipientsForCreate = new LinkedHashSet<>();
        LinkedHashSet<UUID> recipientsForUpdate = new LinkedHashSet<>();
        if (metadataChanged && recipientsBefore == null) {
            recipientsForUpdate.addAll(recipientsAfter);
        } else {
            for (UUID recipientId : recipientsAfter) {
                if (metadataChanged && recipientsBefore != null && recipientsBefore.contains(recipientId)) {
                    recipientsForUpdate.add(recipientId);
                } else {
                    recipientsForCreate.add(recipientId);
                }
            }
        }

        CollectionData payload = new CollectionData(collection);
        if (!recipientsForCreate.isEmpty()) {
            result.addNetworkAction(() -> sendCollectionCreatedToUsers(payload, recipientsForCreate));
        }
        if (!recipientsForUpdate.isEmpty()) {
            result.addNetworkAction(() -> sendCollectionUpdatedToUsers(payload, recipientsForUpdate));
        }
    }

    private void enqueueCollectionDeleted(ServerTerritoryOperationResult result, CollectionData collection, @Nullable Set<UUID> recipientsBefore) {
        collectionEvents.postDeleted(collection);
        if (!collection.getPersonal()) {
            UUID collectionId = collection.getId();
            result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionDeleted(collectionId), server));
            return;
        }

        if (recipientsBefore == null || recipientsBefore.isEmpty()) {
            return;
        }

        UUID collectionId = collection.getId();
        result.addNetworkAction(() -> sendCollectionDeletedToUsers(collectionId, recipientsBefore));
    }

    private void sendCollectionCreatedToUsers(CollectionData payload, Set<UUID> recipientIds) {
        for (UUID recipientId : recipientIds) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                continue;
            }

            PacketHandler.sendTo(new PacketCollectionCreated(new CollectionData(payload)), recipient);
        }
    }

    private void sendCollectionUpdatedToUsers(CollectionData payload, Set<UUID> recipientIds) {
        for (UUID recipientId : recipientIds) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                continue;
            }

            PacketHandler.sendTo(new PacketCollectionUpdated(new CollectionData(payload)), recipient);
        }
    }

    private void sendCollectionDeletedToUsers(UUID collectionId, Set<UUID> recipientIds) {
        for (UUID recipientId : recipientIds) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                continue;
            }

            PacketHandler.sendTo(new PacketCollectionDeleted(collectionId), recipient);
        }
    }

    private Set<UUID> getCollectionRecipientIds(CollectionData collection) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        if (!collection.getPersonal()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                recipients.add(player.getUUID());
            }
            return recipients;
        }

        if (collection.getOwner().uuid != null) {
            recipients.add(collection.getOwner().uuid);
        }

        for (FrontierData frontier : territoriesManager.getFrontiersInCollection(collection.getId())) {
            if (frontier.getUsersShared() == null) {
                continue;
            }

            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                if (!userShared.isPending() && userShared.getUser().uuid != null) {
                    recipients.add(userShared.getUser().uuid);
                }
            }
        }

        return recipients;
    }

    private @Nullable CollectionData validateTargetCollectionForCreateSpec(FrontierCreateSpec createSpec) {
        return validateTargetCollectionAssignment(createSpec.getFrontierId(), createSpec.isPersonal(), createSpec.getLifetime(),
                createSpec.getOwner(), createSpec.getCollectionId());
    }

    private @Nullable CollectionData validateTargetCollectionAssignment(UUID frontierId,
                                                                        boolean personal,
                                                                        TerritoryLifetime lifetime,
                                                                        SettingsUser owner,
                                                                        @Nullable UUID collectionId) {
        if (collectionId == null) {
            return null;
        }

        CollectionData collection = territoriesManager.getCollectionFromID(collectionId);
        if (collection == null) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because collection was not found. frontierId={}, collectionId={}",
                    frontierId, collectionId);
            return null;
        }

        if (collection.getPersonal() != personal) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because collection type mismatched frontier type. frontierId={}, collectionId={}, frontierPersonal={}, collectionPersonal={}",
                    frontierId, collectionId, personal, collection.getPersonal());
            return null;
        }
        if (collection.getLifetime() != lifetime) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because collection lifetime mismatched frontier lifetime. frontierId={}, collectionId={}, frontierLifetime={}, collectionLifetime={}",
                    frontierId, collectionId, lifetime, collection.getLifetime());
            return null;
        }

        if (personal && !collection.getOwner().equals(owner)) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because personal collection owner mismatched frontier owner. frontierId={}, collectionId={}, frontierOwner={}, collectionOwner={}",
                    frontierId, collectionId, owner, collection.getOwner());
            return null;
        }

        return collection;
    }

    private FrontierChange touchFrontierMembership(FrontierData frontier, @Nullable UUID collectionId, Date modified) {
        frontier.setModified(modified);
        frontier.setCollectionId(collectionId);
        FrontierChange change = new FrontierChange();
        change.setCollectionId(collectionId);
        change.setModifiedTime(modified.getTime());
        return change;
    }

    private void touchCollection(CollectionData collection, @Nullable Date modified) {
        collection.setModified(modified == null ? new Date() : modified);
        territoriesManager.markDirty();
    }

    private CollectionData buildPersonalCollectionContext(FrontierData frontier) {
        CollectionData collection = new CollectionData();
        collection.setPersonal(true);
        collection.setOwner(frontier.getOwner());
        return collection;
    }

    private static boolean equalsNullableUuid(@Nullable UUID left, @Nullable UUID right) {
        if (left == null) {
            return right == null;
        }

        return left.equals(right);
    }

    private boolean logSyncHashMismatchIfNeeded(ServerPlayer player, FrontierData frontier, long expectedSyncHash, long authoritativeSyncHash) {
        if (expectedSyncHash == authoritativeSyncHash) {
            return false;
        }

        MapFrontiers.LOGGER.warn(
                "Frontier sync hash mismatch after server update apply. frontierId={}, player={}, expectedHash={}, authoritativeHash={}",
                frontier.getId(), player.getName().getString(), expectedSyncHash, authoritativeSyncHash
        );
        return true;
    }

    private boolean canReceivePersonalFrontier(ServerPlayer player, FrontierData frontier) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getOwner().equals(playerUser)) {
            return true;
        }

        SettingsUserShared userShared = frontier.getUserShared(playerUser);
        return userShared != null && !userShared.isPending();
    }

    private ServerTerritoryOperationResult rejectedWithProfileRefresh(ServerPlayer player, @Nullable FrontierData frontier) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.rejected(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }

    private boolean isAuthoritativePersonalFrontier(FrontierData frontier) {
        return frontier.getPersonal() && frontier.isPersistent();
    }

    private boolean isAuthoritativeGlobalFrontier(FrontierData frontier) {
        return !frontier.getPersonal() && frontier.isPersistent();
    }

    private ServerTerritoryOperationResult rejectInvalidAuthoritativeFrontier(ServerPlayer player,
                                                                              @Nullable FrontierData frontier,
                                                                              String message,
                                                                              Object... args) {
        MapFrontiers.LOGGER.warn(message, args);
        return rejectedWithProfileRefresh(player, frontier);
    }

    private record FrontierUpdateEmission(FrontierData frontier, FrontierChange change, int actorId) {
    }
}
