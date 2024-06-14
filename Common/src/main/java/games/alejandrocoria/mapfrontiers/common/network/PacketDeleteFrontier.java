package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_delete_frontier");

    private UUID frontierID;

    public PacketDeleteFrontier() {
    }

    public PacketDeleteFrontier(UUID frontierID) {
        this.frontierID = frontierID;
    }

    public static PacketDeleteFrontier decode(FriendlyByteBuf buf) {
        PacketDeleteFrontier packet = new PacketDeleteFrontier();

        try {
            if (buf.readableBytes() > 1) {
                packet.frontierID = UUIDHelper.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketDeleteFrontier: %s", t));
        }

        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketDeleteFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketDeleteFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketDeleteFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData frontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (frontier != null) {
                if (frontier.getPersonal()) {
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
                                    frontier.getId(), frontier.getPersonal(), player.getId()), frontier, server);
                        }
                    } else {
                        frontier.removeUserShared(playerUser);
                        FrontiersManager.instance.deletePersonalFrontier(playerUser, frontier.getDimension(),
                                frontier.getId());

                        PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                                frontier.getPersonal(), player.getId()), player);
                        PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getId()),
                                frontier, server);

                        frontier.removeChange(FrontierData.Change.Shared);
                    }

                    return;
                } else {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), frontier.getOwner())) {
                        boolean deleted = FrontiersManager.instance.deleteGlobalFrontier(frontier.getDimension(),
                                frontier.getId());
                        if (deleted) {
                            PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(),
                                    frontier.getPersonal(), player.getId()), server);
                        }

                        return;
                    }
                }

                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        }
    }
}
