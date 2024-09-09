package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public class PacketUpdateFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketUpdateFrontier::encode, PacketUpdateFrontier::new);

    private final FrontierData frontier;
    private final Set<FrontierData.Change> changes;

    public PacketUpdateFrontier(FrontierData frontier) {
        this.frontier = frontier;
        changes = frontier.getChanges();
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketUpdateFrontier(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();
        this.changes = null;

        try {
            if (buf.readableBytes() > 1) {
                this.frontier.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketUpdateFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, changes);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketUpdateFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketUpdateFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketUpdateFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
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
                    if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier)) {
                        boolean updated = FrontiersManager.instance.updatePersonalFrontier(message.frontier.getOwner(), message.frontier);
                        if (updated) {
                            if (message.frontier.getUsersShared() != null) {
                                for (SettingsUserShared userShared : message.frontier.getUsersShared()) {
                                    FrontiersManager.instance.updatePersonalFrontier(userShared.getUser(), message.frontier);
                                }
                            }
                            PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(message.frontier, player.getId()),
                                    message.frontier, server);
                        }
                    }

                    return;
                } else {
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateGlobalFrontier, playerUser,
                            MapFrontiers.isOPorHost(player), message.frontier.getOwner())) {
                        boolean updated = FrontiersManager.instance.updateGlobalFrontier(message.frontier);
                        if (updated) {
                            PacketHandler.sendToAll(new PacketFrontierUpdated(message.frontier, player.getId()), server);
                        }

                        return;
                    }
                }

                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        }
    }
}
