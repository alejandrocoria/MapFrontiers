package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSharingUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
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
public class ServerFrontierOperationService {
    private static final int SYSTEM_ACTOR_ID = -1;

    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;

    public ServerFrontierOperationService(MinecraftServer server, FrontiersManager frontiersManager,
                                          FrontierPermissionEvaluator permissionEvaluator) {
        this.server = server;
        this.frontiersManager = frontiersManager;
        this.permissionEvaluator = permissionEvaluator;
    }

    public @Nullable FrontierData getFrontier(UUID frontierId) {
        return frontiersManager.getFrontierFromID(frontierId);
    }

    public @Nullable CollectionData getCollection(UUID collectionId) {
        return frontiersManager.getCollectionFromID(collectionId);
    }

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return frontiersManager.getAllGlobalFrontiers(dimension);
    }

    public ServerFrontierOperationResult createCollection(ServerPlayer player, CollectionData collectionData) {
        if (frontiersManager.getCollectionFromID(collectionData.getId()) != null) {
            return ServerFrontierOperationResult.ignored(null);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        CollectionData collection = new CollectionData(collectionData);
        collection.setOwner(playerUser);

        if (collection.getPersonal()) {
            Date now = new Date();
            collection.setCreated(now);
            collection.setModified(now);
            frontiersManager.addPersonalCollection(collection);
            return createdCollection(collection);
        }

        if (!permissionEvaluator.canCreateGlobalCollection(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        Date now = new Date();
        collection.setCreated(now);
        collection.setModified(now);
        frontiersManager.addGlobalCollection(collection);
        return createdCollection(collection);
    }

    public ServerFrontierOperationResult updateCollection(ServerPlayer player, UUID collectionId, CollectionData collectionData) {
        CollectionData collection = frontiersManager.getCollectionFromID(collectionId);
        if (collection == null) {
            return ServerFrontierOperationResult.notFound();
        }

        if (collection.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalCollection(player, collection)) {
                return ServerFrontierOperationResult.ignored(null);
            }
        } else if (!permissionEvaluator.canUpdateGlobalCollection(player, collection)) {
            return rejectedWithProfileRefresh(player, null);
        }

        collection.setName(collectionData.getName());
        collection.setColor(collectionData.getColor());
        collection.setModified(new Date());
        frontiersManager.markFrontiersUpdated();
        return updatedCollection(collection, null);
    }

    public ServerFrontierOperationResult deleteCollection(ServerPlayer player, UUID collectionId) {
        CollectionData collection = frontiersManager.getCollectionFromID(collectionId);
        if (collection == null) {
            return ServerFrontierOperationResult.notFound();
        }

        if (collection.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalCollection(player, collection)) {
                return ServerFrontierOperationResult.ignored(null);
            }
        } else if (!permissionEvaluator.canDeleteGlobalCollection(player, collection)) {
            return rejectedWithProfileRefresh(player, null);
        }

        Set<UUID> collectionRecipientsBefore = getCollectionRecipientIds(collection);
        List<FrontierData> affectedFrontiers = new ArrayList<>(frontiersManager.getFrontiersInCollection(collectionId));
        Date now = new Date();
        ArrayList<FrontierUpdateEmission> frontierUpdates = new ArrayList<>();
        for (FrontierData frontier : affectedFrontiers) {
            FrontierChange change = touchFrontierMembership(frontier, null, now);
            frontierUpdates.add(new FrontierUpdateEmission(frontier, change, player.getId()));
        }

        CollectionData removedCollection = frontiersManager.removeCollection(collectionId);
        if (removedCollection == null) {
            return ServerFrontierOperationResult.notFound();
        }

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(null);
        enqueueFrontierUpdates(result, frontierUpdates);
        enqueueCollectionDeleted(result, removedCollection, collectionRecipientsBefore);
        return result;
    }

    public ServerFrontierOperationResult createFrontier(ServerPlayer player,
                                                      UUID frontierId,
                                                      ResourceKey<Level> dimension,
                                                      boolean personal,
                                                      @Nullable UUID collectionId,
                                                      FrontierData.FrontierLifetime lifetime,
                                                      @Nullable String sourcePluginId,
                                                      @Nullable List<BlockPos> vertices,
                                                      @Nullable List<ChunkPos> chunks,
                                                      @Nullable List<BlockPos> points,
                                                      @Nullable FrontierData.PathStyle pathStyle) {
        if (lifetime != FrontierData.FrontierLifetime.PERSISTENT) {
            return rejectInvalidAuthoritativeFrontier(player, null,
                    "Rejected authoritative frontier creation because only PERSISTENT lifetime is supported on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, personal, lifetime);
        }

        if (personal) {
            FrontierData frontier = frontiersManager.createNewPersonalFrontier(frontierId, dimension, player, sourcePluginId,
                    vertices, chunks, points, pathStyle);
            CollectionData targetCollection = validateTargetCollection(player, frontier, collectionId);
            if (collectionId != null && targetCollection == null) {
                frontiersManager.deletePersonalFrontier(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                return ServerFrontierOperationResult.rejected(frontier);
            }
            if (targetCollection != null) {
                touchCollection(targetCollection, frontier.getModified());
            }
            return createdPersonalFrontier(frontier, player.getId(), null, targetCollection);
        }

        if (!permissionEvaluator.canCreateGlobalFrontier(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        FrontierData frontier = frontiersManager.createNewGlobalFrontier(frontierId, dimension, player, sourcePluginId,
                vertices, chunks, points, pathStyle);
        CollectionData targetCollection = validateTargetCollection(player, frontier, collectionId);
        if (collectionId != null && targetCollection == null) {
            frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
            return ServerFrontierOperationResult.rejected(frontier);
        }
        if (targetCollection != null) {
            touchCollection(targetCollection, frontier.getModified());
        }
        return createdGlobalFrontier(frontier, player.getId(), null, targetCollection);
    }

    public ServerFrontierOperationResult importPersonalFrontier(ServerPlayer player, FrontierData frontier) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(frontier.getId());

        if (!isAuthoritativePersonalFrontier(frontier)) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected personal frontier import because only persistent personal frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontier.getId(), frontier.getPersonal(), frontier.getLifetime());
        }

        if (currentFrontier != null) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (!frontier.getOwner().equals(playerUser)) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        frontier.removeAllUserShared();
        frontiersManager.importPersonalFrontier(frontier);
        return ServerFrontierOperationResult.success(frontier);
    }

    public ServerFrontierOperationResult importPersonalCollection(ServerPlayer player, CollectionData collection) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        CollectionData currentCollection = frontiersManager.getCollectionFromID(collection.getId());

        if (!collection.getPersonal()) {
            MapFrontiers.LOGGER.warn("Rejected personal collection import because only personal collections can be imported. collectionId={}",
                    collection.getId());
            return ServerFrontierOperationResult.rejected(null);
        }

        if (currentCollection != null) {
            return ServerFrontierOperationResult.ignored(null);
        }

        if (!collection.getOwner().equals(playerUser)) {
            return ServerFrontierOperationResult.ignored(null);
        }

        frontiersManager.importPersonalCollection(collection);
        return createdCollection(collection);
    }

    public ServerFrontierOperationResult createGlobalFrontier(FrontierData frontier) {
        if (!isAuthoritativeGlobalFrontier(frontier)) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier creation because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontier.getId(), frontier.getPersonal(), frontier.getLifetime()
            );
            return ServerFrontierOperationResult.rejected(frontier);
        }

        frontiersManager.addGlobalFrontier(frontier);
        return createdGlobalFrontier(frontier, SYSTEM_ACTOR_ID, null, null);
    }

    public ServerFrontierOperationResult updateFrontier(ServerPlayer player, UUID frontierId, FrontierChange change) {
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(frontierId);
        if (currentFrontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        CollectionData sourceCollection = currentFrontier.hasCollection()
                ? frontiersManager.getCollectionFromID(currentFrontier.getCollectionId())
                : null;
        Set<UUID> sourceCollectionRecipientsBefore = sourceCollection == null ? null : getCollectionRecipientIds(sourceCollection);
        CollectionData targetCollection = sourceCollection;
        Set<UUID> targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
        boolean collectionMembershipChanged = change.hasCollectionIdChange()
                && !equalsNullableUuid(currentFrontier.getCollectionId(), change.getCollectionIdChange().getCollectionId());

        if (currentFrontier.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalFrontier(player, currentFrontier)) {
                return ServerFrontierOperationResult.ignored(currentFrontier);
            }

            if (collectionMembershipChanged && !permissionEvaluator.canUpdatePersonalCollection(player, sourceCollection != null ? sourceCollection : buildPersonalCollectionContext(currentFrontier))) {
                return ServerFrontierOperationResult.ignored(currentFrontier);
            }

            if (collectionMembershipChanged) {
                targetCollection = validateTargetCollectionForAuthoritativeFrontier(currentFrontier, change.getCollectionIdChange().getCollectionId());
                if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                    return ServerFrontierOperationResult.ignored(currentFrontier);
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

            boolean updated = frontiersManager.applyPersonalFrontierChange(currentFrontier.getOwner(), frontierId, change);
            if (!updated) {
                return ServerFrontierOperationResult.notFound();
            }

            PacketFrontierUpdated frontierUpdatedPacket = new PacketFrontierUpdated(frontierId, currentFrontier.getDimension(),
                    true, new FrontierChange(change), player.getId());
            ServerFrontierOperationResult result = ServerFrontierOperationResult.success(currentFrontier);
            if (collectionMembershipChanged) {
                Date modified = currentFrontier.getModified();
                if (sourceCollection != null) {
                    touchCollection(sourceCollection, modified);
                }
                if (targetCollection != null && targetCollection != sourceCollection) {
                    touchCollection(targetCollection, modified);
                }
                if (sourceCollection != null) {
                    enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore);
                }
                if (targetCollection != null && targetCollection != sourceCollection) {
                    enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore);
                }
            }
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierUpdatedPacket, currentFrontier, server));
            return result;
        }

        if (!permissionEvaluator.canUpdateGlobalFrontier(player, currentFrontier)) {
            return rejectedWithProfileRefresh(player, currentFrontier);
        }

        if (collectionMembershipChanged) {
            targetCollection = validateTargetCollectionForAuthoritativeFrontier(currentFrontier, change.getCollectionIdChange().getCollectionId());
            if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                return ServerFrontierOperationResult.ignored(currentFrontier);
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

        boolean updated = frontiersManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerFrontierOperationResult.notFound();
        }

        ServerFrontierOperationResult result = updatedGlobalFrontier(currentFrontier, new FrontierChange(change), player.getId());
        if (collectionMembershipChanged) {
            Date modified = currentFrontier.getModified();
            if (sourceCollection != null) {
                touchCollection(sourceCollection, modified);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                touchCollection(targetCollection, modified);
            }
            if (sourceCollection != null) {
                enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore);
            }
        }
        return result;
    }

    public ServerFrontierOperationResult updateGlobalFrontier(UUID frontierId, FrontierChange change) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }
        if (!isAuthoritativeGlobalFrontier(frontier)) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier update because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, frontier.getPersonal(), frontier.getLifetime()
            );
            return ServerFrontierOperationResult.rejected(frontier);
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? frontiersManager.getCollectionFromID(frontier.getCollectionId())
                : null;
        Set<UUID> sourceCollectionRecipientsBefore = sourceCollection == null ? null : getCollectionRecipientIds(sourceCollection);
        CollectionData targetCollection = sourceCollection;
        Set<UUID> targetCollectionRecipientsBefore = sourceCollectionRecipientsBefore;
        boolean collectionMembershipChanged = change.hasCollectionIdChange()
                && !equalsNullableUuid(frontier.getCollectionId(), change.getCollectionIdChange().getCollectionId());

        if (collectionMembershipChanged) {
            targetCollection = validateTargetCollectionForAuthoritativeFrontier(frontier, change.getCollectionIdChange().getCollectionId());
            if (change.getCollectionIdChange().getCollectionId() != null && targetCollection == null) {
                return ServerFrontierOperationResult.rejected(frontier);
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

        boolean updated = frontiersManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerFrontierOperationResult.notFound();
        }

        ServerFrontierOperationResult result = updatedGlobalFrontier(frontier, new FrontierChange(change), SYSTEM_ACTOR_ID);
        if (collectionMembershipChanged) {
            Date modified = frontier.getModified();
            if (sourceCollection != null) {
                touchCollection(sourceCollection, modified);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                touchCollection(targetCollection, modified);
            }
            if (sourceCollection != null) {
                enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore);
            }
            if (targetCollection != null && targetCollection != sourceCollection) {
                enqueueCollectionVisibilityChange(result, targetCollection, targetCollectionRecipientsBefore);
            }
        }
        return result;
    }

    public ServerFrontierOperationResult deleteFrontier(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getPersonal()) {
            if (frontier.getOwner().equals(playerUser)) {
                CollectionData collection = frontier.hasCollection() ? frontiersManager.getCollectionFromID(frontier.getCollectionId()) : null;
                Set<UUID> collectionRecipientsBefore = collection == null ? null : getCollectionRecipientIds(collection);
                boolean deleted = frontiersManager.deletePersonalFrontier(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                if (!deleted) {
                    return ServerFrontierOperationResult.notFound();
                }

                if (frontier.getUsersShared() != null) {
                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                        frontiersManager.deletePersonalFrontier(userShared.getUser(), frontier.getDimension(), frontier.getId());
                    }
                }

                if (collection != null) {
                    touchCollection(collection, new Date());
                }

                ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
                if (collection != null) {
                    enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore);
                }
                result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierDeleted(frontier.getDimension(),
                        frontier.getId(), frontier.getPersonal(), player.getId()), frontier, server));
                return result;
            }

            frontier.removeUserShared(playerUser);
            frontiersManager.deletePersonalFrontier(playerUser, frontier.getDimension(), frontier.getId());

            PacketFrontierSharingUpdated frontierSharingUpdatedPacket = new PacketFrontierSharingUpdated(frontier.getId(), frontier.getDimension(),
                    FrontierSharingChange.fromFrontierData(frontier), player.getId());

            ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                    frontier.getPersonal(), player.getId()), player));
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierSharingUpdatedPacket, frontier, server));
            return result;
        }

        if (!permissionEvaluator.canDeleteGlobalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerFrontierOperationResult.notFound();
        }

        CollectionData collection = frontier.hasCollection() ? frontiersManager.getCollectionFromID(frontier.getCollectionId()) : null;
        ServerFrontierOperationResult result = deletedGlobalFrontier(frontier, player.getId());
        if (collection != null) {
            touchCollection(collection, new Date());
            enqueueCollectionVisibilityChange(result, collection, null);
        }
        return result;
    }

    public ServerFrontierOperationResult deleteGlobalFrontier(UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }
        if (!isAuthoritativeGlobalFrontier(frontier)) {
            MapFrontiers.LOGGER.warn(
                    "Rejected global frontier deletion because only persistent global frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontierId, frontier.getPersonal(), frontier.getLifetime()
            );
            return ServerFrontierOperationResult.rejected(frontier);
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerFrontierOperationResult.notFound();
        }

        CollectionData collection = frontier.hasCollection() ? frontiersManager.getCollectionFromID(frontier.getCollectionId()) : null;
        ServerFrontierOperationResult result = deletedGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
        if (collection != null) {
            touchCollection(collection, new Date());
            enqueueCollectionVisibilityChange(result, collection, null);
        }
        return result;
    }

    public ServerFrontierOperationResult changeFrontierToGlobal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? frontiersManager.getCollectionFromID(frontier.getCollectionId())
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

        boolean changed = frontiersManager.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return ServerFrontierOperationResult.notFound();
        }

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        if (sourceCollection != null) {
            touchCollection(sourceCollection, frontier.getModified());
            enqueueCollectionVisibilityChange(result, sourceCollection, sourceCollectionRecipientsBefore);
        }
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToGlobal(frontier.getId(), frontier.getModified()), relevantPlayers));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierCreated(frontier, player.getId()), server, relevantPlayers));
        return result;
    }

    public ServerFrontierOperationResult changeFrontierToPersonal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        CollectionData sourceCollection = frontier.hasCollection()
                ? frontiersManager.getCollectionFromID(frontier.getCollectionId())
                : null;

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

        boolean changed = frontiersManager.changeGlobalFrontierToPersonal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return ServerFrontierOperationResult.notFound();
        }

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        if (sourceCollection != null) {
            touchCollection(sourceCollection, frontier.getModified());
            enqueueCollectionVisibilityChange(result, sourceCollection, null);
        }
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToPersonal(frontier.getId(), frontier.getModified()), player));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, player.getId()), server, player));
        return result;
    }

    private ServerFrontierOperationResult createdPersonalFrontier(FrontierData frontier,
                                                                  int actorId,
                                                                  @Nullable Set<UUID> collectionRecipientsBefore,
                                                                  @Nullable CollectionData collection) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        if (collection != null) {
            enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore);
        }
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, actorId), frontier, server));
        return result;
    }

    private ServerFrontierOperationResult createdGlobalFrontier(FrontierData frontier,
                                                                int actorId,
                                                                @Nullable Set<UUID> collectionRecipientsBefore,
                                                                @Nullable CollectionData collection) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        if (collection != null) {
            enqueueCollectionVisibilityChange(result, collection, collectionRecipientsBefore);
        }
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierCreated(frontier, actorId), server));
        return result;
    }

    private ServerFrontierOperationResult updatedGlobalFrontier(FrontierData frontier, FrontierChange change, int actorId) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierUpdated(frontier.getId(), frontier.getDimension(),
                false, change, actorId), server));
        return result;
    }

    private ServerFrontierOperationResult deletedGlobalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, actorId), server));
        return result;
    }

    private ServerFrontierOperationResult createdCollection(CollectionData collection) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(null);
        if (collection.getPersonal()) {
            Set<UUID> recipients = getCollectionRecipientIds(collection);
            CollectionData payload = new CollectionData(collection);
            result.addNetworkAction(() -> sendToUsers(payload, recipients, true, false, false));
        } else {
            CollectionData payload = new CollectionData(collection);
            result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionCreated(payload), server));
        }
        return result;
    }

    private ServerFrontierOperationResult updatedCollection(CollectionData collection, @Nullable Set<UUID> recipientsBefore) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(null);
        enqueueCollectionVisibilityChange(result, collection, recipientsBefore);
        return result;
    }

    private void enqueueFrontierUpdates(ServerFrontierOperationResult result, List<FrontierUpdateEmission> updates) {
        for (FrontierUpdateEmission update : updates) {
            FrontierData frontier = update.frontier();
            PacketFrontierUpdated packet = new PacketFrontierUpdated(frontier.getId(), frontier.getDimension(),
                    frontier.getPersonal(), new FrontierChange(update.change()), update.actorId());
            result.addNetworkAction(() -> {
                if (frontier.getPersonal()) {
                    PacketHandler.sendToUsersWithAccess(packet, frontier, server);
                } else {
                    PacketHandler.sendToAll(packet, server);
                }
            });
        }
    }

    private void enqueueCollectionVisibilityChange(ServerFrontierOperationResult result,
                                                   CollectionData collection,
                                                   @Nullable Set<UUID> recipientsBefore) {
        if (!collection.getPersonal()) {
            CollectionData payload = new CollectionData(collection);
            result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionUpdated(payload), server));
            return;
        }

        Set<UUID> recipientsAfter = getCollectionRecipientIds(collection);
        LinkedHashSet<UUID> recipientsForCreate = new LinkedHashSet<>();
        LinkedHashSet<UUID> recipientsForUpdate = new LinkedHashSet<>();
        if (recipientsBefore == null) {
            recipientsForUpdate.addAll(recipientsAfter);
        } else {
            for (UUID recipientId : recipientsAfter) {
                if (recipientsBefore.contains(recipientId)) {
                    recipientsForUpdate.add(recipientId);
                } else {
                    recipientsForCreate.add(recipientId);
                }
            }
        }

        CollectionData payload = new CollectionData(collection);
        if (!recipientsForCreate.isEmpty()) {
            result.addNetworkAction(() -> sendToUsers(payload, recipientsForCreate, true, false, false));
        }
        if (!recipientsForUpdate.isEmpty()) {
            result.addNetworkAction(() -> sendToUsers(payload, recipientsForUpdate, false, true, false));
        }
    }

    private void enqueueCollectionDeleted(ServerFrontierOperationResult result, CollectionData collection, @Nullable Set<UUID> recipientsBefore) {
        if (!collection.getPersonal()) {
            UUID collectionId = collection.getId();
            result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketCollectionDeleted(collectionId), server));
            return;
        }

        if (recipientsBefore == null || recipientsBefore.isEmpty()) {
            return;
        }

        UUID collectionId = collection.getId();
        result.addNetworkAction(() -> sendToUsers(collectionId, recipientsBefore, false, false, true));
    }

    private void sendToUsers(CollectionData payload,
                             Set<UUID> recipientIds,
                             boolean create,
                             boolean update,
                             boolean delete) {
        for (UUID recipientId : recipientIds) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                continue;
            }

            if (create) {
                PacketHandler.sendTo(new PacketCollectionCreated(new CollectionData(payload)), recipient);
            }
            if (update) {
                PacketHandler.sendTo(new PacketCollectionUpdated(new CollectionData(payload)), recipient);
            }
        }
    }

    private void sendToUsers(UUID collectionId,
                             Set<UUID> recipientIds,
                             boolean create,
                             boolean update,
                             boolean delete) {
        for (UUID recipientId : recipientIds) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                continue;
            }

            if (delete) {
                PacketHandler.sendTo(new PacketCollectionDeleted(collectionId), recipient);
            }
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

        for (FrontierData frontier : frontiersManager.getFrontiersInCollection(collection.getId())) {
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

    private @Nullable CollectionData validateTargetCollection(ServerPlayer player, FrontierData frontier, @Nullable UUID collectionId) {
        if (collectionId == null) {
            frontier.setCollectionId(null);
            return null;
        }

        CollectionData collection = frontiersManager.getCollectionFromID(collectionId);
        if (collection == null) {
            MapFrontiers.LOGGER.warn("Rejected collection assignment because collection was not found. frontierId={}, collectionId={}",
                    frontier.getId(), collectionId);
            return null;
        }

        if (collection.getPersonal() != frontier.getPersonal()) {
            MapFrontiers.LOGGER.warn("Rejected collection assignment because collection type mismatched frontier type. frontierId={}, collectionId={}, frontierPersonal={}, collectionPersonal={}",
                    frontier.getId(), collectionId, frontier.getPersonal(), collection.getPersonal());
            return null;
        }

        if (frontier.getPersonal() && !collection.getOwner().equals(frontier.getOwner())) {
            MapFrontiers.LOGGER.warn("Rejected collection assignment because personal collection owner mismatched frontier owner. frontierId={}, collectionId={}, frontierOwner={}, collectionOwner={}",
                    frontier.getId(), collectionId, frontier.getOwner(), collection.getOwner());
            return null;
        }

        frontier.setCollectionId(collectionId);
        return collection;
    }

    private @Nullable CollectionData validateTargetCollectionForAuthoritativeFrontier(FrontierData frontier, @Nullable UUID collectionId) {
        if (collectionId == null) {
            return null;
        }

        CollectionData collection = frontiersManager.getCollectionFromID(collectionId);
        if (collection == null) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because collection was not found. frontierId={}, collectionId={}",
                    frontier.getId(), collectionId);
            return null;
        }

        if (collection.getPersonal() != frontier.getPersonal()) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because collection type mismatched frontier type. frontierId={}, collectionId={}, frontierPersonal={}, collectionPersonal={}",
                    frontier.getId(), collectionId, frontier.getPersonal(), collection.getPersonal());
            return null;
        }

        if (frontier.getPersonal() && !collection.getOwner().equals(frontier.getOwner())) {
            MapFrontiers.LOGGER.warn("Rejected authoritative collection assignment because personal collection owner mismatched frontier owner. frontierId={}, collectionId={}, frontierOwner={}, collectionOwner={}",
                    frontier.getId(), collectionId, frontier.getOwner(), collection.getOwner());
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
        frontiersManager.markFrontiersUpdated();
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

    private ServerFrontierOperationResult rejectedWithProfileRefresh(ServerPlayer player, @Nullable FrontierData frontier) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.rejected(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }

    private boolean isAuthoritativePersonalFrontier(FrontierData frontier) {
        return frontier.getPersonal() && frontier.isPersistent();
    }

    private boolean isAuthoritativeGlobalFrontier(FrontierData frontier) {
        return !frontier.getPersonal() && frontier.isPersistent();
    }

    private ServerFrontierOperationResult rejectInvalidAuthoritativeFrontier(ServerPlayer player,
                                                                             @Nullable FrontierData frontier,
                                                                             String message,
                                                                             Object... args) {
        MapFrontiers.LOGGER.warn(message, args);
        return rejectedWithProfileRefresh(player, frontier);
    }

    private record FrontierUpdateEmission(FrontierData frontier, FrontierChange change, int actorId) {
    }
}
