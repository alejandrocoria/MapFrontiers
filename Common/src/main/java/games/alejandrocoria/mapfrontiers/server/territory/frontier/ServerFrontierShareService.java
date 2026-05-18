package games.alejandrocoria.mapfrontiers.server.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSharingUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontierShared;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoriesManager;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoryPermissionEvaluator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerFrontierShareService {
    private static final int PENDING_SHARE_TICK_INTERVAL = 100;
    private static final int PENDING_SHARE_TICK_DURATION = 1200;

    private final MinecraftServer server;
    private final TerritoriesManager territoriesManager;
    private final TerritoryPermissionEvaluator permissionEvaluator;
    private final Map<Integer, PendingShareFrontier> pendingShareFrontiers = new HashMap<>();
    private int pendingShareFrontiersTick = 0;
    private int nextPendingShareMessageId = 0;

    public ServerFrontierShareService(MinecraftServer server, TerritoriesManager territoriesManager,
                                      TerritoryPermissionEvaluator permissionEvaluator) {
        this.server = server;
        this.territoriesManager = territoriesManager;
        this.permissionEvaluator = permissionEvaluator;
    }

    public boolean canSendCommandAcceptFrontier(ServerPlayer player) {
        return permissionEvaluator.canSendCommandAcceptFrontier(player);
    }

    public ServerTerritoryOperationResult sharePersonalFrontier(ServerPlayer player, UUID frontierId, SettingsUserShared userShared) {
        userShared.getUser().fillMissingInfo(false, server);
        if (userShared.getUser().uuid == null) {
            return ServerTerritoryOperationResult.ignored(null);
        }

        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(userShared.getUser().uuid);
        if (targetPlayer == null) {
            return ServerTerritoryOperationResult.ignored(null);
        }

        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (frontier.getOwner().equals(userShared.getUser()) || frontier.hasUserShared(userShared.getUser())) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        if (!permissionEvaluator.canManagePersonalSharedAccess(player, frontier)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        int shareMessageId = createPendingShare(userShared.getUser(), frontier.getId());

        if (!territoriesManager.addPendingPersonalFrontierShare(frontierId, userShared)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketPersonalFrontierShared(shareMessageId, playerUser,
                frontier.getOwner(), frontier.getName1(), frontier.getName2()), targetPlayer));
        return result;
    }

    public ServerTerritoryOperationResult updateSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId,
                                                                           SettingsUserShared userShared) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        SettingsUserShared currentUserShared = frontier.getUserShared(userShared.getUser());
        if (currentUserShared == null) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!territoriesManager.updatePersonalFrontierShare(frontierId, userShared)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        PacketFrontierSharingUpdated frontierSharingUpdatedPacket = createSharingUpdatedPacket(frontier, player.getId());

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierSharingUpdatedPacket, frontier, server));
        return result;
    }

    public ServerTerritoryOperationResult removeSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId, SettingsUser targetUser) {
        targetUser.fillMissingInfo(false, server);
        if (targetUser.uuid == null) {
            return ServerTerritoryOperationResult.ignored(null);
        }

        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)) {
            return rejectedWithProfileRefresh(player, frontier);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        SettingsUserShared userShared = frontier.getUserShared(targetUser);
        if (userShared == null || userShared.getUser().equals(playerUser)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!permissionEvaluator.canManagePersonalSharedAccess(player, frontier)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        if (!territoriesManager.removePersonalFrontierShare(frontierId, targetUser)) {
            return ServerTerritoryOperationResult.ignored(frontier);
        }

        PacketFrontierSharingUpdated frontierSharingUpdatedPacket = createSharingUpdatedPacket(frontier, player.getId());

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (userShared.isPending()) {
            removePendingSharesForTarget(targetUser);
        } else {
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUser.uuid);
            if (targetPlayer != null) {
                result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontierId, true, -1), targetPlayer));
            }
        }

        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierSharingUpdatedPacket, frontier, server));
        return result;
    }

    public ServerTerritoryOperationResult acceptShareInvitation(ServerPlayer player, int messageId) {
        PendingShareFrontier pending = getPendingShare(messageId);
        if (pending == null) {
            return ServerTerritoryOperationResult.notFound(ServerTerritoryOperationResult.Reason.InvitationExpired);
        }

        SettingsUser playerUser = permissionEvaluator.getPlayerUser(player);
        if (!pending.targetUser.equals(playerUser)) {
            return ServerTerritoryOperationResult.rejected(ServerTerritoryOperationResult.Reason.WrongTarget, null);
        }

        FrontierData frontier = territoriesManager.getFrontierFromID(pending.frontierID);
        if (frontier == null || !frontier.getPersonal()) {
            removePendingShare(messageId);
            return ServerTerritoryOperationResult.notFound(ServerTerritoryOperationResult.Reason.FrontierMissing);
        }

        SettingsUserShared userShared = frontier.getUserShared(pending.targetUser);
        if (userShared == null) {
            return ServerTerritoryOperationResult.ignored(ServerTerritoryOperationResult.Reason.SharedUserMissing, frontier);
        }

        if (territoriesManager.hasPersonalFrontier(pending.targetUser, frontier.getId())) {
            removePendingShare(messageId);
            return ServerTerritoryOperationResult.ignored(ServerTerritoryOperationResult.Reason.AlreadyAccepted, frontier);
        }

        boolean targetAlreadySeesCollection = frontier.hasCollection()
                && territoriesManager.userHasVisiblePersonalCollection(pending.targetUser, frontier.getCollectionId());
        if (!territoriesManager.acceptPendingPersonalFrontierShare(pending.targetUser, pending.frontierID)) {
            return ServerTerritoryOperationResult.ignored(ServerTerritoryOperationResult.Reason.SharedUserMissing, frontier);
        }
        removePendingShare(messageId);

        PacketFrontierSharingUpdated frontierSharingUpdatedPacket = createSharingUpdatedPacket(frontier);

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        if (frontier.hasCollection() && !targetAlreadySeesCollection) {
            CollectionData collection = territoriesManager.getCollectionFromID(frontier.getCollectionId());
            if (collection != null) {
                result.addNetworkAction(() -> PacketHandler.sendTo(new PacketCollectionCreated(new CollectionData(collection)), player));
            }
        }
        result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierCreated(frontier), player));
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccess(frontierSharingUpdatedPacket, frontier, server));
        return result;
    }

    public void tickPendingInvitations() {
        ++pendingShareFrontiersTick;
        if (pendingShareFrontiersTick < PENDING_SHARE_TICK_INTERVAL) {
            return;
        }

        pendingShareFrontiersTick -= PENDING_SHARE_TICK_INTERVAL;

        List<Integer> expiredMessageIds = new ArrayList<>();
        for (Map.Entry<Integer, PendingShareFrontier> entry : pendingShareFrontiers.entrySet()) {
            PendingShareFrontier pending = entry.getValue();
            pending.tickCount += PENDING_SHARE_TICK_INTERVAL;

            if (pending.tickCount < PENDING_SHARE_TICK_DURATION) {
                continue;
            }

            FrontierData frontier = territoriesManager.getFrontierFromID(pending.frontierID);
            if (frontier != null && territoriesManager.expirePendingPersonalFrontierShare(pending.frontierID, pending.targetUser)) {
                PacketHandler.sendToUsersWithAccess(createSharingUpdatedPacket(frontier), frontier, server);
            }

            expiredMessageIds.add(entry.getKey());
        }

        for (Integer messageId : expiredMessageIds) {
            removePendingShare(messageId);
        }
    }

    private int createPendingShare(SettingsUser targetUser, UUID frontierId) {
        nextPendingShareMessageId = advancePendingShareMessageId(nextPendingShareMessageId);
        pendingShareFrontiers.put(nextPendingShareMessageId, new PendingShareFrontier(frontierId, targetUser));
        return nextPendingShareMessageId;
    }

    private @Nullable PendingShareFrontier getPendingShare(int messageId) {
        return pendingShareFrontiers.get(messageId);
    }

    private void removePendingShare(int messageId) {
        pendingShareFrontiers.remove(messageId);
    }

    private void removePendingSharesForTarget(SettingsUser targetUser) {
        pendingShareFrontiers.entrySet().removeIf(entry -> entry.getValue().targetUser.equals(targetUser));
    }

    private static int advancePendingShareMessageId(int currentMessageId) {
        ++currentMessageId;
        if (currentMessageId == 1000) {
            return 1;
        }
        return currentMessageId;
    }

    private ServerTerritoryOperationResult rejectedWithProfileRefresh(ServerPlayer player, @Nullable FrontierData frontier) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.rejected(frontier);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }

    private static PacketFrontierSharingUpdated createSharingUpdatedPacket(FrontierData frontier) {
        return createSharingUpdatedPacket(frontier, -1);
    }

    private static PacketFrontierSharingUpdated createSharingUpdatedPacket(FrontierData frontier, int playerId) {
        return new PacketFrontierSharingUpdated(frontier.getId(), frontier.getDimension(), FrontierSharingChange.fromFrontierData(frontier), playerId);
    }
}
