package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontierShared;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerFrontierShareService {
    private static final int PENDING_SHARE_TICK_INTERVAL = 100;
    private static final int PENDING_SHARE_TICK_DURATION = 1200;

    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;
    private int pendingShareFrontiersTick = 0;

    public ServerFrontierShareService(MinecraftServer server, FrontiersManager frontiersManager,
                                      FrontierPermissionEvaluator permissionEvaluator) {
        this.server = server;
        this.frontiersManager = frontiersManager;
        this.permissionEvaluator = permissionEvaluator;
    }

    public boolean canSendCommandAcceptFrontier(ServerPlayer player) {
        return permissionEvaluator.canSendCommandAcceptFrontier(player);
    }

    public ServerFrontierOperationResult sharePersonalFrontier(ServerPlayer player, UUID frontierId, SettingsUserShared userShared) {
        userShared.getUser().fillMissingInfo(false, server);
        if (userShared.getUser().uuid == null) {
            return ServerFrontierOperationResult.ignored(null);
        }

        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(userShared.getUser().uuid);
        if (targetPlayer == null) {
            return ServerFrontierOperationResult.ignored(null);
        }

        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (frontier.getOwner().equals(userShared.getUser()) || frontier.hasUserShared(userShared.getUser())) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        if (!permissionEvaluator.canManagePersonalShareSettings(player, frontier)) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        int shareMessageId = frontiersManager.addShareMessage(userShared.getUser(), frontier.getId());

        userShared.setPending(true);
        frontier.addUserShared(userShared);
        frontiersManager.saveFrontierData();

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketPersonalFrontierShared(shareMessageId, playerUser,
                frontier.getOwner(), frontier.getName1(), frontier.getName2()), targetPlayer));
        frontier.removeChange(FrontierData.Change.Shared);
        return result;
    }

    public ServerFrontierOperationResult updateSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId,
                                                                        SettingsUserShared userShared) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        SettingsUserShared currentUserShared = frontier.getUserShared(userShared.getUser());
        if (currentUserShared == null) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        currentUserShared.setActions(userShared.getActions());
        frontier.addChange(FrontierData.Change.Shared);
        frontiersManager.saveFrontierData();

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getId()), frontier, server));
        return result;
    }

    public ServerFrontierOperationResult removeSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId, SettingsUser targetUser) {
        targetUser.fillMissingInfo(false, server);
        if (targetUser.uuid == null) {
            return ServerFrontierOperationResult.ignored(null);
        }

        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        SettingsUserShared userShared = frontier.getUserShared(targetUser);
        if (userShared == null || userShared.getUser().equals(playerUser)) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canManagePersonalShareSettings(player, frontier)) {
            return ServerFrontierOperationResult.ignored(frontier);
        }

        frontier.removeUserShared(targetUser);
        frontiersManager.saveFrontierData();

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        if (userShared.isPending()) {
            frontiersManager.removePendingShareFrontier(targetUser);
        } else {
            frontiersManager.deletePersonalFrontier(targetUser, frontier.getDimension(), frontierId);

            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUser.uuid);
            if (targetPlayer != null) {
                result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontierId, true, -1), targetPlayer));
            }
        }

        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getId()), frontier, server));
        frontier.removeChange(FrontierData.Change.Shared);
        return result;
    }

    public ServerFrontierOperationResult acceptShareInvitation(ServerPlayer player, int messageId) {
        PendingShareFrontier pending = frontiersManager.getPendingShareFrontier(messageId);
        if (pending == null) {
            return ServerFrontierOperationResult.notFound(ServerFrontierOperationResult.Reason.InvitationExpired);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (!pending.targetUser.equals(playerUser)) {
            return ServerFrontierOperationResult.rejected(ServerFrontierOperationResult.Reason.WrongTarget, null);
        }

        FrontierData frontier = frontiersManager.getFrontierFromID(pending.frontierID);
        if (frontier == null || !frontier.getPersonal()) {
            frontiersManager.removePendingShareFrontier(messageId);
            return ServerFrontierOperationResult.notFound(ServerFrontierOperationResult.Reason.FrontierMissing);
        }

        SettingsUserShared userShared = frontier.getUserShared(pending.targetUser);
        if (userShared == null) {
            return ServerFrontierOperationResult.ignored(ServerFrontierOperationResult.Reason.SharedUserMissing, frontier);
        }

        if (frontiersManager.hasPersonalFrontier(pending.targetUser, frontier.getId())) {
            frontiersManager.removePendingShareFrontier(messageId);
            return ServerFrontierOperationResult.ignored(ServerFrontierOperationResult.Reason.AlreadyAccepted, frontier);
        }

        frontiersManager.addPersonalFrontier(pending.targetUser, frontier);
        userShared.setPending(false);
        frontier.addChange(FrontierData.Change.Shared);
        frontiersManager.saveFrontierData();
        frontiersManager.removePendingShareFrontier(messageId);

        ServerFrontierOperationResult result = ServerFrontierOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierCreated(frontier), player));
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier), frontier, server));
        frontier.removeChange(FrontierData.Change.Shared);
        return result;
    }

    public void tickPendingInvitations() {
        ++pendingShareFrontiersTick;
        if (pendingShareFrontiersTick < PENDING_SHARE_TICK_INTERVAL) {
            return;
        }

        pendingShareFrontiersTick -= PENDING_SHARE_TICK_INTERVAL;

        List<Integer> expiredMessageIds = new ArrayList<>();
        for (Map.Entry<Integer, PendingShareFrontier> entry : frontiersManager.getPendingShareFrontiers().entrySet()) {
            PendingShareFrontier pending = entry.getValue();
            pending.tickCount += PENDING_SHARE_TICK_INTERVAL;

            if (pending.tickCount < PENDING_SHARE_TICK_DURATION) {
                continue;
            }

            FrontierData frontier = frontiersManager.getFrontierFromID(pending.frontierID);
            if (frontier != null && frontier.getUsersShared() != null) {
                boolean removed = frontier.getUsersShared().removeIf(x -> x.getUser().equals(pending.targetUser));
                if (removed) {
                    frontiersManager.saveFrontierData();
                    PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier), frontier, server);
                }
            }

            expiredMessageIds.add(entry.getKey());
        }

        for (Integer messageId : expiredMessageIds) {
            frontiersManager.removePendingShareFrontier(messageId);
        }
    }

    private ServerFrontierOperationResult rejectedWithProfileRefresh(ServerPlayer player, @Nullable FrontierData frontier) {
        ServerFrontierOperationResult result = ServerFrontierOperationResult.rejected(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }
}
