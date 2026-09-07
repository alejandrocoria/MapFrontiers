package games.alejandrocoria.mapfrontiers.server.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketCollectionDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSharingUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontierShared;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoriesManager;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoryPermissionEvaluator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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

    public ServerTerritoryOperationResult sharePersonalFrontier(ServerPlayer player, UUID frontierId,
                                                                FrontierUserAccess userShared, long baseRevision,
                                                                long requestId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return missingSharingState(player, frontierId);
        }
        if (!canReceivePersonalFrontier(player, frontier)) {
            return missingSharingState(player, frontierId);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)
                || !permissionEvaluator.canManagePersonalSharedAccess(player, frontier)) {
            return rejectedSharingRequest(player, frontier, requestId, true);
        }

        if (baseRevision != frontier.getSharingRevision()) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        if (frontier.getOwner().equals(userShared.getPlayerId())) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }
        if (frontier.hasUserAccess(userShared.getPlayerId())) {
            return acceptedSharingNoOp(player, frontier, requestId);
        }

        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(userShared.getPlayerId().uuid());
        if (targetPlayer == null) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        PlayerId playerUser = permissionEvaluator.getPlayerUser(player);
        if (!territoriesManager.addPendingPersonalFrontierShare(frontierId, userShared)) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }
        int shareMessageId = createPendingShare(userShared.getPlayerId(), frontier.getId());

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        enqueueDirectSharingResponse(result, player, frontier, requestId, OperationResolution.Accepted);
        PacketPersonalFrontierShared invitation = new PacketPersonalFrontierShared(shareMessageId,
                new SettingsUser(playerUser), new SettingsUser(frontier.getOwner()), frontier.getName1(), frontier.getName2());
        result.addNetworkAction(() -> PacketHandler.sendTo(invitation, targetPlayer));
        enqueueSharingBroadcast(result, player, frontier);
        return result;
    }

    public ServerTerritoryOperationResult updateSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId,
                                                                           FrontierUserAccess userShared,
                                                                           long baseRevision, long requestId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return missingSharingState(player, frontierId);
        }
        if (!canReceivePersonalFrontier(player, frontier)) {
            return missingSharingState(player, frontierId);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)
                || !permissionEvaluator.canManagePersonalSharedAccess(player, frontier)) {
            return rejectedSharingRequest(player, frontier, requestId, true);
        }

        if (baseRevision != frontier.getSharingRevision()) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        FrontierUserAccess currentUserShared = frontier.getUserAccess(userShared.getPlayerId());
        if (currentUserShared == null) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }
        if (currentUserShared.getActions().equals(userShared.getActions())) {
            return acceptedSharingNoOp(player, frontier, requestId);
        }

        if (!territoriesManager.updatePersonalFrontierShare(frontierId, userShared)) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        enqueueDirectSharingResponse(result, player, frontier, requestId, OperationResolution.Accepted);
        enqueueSharingBroadcast(result, player, frontier);
        return result;
    }

    public ServerTerritoryOperationResult removeSharedUserPersonalFrontier(ServerPlayer player, UUID frontierId,
                                                                           PlayerId targetUser,
                                                                           long baseRevision, long requestId) {
        FrontierData frontier = territoriesManager.getFrontierFromID(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return missingSharingState(player, frontierId);
        }
        if (!canReceivePersonalFrontier(player, frontier)) {
            return missingSharingState(player, frontierId);
        }

        if (!permissionEvaluator.canSharePersonalFrontier(player, frontier)
                || !permissionEvaluator.canManagePersonalSharedAccess(player, frontier)) {
            return rejectedSharingRequest(player, frontier, requestId, true);
        }

        if (baseRevision != frontier.getSharingRevision()) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        PlayerId playerUser = permissionEvaluator.getPlayerUser(player);
        FrontierUserAccess userShared = frontier.getUserAccess(targetUser);
        if (userShared == null) {
            return acceptedSharingNoOp(player, frontier, requestId);
        }
        if (userShared.getPlayerId().equals(playerUser)) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        if (!territoriesManager.removePersonalFrontierShare(frontierId, targetUser)) {
            return rejectedSharingRequest(player, frontier, requestId, false);
        }

        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        enqueueDirectSharingResponse(result, player, frontier, requestId, OperationResolution.Accepted);
        if (userShared.isPending()) {
            removePendingSharesForTarget(targetUser);
        } else {
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUser.uuid());
            if (targetPlayer != null) {
                result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontierId, true, -1), targetPlayer));
                if (frontier.hasCollection() && !territoriesManager.userKnowsPersonalCollection(targetUser, frontier.getCollectionId())) {
                    result.addNetworkAction(() -> PacketHandler.sendTo(new PacketCollectionDeleted(frontier.getCollectionId()), targetPlayer));
                }
            }
        }

        enqueueSharingBroadcast(result, player, frontier);
        return result;
    }

    public ServerTerritoryOperationResult acceptShareInvitation(ServerPlayer player, int messageId) {
        PendingShareFrontier pending = getPendingShare(messageId);
        if (pending == null) {
            return ServerTerritoryOperationResult.notFound(ServerTerritoryOperationResult.Reason.InvitationExpired);
        }

        PlayerId playerUser = permissionEvaluator.getPlayerUser(player);
        if (!pending.targetUser.equals(playerUser)) {
            return ServerTerritoryOperationResult.rejected(ServerTerritoryOperationResult.Reason.WrongTarget, null);
        }

        FrontierData frontier = territoriesManager.getFrontierFromID(pending.frontierID);
        if (frontier == null || !frontier.getPersonal()) {
            removePendingShare(messageId);
            return ServerTerritoryOperationResult.notFound(ServerTerritoryOperationResult.Reason.FrontierMissing);
        }

        FrontierUserAccess userShared = frontier.getUserAccess(pending.targetUser);
        if (userShared == null) {
            return ServerTerritoryOperationResult.ignored(ServerTerritoryOperationResult.Reason.SharedUserMissing, frontier);
        }

        if (territoriesManager.hasPersonalFrontier(pending.targetUser, frontier.getId())) {
            removePendingShare(messageId);
            return ServerTerritoryOperationResult.ignored(ServerTerritoryOperationResult.Reason.AlreadyAccepted, frontier);
        }

        boolean targetAlreadySeesCollection = frontier.hasCollection()
                && territoriesManager.userKnowsPersonalCollection(pending.targetUser, frontier.getCollectionId());
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

    private int createPendingShare(PlayerId targetUser, UUID frontierId) {
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

    private void removePendingSharesForTarget(PlayerId targetUser) {
        pendingShareFrontiers.entrySet().removeIf(entry -> entry.getValue().targetUser.equals(targetUser));
    }

    private static int advancePendingShareMessageId(int currentMessageId) {
        ++currentMessageId;
        if (currentMessageId == 1000) {
            return 1;
        }
        return currentMessageId;
    }

    private ServerTerritoryOperationResult missingSharingState(ServerPlayer player, UUID frontierId) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.notFound();
        PacketFrontierDeleted deleted = new PacketFrontierDeleted(Level.OVERWORLD, frontierId, true, -1);
        result.addNetworkAction(() -> PacketHandler.sendTo(deleted, player));
        return result;
    }

    private boolean canReceivePersonalFrontier(ServerPlayer player, FrontierData frontier) {
        PlayerId playerUser = permissionEvaluator.getPlayerUser(player);
        if (frontier.getOwner().equals(playerUser)) {
            return true;
        }

        FrontierUserAccess userShared = frontier.getUserAccess(playerUser);
        return userShared != null && !userShared.isPending();
    }

    private ServerTerritoryOperationResult rejectedSharingRequest(ServerPlayer player, FrontierData frontier,
                                                                   long requestId, boolean refreshProfile) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.rejected(frontier);
        enqueueDirectSharingResponse(result, player, frontier, requestId, OperationResolution.Rejected);
        if (refreshProfile) {
            result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        }
        return result;
    }

    private ServerTerritoryOperationResult acceptedSharingNoOp(ServerPlayer player, FrontierData frontier,
                                                                long requestId) {
        ServerTerritoryOperationResult result = ServerTerritoryOperationResult.success(frontier);
        enqueueDirectSharingResponse(result, player, frontier, requestId, OperationResolution.Accepted);
        return result;
    }

    private void enqueueDirectSharingResponse(ServerTerritoryOperationResult result, ServerPlayer player,
                                              FrontierData frontier, long requestId,
                                              OperationResolution resolution) {
        PacketFrontierSharingUpdated response = createSharingUpdatedPacket(frontier, player.getId(), requestId, resolution);
        result.addNetworkAction(() -> PacketHandler.sendTo(response, player));
    }

    private void enqueueSharingBroadcast(ServerTerritoryOperationResult result, ServerPlayer actor,
                                         FrontierData frontier) {
        PacketFrontierSharingUpdated broadcast = createSharingUpdatedPacket(frontier, actor.getId(), 0L,
                OperationResolution.Accepted);
        result.addNetworkAction(() -> PacketHandler.sendToUsersWithAccessExcept(broadcast, frontier, server,
                actor.getUUID()));
    }

    private static PacketFrontierSharingUpdated createSharingUpdatedPacket(FrontierData frontier) {
        return createSharingUpdatedPacket(frontier, -1);
    }

    private static PacketFrontierSharingUpdated createSharingUpdatedPacket(FrontierData frontier, int playerId) {
        return createSharingUpdatedPacket(frontier, playerId, 0L, OperationResolution.Accepted);
    }

    private static PacketFrontierSharingUpdated createSharingUpdatedPacket(FrontierData frontier, int playerId,
                                                                            long requestId,
                                                                            OperationResolution resolution) {
        return new PacketFrontierSharingUpdated(frontier.getId(), frontier.getDimension(),
                FrontierSharingChange.fromFrontierData(frontier), playerId, requestId, resolution);
    }
}
