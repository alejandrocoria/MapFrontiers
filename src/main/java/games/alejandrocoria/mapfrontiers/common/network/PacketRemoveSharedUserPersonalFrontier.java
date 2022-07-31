package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Supplier;

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

    public static void handle(PacketRemoveSharedUserPersonalFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            SettingsUser playerUser = new SettingsUser(player);

            message.targetUser.fillMissingInfo(false);
            if (message.targetUser.uuid == null) {
                return;
            }

            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier, playerUser,
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
                            FrontiersManager.instance.deletePersonalFrontier(message.targetUser, currentFrontier.getDimension(),
                                    message.frontierID);

                            ServerPlayer targetPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList()
                                    .getPlayer(message.targetUser.uuid);
                            if (targetPlayer != null) {
                                PacketHandler.sendTo(
                                        new PacketFrontierDeleted(currentFrontier.getDimension(), message.frontierID, true, -1),
                                        targetPlayer);
                            }
                        }

                        PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(currentFrontier, player.getId()),
                                currentFrontier);

                        currentFrontier.removeChange(FrontierData.Change.Shared);
                    }
                } else {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)),
                            player);
                }
            }
        });

        context.setPacketHandled(true);
    }
}
