package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.api.Dispatcher;
import commonnetwork.api.Network;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketHandler {
    public static void init() {
        // server to client
        Network.registerPacket(PacketFrontiers.CHANNEL, PacketFrontiers.class, PacketFrontiers::encode, PacketFrontiers::decode, PacketFrontiers::handle);
        Network.registerPacket(PacketFrontierCreated.CHANNEL, PacketFrontierCreated.class, PacketFrontierCreated::encode, PacketFrontierCreated::decode, PacketFrontierCreated::handle);
        Network.registerPacket(PacketFrontierDeleted.CHANNEL, PacketFrontierDeleted.class, PacketFrontierDeleted::encode, PacketFrontierDeleted::decode, PacketFrontierDeleted::handle);
        Network.registerPacket(PacketFrontierUpdated.CHANNEL, PacketFrontierUpdated.class, PacketFrontierUpdated::encode, PacketFrontierUpdated::decode, PacketFrontierUpdated::handle);
        Network.registerPacket(PacketSettingsProfile.CHANNEL, PacketSettingsProfile.class, PacketSettingsProfile::encode, PacketSettingsProfile::decode, PacketSettingsProfile::handle);
        Network.registerPacket(PacketPersonalFrontierShared.CHANNEL, PacketPersonalFrontierShared.class, PacketPersonalFrontierShared::encode, PacketPersonalFrontierShared::decode, PacketPersonalFrontierShared::handle);

        // client to server
        Network.registerPacket(PacketPersonalFrontier.CHANNEL, PacketPersonalFrontier.class, PacketPersonalFrontier::encode, PacketPersonalFrontier::decode, PacketPersonalFrontier::handle);
        Network.registerPacket(PacketCreateFrontier.CHANNEL, PacketCreateFrontier.class, PacketCreateFrontier::encode, PacketCreateFrontier::decode, PacketCreateFrontier::handle);
        Network.registerPacket(PacketDeleteFrontier.CHANNEL, PacketDeleteFrontier.class, PacketDeleteFrontier::encode, PacketDeleteFrontier::decode, PacketDeleteFrontier::handle);
        Network.registerPacket(PacketUpdateFrontier.CHANNEL, PacketUpdateFrontier.class, PacketUpdateFrontier::encode, PacketUpdateFrontier::decode, PacketUpdateFrontier::handle);
        Network.registerPacket(PacketRequestFrontierSettings.CHANNEL, PacketRequestFrontierSettings.class, PacketRequestFrontierSettings::encode, PacketRequestFrontierSettings::decode, PacketRequestFrontierSettings::handle);
        Network.registerPacket(PacketSharePersonalFrontier.CHANNEL, PacketSharePersonalFrontier.class, PacketSharePersonalFrontier::encode, PacketSharePersonalFrontier::decode, PacketSharePersonalFrontier::handle);
        Network.registerPacket(PacketRemoveSharedUserPersonalFrontier.CHANNEL, PacketRemoveSharedUserPersonalFrontier.class, PacketRemoveSharedUserPersonalFrontier::encode, PacketRemoveSharedUserPersonalFrontier::decode, PacketRemoveSharedUserPersonalFrontier::handle);
        Network.registerPacket(PacketUpdateSharedUserPersonalFrontier.CHANNEL, PacketUpdateSharedUserPersonalFrontier.class, PacketUpdateSharedUserPersonalFrontier::encode, PacketUpdateSharedUserPersonalFrontier::decode, PacketUpdateSharedUserPersonalFrontier::handle);

        // both
        Network.registerPacket(PacketFrontierSettings.CHANNEL, PacketFrontierSettings.class, PacketFrontierSettings::encode, PacketFrontierSettings::decode, PacketFrontierSettings::handle);

        MapFrontiers.LOGGER.info("PacketHandler init done");
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
        Dispatcher.sendToClient(message, player);
    }

    public static <MSG> void sendToAll(MSG message, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Dispatcher.sendToClient(message, player);
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        Dispatcher.sendToServer(message);
    }
}
