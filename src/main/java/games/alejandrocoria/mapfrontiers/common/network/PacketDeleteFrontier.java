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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier {
    private final UUID frontierID;

    public PacketDeleteFrontier(UUID frontierID) {
        this.frontierID = frontierID;
    }

    public static PacketDeleteFrontier fromBytes(PacketBuffer buf) {
        return new PacketDeleteFrontier(UUIDHelper.fromBytes(buf));
    }

    public static void toBytes(PacketDeleteFrontier packet, PacketBuffer buf) {
        UUIDHelper.toBytes(buf, packet.frontierID);
    }

    public static void handle(PacketDeleteFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) {
                return;
            }

            SettingsUser playerUser = new SettingsUser(player);
            FrontierData frontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (frontier != null) {
                if (frontier.getPersonal()) {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), frontier.getOwner())) {
                        if (frontier.getOwner().equals(playerUser)) {
                            boolean deleted = FrontiersManager.instance.deletePersonalFrontier(frontier.getOwner(),
                                    frontier.getDimension(), frontier.getId());
                            if (deleted) {
                                if (frontier.getUsersShared() != null) {
                                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                                        FrontiersManager.instance.deletePersonalFrontier(userShared.getUser(),
                                                frontier.getDimension(), frontier.getId());
                                    }
                                }
                                PacketHandler.sendToUsersWithAccess(new PacketFrontierDeleted(frontier.getDimension(),
                                        frontier.getId(), frontier.getPersonal(), player.getId()), frontier);
                            }
                        } else {
                            frontier.removeUserShared(playerUser);
                            FrontiersManager.instance.deletePersonalFrontier(playerUser, frontier.getDimension(),
                                    frontier.getId());

                            PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                                    frontier.getPersonal(), player.getId()), player);
                            PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getId()),
                                    frontier);

                            frontier.removeChange(FrontierData.Change.Shared);
                        }

                        return;
                    }
                } else {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.DeleteFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), frontier.getOwner())) {
                        boolean deleted = FrontiersManager.instance.deleteGlobalFrontier(frontier.getDimension(),
                                frontier.getId());
                        if (deleted) {
                            PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                                    frontier.getPersonal(), player.getId()));
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
