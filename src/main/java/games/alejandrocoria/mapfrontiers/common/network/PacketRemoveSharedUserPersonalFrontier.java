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
public class PacketRemoveSharedUserPersonalFrontier {
    private UUID frontierID;
    private final SettingsUser targetUser;

    public PacketRemoveSharedUserPersonalFrontier() {
        targetUser = new SettingsUser();
    }

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    public static PacketRemoveSharedUserPersonalFrontier fromBytes(FriendlyByteBuf buf) {
        PacketRemoveSharedUserPersonalFrontier packet = new PacketRemoveSharedUserPersonalFrontier();
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.targetUser.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketRemoveSharedUserPersonalFrontier packet, FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, packet.frontierID);
        packet.targetUser.toBytes(buf);
    }

    public static void handle(PacketRemoveSharedUserPersonalFrontier message, MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            SettingsUser playerUser = new SettingsUser(player);

            message.targetUser.fillMissingInfo(false, server);
            if (message.targetUser.uuid == null) {
                return;
            }

            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, playerUser,
                        MapFrontiers.isOPorHost(player), currentFrontier.getOwner())) {
                    SettingsUserShared userShared = currentFrontier.getUserShared(message.targetUser);

                    if (userShared == null || userShared.getUser().equals(playerUser)) {
                        return;
                    }

                    if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateSettings)) {
                        currentFrontier.removeUserShared(message.targetUser);

                        if (userShared.isPending()) {
                            FrontiersManager.instance.removePendingShareFrontier(message.targetUser);
                        } else {
                            FrontiersManager.instance.deletePersonalFrontier(message.targetUser, currentFrontier.getDimension(), message.frontierID);

                            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(message.targetUser.uuid);
                            if (targetPlayer != null) {
                                PacketHandler.sendTo(PacketFrontierDeleted.class, new PacketFrontierDeleted(currentFrontier.getDimension(), message.frontierID,
                                        true, -1), targetPlayer);
                            }
                        }

                        PacketHandler.sendToUsersWithAccess(PacketFrontierUpdated.class, new PacketFrontierUpdated(currentFrontier, player.getId()), currentFrontier, server);

                        currentFrontier.removeChange(FrontierData.Change.Shared);
                    }
                } else {
                    PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                }
            }
        });
    }
}
