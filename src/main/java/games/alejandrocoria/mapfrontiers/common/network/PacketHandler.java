package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MapFrontiers.MODID, "main"), () -> PROTOCOL_VERSION, (s) -> true, (s) -> true);

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(id++, PacketFrontiers.class, PacketFrontiers::toBytes, PacketFrontiers::fromBytes,
                PacketFrontiers::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketFrontierCreated.class, PacketFrontierCreated::toBytes, PacketFrontierCreated::fromBytes,
                PacketFrontierCreated::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketPersonalFrontier.class, PacketPersonalFrontier::toBytes, PacketPersonalFrontier::fromBytes,
                PacketPersonalFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketCreateFrontier.class, PacketCreateFrontier::toBytes, PacketCreateFrontier::fromBytes,
                PacketCreateFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketDeleteFrontier.class, PacketDeleteFrontier::toBytes, PacketDeleteFrontier::fromBytes,
                PacketDeleteFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketFrontierDeleted.class, PacketFrontierDeleted::toBytes,
                PacketFrontierDeleted::fromBytes, PacketFrontierDeleted::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketUpdateFrontier.class, PacketUpdateFrontier::toBytes, PacketUpdateFrontier::fromBytes,
                PacketUpdateFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketFrontierUpdated.class, PacketFrontierUpdated::toBytes,
                PacketFrontierUpdated::fromBytes, PacketFrontierUpdated::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketRequestFrontierSettings.class, PacketRequestFrontierSettings::toBytes,
                PacketRequestFrontierSettings::fromBytes, PacketRequestFrontierSettings::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketFrontierSettings.class, PacketFrontierSettings::toBytes,
                PacketFrontierSettings::fromBytes, PacketFrontierSettings::handle);
        INSTANCE.registerMessage(id++, PacketSettingsProfile.class, PacketSettingsProfile::toBytes,
                PacketSettingsProfile::fromBytes, PacketSettingsProfile::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketSharePersonalFrontier.class, PacketSharePersonalFrontier::toBytes,
                PacketSharePersonalFrontier::fromBytes, PacketSharePersonalFrontier::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketPersonalFrontierShared.class, PacketPersonalFrontierShared::toBytes,
                PacketPersonalFrontierShared::fromBytes, PacketPersonalFrontierShared::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, PacketRemoveSharedUserPersonalFrontier.class,
                PacketRemoveSharedUserPersonalFrontier::toBytes, PacketRemoveSharedUserPersonalFrontier::fromBytes,
                PacketRemoveSharedUserPersonalFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, PacketUpdateSharedUserPersonalFrontier.class,
                PacketUpdateSharedUserPersonalFrontier::toBytes, PacketUpdateSharedUserPersonalFrontier::fromBytes,
                PacketUpdateSharedUserPersonalFrontier::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static <MSG> void sendToUsersWithAccess(MSG message, FrontierData frontier) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

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
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
