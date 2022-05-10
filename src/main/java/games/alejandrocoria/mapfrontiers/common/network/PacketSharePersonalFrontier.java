package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketSharePersonalFrontier {
    private UUID frontierID;
    private final SettingsUser targetUser;

    public PacketSharePersonalFrontier() {
        targetUser = new SettingsUser();
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    public static PacketSharePersonalFrontier fromBytes(PacketBuffer buf) {
        PacketSharePersonalFrontier packet = new PacketSharePersonalFrontier();
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.targetUser.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketSharePersonalFrontier packet, PacketBuffer buf) {
        UUIDHelper.toBytes(buf, packet.frontierID);
        packet.targetUser.toBytes(buf);
    }

    public static void handle(PacketSharePersonalFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) {
                return;
            }

            SettingsUser playerUser = new SettingsUser(player);

            message.targetUser.fillMissingInfo(false);
            if (message.targetUser.uuid == null) {
                return;
            }

            ServerPlayerEntity targetPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(message.targetUser.uuid);
            if (targetPlayer == null) {
                return;
            }

            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (currentFrontier.getOwner().equals(message.targetUser) || currentFrontier.hasUserShared(message.targetUser)) {
                    return;
                }

                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier, playerUser,
                        MapFrontiers.isOPorHost(player), currentFrontier.getOwner())) {
                    if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateSettings)) {
                        int shareMessageID = FrontiersManager.instance.addShareMessage(message.targetUser,
                                currentFrontier.getId());

                        currentFrontier.addUserShared(new SettingsUserShared(message.targetUser, true));

                        PacketHandler.sendTo(new PacketPersonalFrontierShared(shareMessageID, playerUser,
                                currentFrontier.getOwner(), currentFrontier.getName1(), currentFrontier.getName2()),
                                targetPlayer);

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
