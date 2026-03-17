package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.CommonNetworkMod;
import commonnetwork.api.Dispatcher;
import commonnetwork.api.Network;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketHandler {
    public static void init() {
        // server to client
        CommonNetworkMod.registerPacket(PacketFrontiers.type(), PacketFrontiers.class, PacketFrontiers.STREAM_CODEC, PacketFrontiers::handle);
        CommonNetworkMod.registerPacket(PacketFrontierCreated.type(), PacketFrontierCreated.class, PacketFrontierCreated.STREAM_CODEC, PacketFrontierCreated::handle);
        CommonNetworkMod.registerPacket(PacketFrontierDeleted.type(), PacketFrontierDeleted.class, PacketFrontierDeleted.STREAM_CODEC, PacketFrontierDeleted::handle);
        CommonNetworkMod.registerPacket(PacketFrontierUpdated.type(), PacketFrontierUpdated.class, PacketFrontierUpdated.STREAM_CODEC, PacketFrontierUpdated::handle);
        CommonNetworkMod.registerPacket(PacketSettingsProfile.type(), PacketSettingsProfile.class, PacketSettingsProfile.STREAM_CODEC, PacketSettingsProfile::handle);
        CommonNetworkMod.registerPacket(PacketPersonalFrontierShared.type(), PacketPersonalFrontierShared.class, PacketPersonalFrontierShared.STREAM_CODEC, PacketPersonalFrontierShared::handle);

        // client to server
        CommonNetworkMod.registerPacket(PacketPersonalFrontier.type(), PacketPersonalFrontier.class, PacketPersonalFrontier.STREAM_CODEC, PacketPersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketCreateFrontier.type(), PacketCreateFrontier.class, PacketCreateFrontier.STREAM_CODEC, PacketCreateFrontier::handle);
        CommonNetworkMod.registerPacket(PacketDeleteFrontier.type(), PacketDeleteFrontier.class, PacketDeleteFrontier.STREAM_CODEC, PacketDeleteFrontier::handle);
        CommonNetworkMod.registerPacket(PacketUpdateFrontier.type(), PacketUpdateFrontier.class, PacketUpdateFrontier.STREAM_CODEC, PacketUpdateFrontier::handle);
        CommonNetworkMod.registerPacket(PacketRequestFrontierSettings.type(), PacketRequestFrontierSettings.class, PacketRequestFrontierSettings.STREAM_CODEC, PacketRequestFrontierSettings::handle);
        CommonNetworkMod.registerPacket(PacketSharePersonalFrontier.type(), PacketSharePersonalFrontier.class, PacketSharePersonalFrontier.STREAM_CODEC, PacketSharePersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketRemoveSharedUserPersonalFrontier.type(), PacketRemoveSharedUserPersonalFrontier.class, PacketRemoveSharedUserPersonalFrontier.STREAM_CODEC, PacketRemoveSharedUserPersonalFrontier::handle);
        CommonNetworkMod.registerPacket(PacketUpdateSharedUserPersonalFrontier.type(), PacketUpdateSharedUserPersonalFrontier.class, PacketUpdateSharedUserPersonalFrontier.STREAM_CODEC, PacketUpdateSharedUserPersonalFrontier::handle);

        // both
        CommonNetworkMod.registerPacket(PacketHandshake.type(), PacketHandshake.class, PacketHandshake.STREAM_CODEC, PacketHandshake::handle);
        CommonNetworkMod.registerPacket(PacketFrontierSettings.type(), PacketFrontierSettings.class, PacketFrontierSettings.STREAM_CODEC, PacketFrontierSettings::handle);
        CommonNetworkMod.registerPacket(PacketChangeFrontierToGlobal.type(), PacketChangeFrontierToGlobal.class, PacketChangeFrontierToGlobal.STREAM_CODEC, PacketChangeFrontierToGlobal::handle);
        CommonNetworkMod.registerPacket(PacketChangeFrontierToPersonal.type(), PacketChangeFrontierToPersonal.class, PacketChangeFrontierToPersonal.STREAM_CODEC, PacketChangeFrontierToPersonal::handle);

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
