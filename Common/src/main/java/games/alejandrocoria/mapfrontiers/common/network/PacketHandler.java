package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.CommonNetworkMod;
import commonnetwork.api.Dispatcher;
import commonnetwork.api.Network;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketHandler {
    public static void init() {
        // server to client
        CommonNetworkMod.registerPacket(PacketTerritoriesSnapshot.TYPE, PacketTerritoriesSnapshot.STREAM_CODEC, PacketTerritoriesSnapshot::handle);
        CommonNetworkMod.registerPacket(PacketCollectionCreated.TYPE, PacketCollectionCreated.STREAM_CODEC, PacketCollectionCreated::handle);
        CommonNetworkMod.registerPacket(PacketCollectionUpdated.TYPE, PacketCollectionUpdated.STREAM_CODEC, PacketCollectionUpdated::handle);
        CommonNetworkMod.registerPacket(PacketCollectionDeleted.TYPE, PacketCollectionDeleted.STREAM_CODEC, PacketCollectionDeleted::handle);
        CommonNetworkMod.registerPacket(PacketFrontierCreated.TYPE, PacketFrontierCreated.STREAM_CODEC, PacketFrontierCreated::handle);
        CommonNetworkMod.registerPacket(PacketFrontierDeleted.TYPE, PacketFrontierDeleted.STREAM_CODEC, PacketFrontierDeleted::handle);
        CommonNetworkMod.registerPacket(PacketFrontierUpdated.TYPE, PacketFrontierUpdated.STREAM_CODEC, PacketFrontierUpdated::handle);
        CommonNetworkMod.registerPacket(PacketFrontierResync.TYPE, PacketFrontierResync.STREAM_CODEC, PacketFrontierResync::handle);
        CommonNetworkMod.registerPacket(PacketFrontierSharingUpdated.TYPE, PacketFrontierSharingUpdated.STREAM_CODEC, PacketFrontierSharingUpdated::handle);
        CommonNetworkMod.registerPacket(PacketSettingsProfile.TYPE, PacketSettingsProfile.STREAM_CODEC, PacketSettingsProfile::handle);
        CommonNetworkMod.registerPacket(PacketPersonalFrontierShared.TYPE, PacketPersonalFrontierShared.STREAM_CODEC, PacketPersonalFrontierShared::handle);

        // client to server
        CommonNetworkMod.registerPacket(PacketPersonalFrontier.TYPE, PacketPersonalFrontier.STREAM_CODEC, PacketPersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketPersonalCollection.TYPE, PacketPersonalCollection.STREAM_CODEC, PacketPersonalCollection::handle);
        CommonNetworkMod.registerPacket(PacketCreateCollection.TYPE, PacketCreateCollection.STREAM_CODEC, PacketCreateCollection::handle);
        CommonNetworkMod.registerPacket(PacketUpdateCollection.TYPE, PacketUpdateCollection.STREAM_CODEC, PacketUpdateCollection::handle);
        CommonNetworkMod.registerPacket(PacketDeleteCollection.TYPE, PacketDeleteCollection.STREAM_CODEC, PacketDeleteCollection::handle);
        CommonNetworkMod.registerPacket(PacketCreateFrontier.TYPE, PacketCreateFrontier.STREAM_CODEC, PacketCreateFrontier::handle);
        CommonNetworkMod.registerPacket(PacketDeleteFrontier.TYPE, PacketDeleteFrontier.STREAM_CODEC, PacketDeleteFrontier::handle);
        CommonNetworkMod.registerPacket(PacketUpdateFrontier.TYPE, PacketUpdateFrontier.STREAM_CODEC, PacketUpdateFrontier::handle);
        CommonNetworkMod.registerPacket(PacketRequestFrontierResync.TYPE, PacketRequestFrontierResync.STREAM_CODEC, PacketRequestFrontierResync::handle);
        CommonNetworkMod.registerPacket(PacketRequestFrontierSettings.TYPE, PacketRequestFrontierSettings.STREAM_CODEC, PacketRequestFrontierSettings::handle);
        CommonNetworkMod.registerPacket(PacketSharePersonalFrontier.TYPE, PacketSharePersonalFrontier.STREAM_CODEC, PacketSharePersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketRemoveSharedUserPersonalFrontier.TYPE, PacketRemoveSharedUserPersonalFrontier.STREAM_CODEC, PacketRemoveSharedUserPersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketUpdateSharedUserPersonalFrontier.TYPE, PacketUpdateSharedUserPersonalFrontier.STREAM_CODEC, PacketUpdateSharedUserPersonalFrontier::handle);

        // both
        CommonNetworkMod.registerPacket(PacketHandshake.TYPE, PacketHandshake.STREAM_CODEC, PacketHandshake::handle);
        CommonNetworkMod.registerPacket(PacketFrontierSettings.TYPE, PacketFrontierSettings.STREAM_CODEC, PacketFrontierSettings::handle);
        CommonNetworkMod.registerPacket(PacketChangeFrontierToGlobal.TYPE, PacketChangeFrontierToGlobal.STREAM_CODEC, PacketChangeFrontierToGlobal::handle);
        CommonNetworkMod.registerPacket(PacketChangeFrontierToPersonal.TYPE, PacketChangeFrontierToPersonal.STREAM_CODEC, PacketChangeFrontierToPersonal::handle);
    }

    public static void sendToUsersWithAccess(CustomPacketPayload message, FrontierData frontier, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(frontier.getOwner().uuid);
        if (player != null) {
            sendTo(message, player);
        }

        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                if (!userShared.isPending()) {
                    player = server.getPlayerList().getPlayer(userShared.getUser().uuid);
                    if (player != null) {
                        sendTo(message, player);
                    }
                }
            }
        }
    }

    public static void sendTo(CustomPacketPayload message, ServerPlayer player) {
        Network.getNetworkHandler().sendToClient(message, player, true);
    }

    public static void sendTo(CustomPacketPayload message, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendTo(message, player);
        }
    }

    public static void sendToAll(CustomPacketPayload message, MinecraftServer server) {
        Network.getNetworkHandler().sendToAllClients(message, server, true);
    }

    public static void sendToAllExcept(CustomPacketPayload message, MinecraftServer server, ServerPlayer ignorePlayer) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.equals(ignorePlayer)) {
                sendTo(message, player);
            }
        }
    }

    public static void sendToAllExcept(CustomPacketPayload message, MinecraftServer server, List<ServerPlayer> ignorePlayers) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ignorePlayers.contains(player)) {
                sendTo(message, player);
            }
        }
    }

    public static void sendToServer(CustomPacketPayload message) {
        Dispatcher.sendToServer(message, true);
    }
}
