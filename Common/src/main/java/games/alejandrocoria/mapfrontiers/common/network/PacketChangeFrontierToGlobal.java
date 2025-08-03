package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketChangeFrontierToGlobal {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_change_frontier_to_global");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChangeFrontierToGlobal> STREAM_CODEC = StreamCodec.ofMember(PacketChangeFrontierToGlobal::encode, PacketChangeFrontierToGlobal::new);

    private UUID frontierID;
    private Date modified;

    public PacketChangeFrontierToGlobal(UUID frontierID, @Nullable Date modified) {
        this.frontierID = frontierID;
        this.modified = modified;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketChangeFrontierToGlobal(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                if (buf.readBoolean()) {
                    modified = new Date(buf.readLong());
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketChangeFrontierToGlobal: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            if (modified == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeLong(modified.getTime());
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketChangeFrontierToGlobal: %s", t));
        }
    }

    public static void handle(PacketContext<PacketChangeFrontierToGlobal> ctx) {
        PacketChangeFrontierToGlobal message = ctx.message();

        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData frontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (frontier != null) {
                if (frontier.getPersonal() && frontier.getOwner().equals(playerUser)) {
                    List<ServerPlayer> relevantPlayers = new ArrayList<>();
                    relevantPlayers.add(player);
                    if (frontier.getUsersShared() != null) {
                        for (SettingsUserShared userShared : frontier.getUsersShared()) {
                            if (!userShared.isPending()) {
                                ServerPlayer otherPlayer = server.getPlayerList().getPlayer(userShared.getUser().uuid);
                                if (otherPlayer != null) {
                                    relevantPlayers.add(otherPlayer);
                                }
                            }
                        }
                    }

                    boolean changed = FrontiersManager.instance.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
                    if (changed) {
                        PacketHandler.sendTo(new PacketChangeFrontierToGlobal(frontier.getId(), frontier.getModified()), relevantPlayers);
                        PacketHandler.sendToAllExcept(new PacketFrontierCreated(frontier, player.getId()), server, relevantPlayers);
                    }

                    return;
                }

                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        } else {
            FrontiersOverlayManager personalManager = MapFrontiersClient.getFrontiersOverlayManager(true);
            FrontierOverlay frontierOverlay = personalManager.deleteFrontier(message.frontierID);
            frontierOverlay.setPersonal(false);
            if (message.modified != null) {
                frontierOverlay.setModified(message.modified);
            }
            frontierOverlay.removeAllUserShared();
            frontierOverlay.removeChanges();
            frontierOverlay.recreateBannerRenderer();
            MapFrontiersClient.getFrontiersOverlayManager(false).addFrontier(frontierOverlay);
            ClientEventHandler.postUpdatedFrontierEvent(frontierOverlay, -1);
            frontierOverlay.updateOverlay();
        }
    }
}
