package games.alejandrocoria.mapfrontiers.common.frontier.server;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
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
public class ServerFrontierCommandService {
    private static final int SYSTEM_ACTOR_ID = -1;

    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;

    public ServerFrontierCommandService(MinecraftServer server, FrontiersManager frontiersManager,
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

    public ServerFrontierCommandResult createFrontier(ServerPlayer player,
                                                      UUID frontierId,
                                                      ResourceKey<Level> dimension,
                                                      boolean personal,
                                                      @Nullable String sourcePluginId,
                                                      @Nullable List<BlockPos> vertices,
                                                      @Nullable List<ChunkPos> chunks) {
        if (personal) {
            FrontierData frontier = frontiersManager.createNewPersonalFrontier(frontierId, dimension, player, sourcePluginId, vertices, chunks);
            return createdPersonalFrontier(frontier, player.getId());
        }

        if (!permissionEvaluator.canCreateGlobalFrontier(player)) {
            return rejectedWithProfileRefresh(player, null);
        }

        FrontierData frontier = frontiersManager.createNewGlobalFrontier(frontierId, dimension, player, sourcePluginId, vertices, chunks);
        return createdGlobalFrontier(frontier, player.getId());
    }

    public ServerFrontierCommandResult importPersonalFrontier(ServerPlayer player, FrontierData frontier) {
        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(frontier.getId());

        if (currentFrontier != null || !frontier.getPersonal() || !frontier.getOwner().equals(playerUser)) {
            return ServerFrontierCommandResult.ignored(frontier);
        }

        frontier.removeAllUserShared();
        frontier.removeChange(FrontierData.Change.Shared);
        frontiersManager.addPersonalFrontier(frontier);
        return ServerFrontierCommandResult.success(frontier);
    }

    public ServerFrontierCommandResult createGlobalFrontier(FrontierData frontier) {
        frontiersManager.addGlobalFrontier(frontier);
        return createdGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
    }

    public ServerFrontierCommandResult updateFrontier(ServerPlayer player, FrontierData requestedFrontier) {
        FrontierData currentFrontier = frontiersManager.getFrontierFromID(requestedFrontier.getId());
        if (currentFrontier == null) {
            return ServerFrontierCommandResult.notFound();
        }

        requestedFrontier.setPersonal(currentFrontier.getPersonal());
        if (!currentFrontier.getOwner().isEmpty()) {
            requestedFrontier.setOwner(currentFrontier.getOwner());
        }

        requestedFrontier.setUsersShared(currentFrontier.getUsersShared());
        requestedFrontier.removeChange(FrontierData.Change.Shared);

        if (requestedFrontier.getPersonal()) {
            if (!permissionEvaluator.canUpdatePersonalFrontier(player, currentFrontier)) {
                return ServerFrontierCommandResult.ignored(currentFrontier);
            }

            boolean updated = frontiersManager.updatePersonalFrontier(requestedFrontier.getOwner(), requestedFrontier);
            if (!updated) {
                return ServerFrontierCommandResult.notFound();
            }

            if (requestedFrontier.getUsersShared() != null) {
                for (SettingsUserShared userShared : requestedFrontier.getUsersShared()) {
                    frontiersManager.updatePersonalFrontier(userShared.getUser(), requestedFrontier);
                }
            }

            ServerFrontierCommandResult result = ServerFrontierCommandResult.success(requestedFrontier);
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(requestedFrontier, player.getId()),
                    requestedFrontier, server));
            return result;
        }

        if (!permissionEvaluator.canUpdateGlobalFrontier(player, requestedFrontier)) {
            return rejectedWithProfileRefresh(player, currentFrontier);
        }

        boolean updated = frontiersManager.updateGlobalFrontier(requestedFrontier);
        if (!updated) {
            return ServerFrontierCommandResult.notFound();
        }

        return updatedGlobalFrontier(requestedFrontier, player.getId());
    }

    public ServerFrontierCommandResult updateGlobalFrontier(FrontierData frontier) {
        boolean updated = frontiersManager.updateGlobalFrontier(frontier);
        if (!updated) {
            return ServerFrontierCommandResult.notFound();
        }

        return updatedGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
    }

    public ServerFrontierCommandResult deleteFrontier(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierCommandResult.notFound();
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getPersonal()) {
            if (frontier.getOwner().equals(playerUser)) {
                boolean deleted = frontiersManager.deletePersonalFrontier(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                if (!deleted) {
                    return ServerFrontierCommandResult.notFound();
                }

                if (frontier.getUsersShared() != null) {
                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                        frontiersManager.deletePersonalFrontier(userShared.getUser(), frontier.getDimension(), frontier.getId());
                    }
                }

                ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
                result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierDeleted(frontier.getDimension(),
                        frontier.getId(), frontier.getPersonal(), player.getId()), frontier, server));
                return result;
            }

            frontier.removeUserShared(playerUser);
            frontiersManager.deletePersonalFrontier(playerUser, frontier.getDimension(), frontier.getId());

            ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                    frontier.getPersonal(), player.getId()), player));
            result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getId()),
                    frontier, server));
            frontier.removeChange(FrontierData.Change.Shared);
            return result;
        }

        if (!permissionEvaluator.canDeleteGlobalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerFrontierCommandResult.notFound();
        }

        return deletedGlobalFrontier(frontier, player.getId());
    }

    public ServerFrontierCommandResult deleteGlobalFrontier(UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null || frontier.getPersonal()) {
            return ServerFrontierCommandResult.notFound();
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (!deleted) {
            return ServerFrontierCommandResult.notFound();
        }

        return deletedGlobalFrontier(frontier, SYSTEM_ACTOR_ID);
    }

    public ServerFrontierCommandResult changeFrontierToGlobal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierCommandResult.notFound();
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (!frontier.getPersonal() || !frontier.getOwner().equals(playerUser)) {
            return rejectedWithProfileRefresh(player, frontier);
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
            return ServerFrontierCommandResult.notFound();
        }

        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToGlobal(frontier.getId(), frontier.getModified()), relevantPlayers));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierCreated(frontier, player.getId()), server, relevantPlayers));
        return result;
    }

    public ServerFrontierCommandResult changeFrontierToPersonal(ServerPlayer player, UUID frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null) {
            return ServerFrontierCommandResult.notFound();
        }

        if (frontier.getPersonal()) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        if (!permissionEvaluator.canDeleteGlobalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        boolean changed = frontiersManager.changeGlobalFrontierToPersonal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return ServerFrontierCommandResult.notFound();
        }

        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketChangeFrontierToPersonal(frontier.getId(), frontier.getModified()), player));
        result.addNetworkAction(() -> PacketHandler.sendToAllExcept(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, player.getId()), server, player));
        return result;
    }

    private ServerFrontierCommandResult createdPersonalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, actorId), frontier, server));
        return result;
    }

    private ServerFrontierCommandResult createdGlobalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierCreated(frontier, actorId), server));
        return result;
    }

    private ServerFrontierCommandResult updatedGlobalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierUpdated(frontier, actorId), server));
        return result;
    }

    private ServerFrontierCommandResult deletedGlobalFrontier(FrontierData frontier, int actorId) {
        ServerFrontierCommandResult result = ServerFrontierCommandResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                false, actorId), server));
        return result;
    }

    private ServerFrontierCommandResult rejectedWithProfileRefresh(ServerPlayer player, @Nullable FrontierData frontier) {
        ServerFrontierCommandResult result = ServerFrontierCommandResult.rejected(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }
}
