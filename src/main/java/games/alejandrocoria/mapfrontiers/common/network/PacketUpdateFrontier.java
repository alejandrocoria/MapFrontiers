package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketUpdateFrontier {
    private final FrontierData frontier;

    public PacketUpdateFrontier() {
        frontier = new FrontierData();
    }

    public PacketUpdateFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public static PacketUpdateFrontier fromBytes(FriendlyByteBuf buf) {
        PacketUpdateFrontier packet = new PacketUpdateFrontier();
        packet.frontier.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketUpdateFrontier packet, FriendlyByteBuf buf) {
        packet.frontier.toBytes(buf);
    }

    public static void handle(PacketUpdateFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            SettingsUser playerUser = new SettingsUser(player);
            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getId());

            if (currentFrontier != null) {
                message.frontier.setPersonal(currentFrontier.getPersonal());
                if (!currentFrontier.getOwner().isEmpty()) {
                    message.frontier.setOwner(currentFrontier.getOwner());
                }

                message.frontier.setUsersShared(currentFrontier.getUsersShared());
                message.frontier.removeChange(FrontierData.Change.Shared);

                if (message.frontier.getPersonal()) {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), message.frontier.getOwner())) {
                        if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier)) {
                            boolean updated = FrontiersManager.instance.updatePersonalFrontier(message.frontier.getOwner(),
                                    message.frontier);
                            if (updated) {
                                if (message.frontier.getUsersShared() != null) {
                                    for (SettingsUserShared userShared : message.frontier.getUsersShared()) {
                                        FrontiersManager.instance.updatePersonalFrontier(userShared.getUser(), message.frontier);
                                    }
                                }
                                PacketHandler.sendToUsersWithAccess(
                                        new PacketFrontierUpdated(message.frontier, player.getId()), message.frontier);
                            }
                        }

                        return;
                    }
                } else {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), message.frontier.getOwner())) {
                        boolean updated = FrontiersManager.instance.updateGlobalFrontier(message.frontier);
                        if (updated) {
                            PacketHandler.sendToAll(new PacketFrontierUpdated(message.frontier, player.getId()));
                        }

                        return;
                    }
                }

                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)),
                        player);
            }
        });

        context.setPacketHandled(true);
    }
}
