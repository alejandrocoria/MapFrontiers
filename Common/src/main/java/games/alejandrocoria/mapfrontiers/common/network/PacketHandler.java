package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.api.Dispatcher;
import commonnetwork.api.Network;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketHandler {
    public static void init() {
        // server to client
        Network.registerPacket(PacketTerritoriesSnapshot.CHANNEL, PacketTerritoriesSnapshot.class, PacketTerritoriesSnapshot::encode, PacketTerritoriesSnapshot::new, PacketTerritoriesSnapshot::handle);
        Network.registerPacket(PacketCollectionCreated.CHANNEL, PacketCollectionCreated.class, PacketCollectionCreated::encode, PacketCollectionCreated::new, PacketCollectionCreated::handle);
        Network.registerPacket(PacketCollectionUpdated.CHANNEL, PacketCollectionUpdated.class, PacketCollectionUpdated::encode, PacketCollectionUpdated::new, PacketCollectionUpdated::handle);
        Network.registerPacket(PacketCollectionDeleted.CHANNEL, PacketCollectionDeleted.class, PacketCollectionDeleted::encode, PacketCollectionDeleted::new, PacketCollectionDeleted::handle);
        Network.registerPacket(PacketFrontierCreated.CHANNEL, PacketFrontierCreated.class, PacketFrontierCreated::encode, PacketFrontierCreated::new, PacketFrontierCreated::handle);
        Network.registerPacket(PacketFrontierDeleted.CHANNEL, PacketFrontierDeleted.class, PacketFrontierDeleted::encode, PacketFrontierDeleted::new, PacketFrontierDeleted::handle);
        Network.registerPacket(PacketFrontierUpdated.CHANNEL, PacketFrontierUpdated.class, PacketFrontierUpdated::encode, PacketFrontierUpdated::new, PacketFrontierUpdated::handle);
        Network.registerPacket(PacketFrontierResync.CHANNEL, PacketFrontierResync.class, PacketFrontierResync::encode, PacketFrontierResync::new, PacketFrontierResync::handle);
        Network.registerPacket(PacketFrontierSharingUpdated.CHANNEL, PacketFrontierSharingUpdated.class, PacketFrontierSharingUpdated::encode, PacketFrontierSharingUpdated::new, PacketFrontierSharingUpdated::handle);
        Network.registerPacket(PacketSettingsProfile.CHANNEL, PacketSettingsProfile.class, PacketSettingsProfile::encode, PacketSettingsProfile::new, PacketSettingsProfile::handle);
        Network.registerPacket(PacketPersonalFrontierShared.CHANNEL, PacketPersonalFrontierShared.class, PacketPersonalFrontierShared::encode, PacketPersonalFrontierShared::new, PacketPersonalFrontierShared::handle);

        // client to server
        Network.registerPacket(PacketPersonalFrontier.CHANNEL, PacketPersonalFrontier.class, PacketPersonalFrontier::encode, PacketPersonalFrontier::new, PacketPersonalFrontier::handle);
        Network.registerPacket(PacketPersonalCollection.CHANNEL, PacketPersonalCollection.class, PacketPersonalCollection::encode, PacketPersonalCollection::new, PacketPersonalCollection::handle);
        Network.registerPacket(PacketCreateCollection.CHANNEL, PacketCreateCollection.class, PacketCreateCollection::encode, PacketCreateCollection::new, PacketCreateCollection::handle);
        Network.registerPacket(PacketUpdateCollection.CHANNEL, PacketUpdateCollection.class, PacketUpdateCollection::encode, PacketUpdateCollection::new, PacketUpdateCollection::handle);
        Network.registerPacket(PacketDeleteCollection.CHANNEL, PacketDeleteCollection.class, PacketDeleteCollection::encode, PacketDeleteCollection::new, PacketDeleteCollection::handle);
        Network.registerPacket(PacketCreateFrontier.CHANNEL, PacketCreateFrontier.class, PacketCreateFrontier::encode, PacketCreateFrontier::new, PacketCreateFrontier::handle);
        Network.registerPacket(PacketDeleteFrontier.CHANNEL, PacketDeleteFrontier.class, PacketDeleteFrontier::encode, PacketDeleteFrontier::new, PacketDeleteFrontier::handle);
        Network.registerPacket(PacketUpdateFrontier.CHANNEL, PacketUpdateFrontier.class, PacketUpdateFrontier::encode, PacketUpdateFrontier::new, PacketUpdateFrontier::handle);
        Network.registerPacket(PacketRequestFrontierResync.CHANNEL, PacketRequestFrontierResync.class, PacketRequestFrontierResync::encode, PacketRequestFrontierResync::new, PacketRequestFrontierResync::handle);
        Network.registerPacket(PacketRequestFrontierSettings.CHANNEL, PacketRequestFrontierSettings.class, PacketRequestFrontierSettings::encode, PacketRequestFrontierSettings::new, PacketRequestFrontierSettings::handle);
        Network.registerPacket(PacketSharePersonalFrontier.CHANNEL, PacketSharePersonalFrontier.class, PacketSharePersonalFrontier::encode, PacketSharePersonalFrontier::new, PacketSharePersonalFrontier::handle);
        Network.registerPacket(PacketRemoveSharedUserPersonalFrontier.CHANNEL, PacketRemoveSharedUserPersonalFrontier.class, PacketRemoveSharedUserPersonalFrontier::encode, PacketRemoveSharedUserPersonalFrontier::new, PacketRemoveSharedUserPersonalFrontier::handle);
        Network.registerPacket(PacketUpdateSharedUserPersonalFrontier.CHANNEL, PacketUpdateSharedUserPersonalFrontier.class, PacketUpdateSharedUserPersonalFrontier::encode, PacketUpdateSharedUserPersonalFrontier::new, PacketUpdateSharedUserPersonalFrontier::handle);

        // both
        Network.registerPacket(PacketHandshake.CHANNEL, PacketHandshake.class, PacketHandshake::encode, PacketHandshake::new, PacketHandshake::handle);
        Network.registerPacket(PacketFrontierSettings.CHANNEL, PacketFrontierSettings.class, PacketFrontierSettings::encode, PacketFrontierSettings::new, PacketFrontierSettings::handle);
        Network.registerPacket(PacketChangeFrontierToGlobal.CHANNEL, PacketChangeFrontierToGlobal.class, PacketChangeFrontierToGlobal::encode, PacketChangeFrontierToGlobal::new, PacketChangeFrontierToGlobal::handle);
        Network.registerPacket(PacketChangeFrontierToPersonal.CHANNEL, PacketChangeFrontierToPersonal.class, PacketChangeFrontierToPersonal::encode, PacketChangeFrontierToPersonal::new, PacketChangeFrontierToPersonal::handle);
    }

    public static <MSG> void sendToUsersWithAccess(MSG message, FrontierData frontier, MinecraftServer server) {
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

    public static <MSG> void sendTo(MSG message, ServerPlayer player) {
        Network.getNetworkHandler().sendToClient(message, player, true);
    }

    public static <MSG> void sendTo(MSG message, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendTo(message, player);
        }
    }

    public static <MSG> void sendToAll(MSG message, MinecraftServer server) {
        Network.getNetworkHandler().sendToAllClients(message, server, true);
    }

    public static <MSG> void sendToAllExcept(MSG message, MinecraftServer server, ServerPlayer ignorePlayer) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.equals(ignorePlayer)) {
                sendTo(message, player);
            }
        }
    }

    public static <MSG> void sendToAllExcept(MSG message, MinecraftServer server, List<ServerPlayer> ignorePlayers) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ignorePlayers.contains(player)) {
                sendTo(message, player);
            }
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        Dispatcher.sendToServer(message, true);
    }
}
