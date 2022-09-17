package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketUpdateSharedUserPersonalFrontier {
    private UUID frontierID;
    private final SettingsUserShared userShared;

    public PacketUpdateSharedUserPersonalFrontier() {
        userShared = new SettingsUserShared();
    }

    public PacketUpdateSharedUserPersonalFrontier(UUID frontierID, SettingsUserShared user) {
        this.frontierID = frontierID;
        userShared = user;
    }

    public static PacketUpdateSharedUserPersonalFrontier fromBytes(FriendlyByteBuf buf) {
        PacketUpdateSharedUserPersonalFrontier packet = new PacketUpdateSharedUserPersonalFrontier();
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.userShared.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketUpdateSharedUserPersonalFrontier packet, FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, packet.frontierID);
        packet.userShared.toBytes(buf);
    }

    public static void handle(PacketUpdateSharedUserPersonalFrontier message, MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, playerUser,
                        MapFrontiers.isOPorHost(player), currentFrontier.getOwner())) {
                    SettingsUserShared currentUserShared = currentFrontier.getUserShared(message.userShared.getUser());

                    if (currentUserShared == null) {
                        return;
                    }

                    currentUserShared.setActions(message.userShared.getActions());
                    currentFrontier.addChange(FrontierData.Change.Shared);

                    PacketHandler.sendToUsersWithAccess(PacketFrontierUpdated.class, new PacketFrontierUpdated(currentFrontier, player.getId()), currentFrontier, server);
                } else {
                    PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                }
            }
        });
    }
}
