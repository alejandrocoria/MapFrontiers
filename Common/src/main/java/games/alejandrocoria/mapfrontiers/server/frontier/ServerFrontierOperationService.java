package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
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
import java.util.List;
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

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return frontiersManager.getAllGlobalFrontiers(dimension);
    }

    public ServerFrontierOperationResult createFrontier(ServerPlayer player,
                                                      UUID frontierId,
                                                      ResourceKey<Level> dimension,
                                                      boolean personal,
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
            return createdPersonalFrontier(frontier, player.getId());
        }

        if (!permissionEvaluator.canCreateGlobalFrontier(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        FrontierData frontier = frontiersManager.createNewGlobalFrontier(frontierId, dimension, player, sourcePluginId,
                vertices, chunks, points, pathStyle);
        return createdGlobalFrontier(frontier, player.getId());
    }

    public ServerFrontierOperationResult importPersonalFrontier(ServerPlayer player, FrontierData frontier) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(frontier.getId());

        if (!isAuthoritativePersonalFrontier(frontier)) {
            return rejectInvalidAuthoritativeFrontier(player, frontier,
                    "Rejected personal frontier import because only persistent personal frontiers can exist on the server. frontierId={}, personal={}, lifetime={}",
                    frontier.getId(), frontier.getPersonal(), frontier.getLifetime());
        }

        if (currentFrontier != null || !frontier.getOwner().equals(playerUser)) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        frontier.removeAllUserShared();
        frontiersManager.addPersonalFrontier(frontier);
        return ServerFrontierOperationResult.success(frontier);
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
        return createdGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
    }

    public ServerFrontierOperationResult updateFrontier(ServerPlayer player, UUID frontierId, FrontierChange change) {
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(frontierId);
        if (currentFrontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        if (currentFrontier.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalFrontier(player, currentFrontier)) {
                return ServerFrontierOperationResult.ignored(currentFrontier);
            }

            boolean updated = frontiersManager.applyPersonalFrontierChange(currentFrontier.getOwner(), frontierId, change);
            if (!updated) {
                return ServerFrontierOperationResult.notFound();
            }

            if (currentFrontier.getUsersShared() != null) {
                for (SettingsUserShared userShared : currentFrontier.getUsersShared()) {
                    frontiersManager.applyPersonalFrontierChange(userShared.getUser(), frontierId, new FrontierChange(change));
                }
            }

            PacketFrontierUpdated frontierUpdatedPacket = new PacketFrontierUpdated(frontierId, currentFrontier.getDimension(),
                    true, new FrontierChange(change), player.getId());
            ServerFrontierOperationResult result = ServerFrontierOperationResult.success(currentFrontier);
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierUpdatedPacket, currentFrontier, server));
            return result;
        }

        if (!permissionEvaluator.canUpdateGlobalFrontier(player, currentFrontier)) {
            return rejectedWithProfileRefresh(player, currentFrontier);
        }

        boolean updated = frontiersManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerFrontierOperationResult.notFound();
        }

        return updatedGlobalFrontier(currentFrontier, new FrontierChange(change), player.getId());
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

        boolean updated = frontiersManager.applyGlobalFrontierChange(frontierId, change);
        if (!updated) {
            return ServerFrontierOperationResult.notFound();
        }

        return updatedGlobalFrontier(frontier, new FrontierChange(change), SYSTEM_ACTOR_ID);
    }

    public ServerFrontierOperationResult deleteFrontier(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getPersonal()) {
            if (frontier.getOwner().equals(playerUser)) {
                boolean deleted = frontiersManager.deletePersonalFrontier(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                if (!deleted) {
                    return ServerFrontierOperationResult.notFound();
                }

                if (frontier.getUsersShared() != null) {
                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                        frontiersManager.deletePersonalFrontier(userShared.getUser(), frontier.getDimension(), frontier.getId());
                    }
                }

                ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
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

        return deletedGlobalFrontier(frontier, player.getId());
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

        return deletedGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
    }

    public ServerFrontierOperationResult changeFrontierToGlobal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

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
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToGlobal(frontier.getId(), frontier.getModified()), relevantPlayers));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierCreated(frontier, player.getId()), server, relevantPlayers));
        return result;
    }

    public ServerFrontierOperationResult changeFrontierToPersonal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierOperationResult.notFound();
        }

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
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToPersonal(frontier.getId(), frontier.getModified()), player));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, player.getId()), server, player));
        return result;
    }

    private ServerFrontierOperationResult createdPersonalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, actorId), frontier, server));
        return result;
    }

    private ServerFrontierOperationResult createdGlobalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
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
}
