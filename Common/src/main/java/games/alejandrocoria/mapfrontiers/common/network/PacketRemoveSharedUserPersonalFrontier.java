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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRemoveSharedUserPersonalFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_remove_shared_user_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRemoveSharedUserPersonalFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketRemoveSharedUserPersonalFrontier::encode, PacketRemoveSharedUserPersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUser targetUser;

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketRemoveSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.targetUser = new SettingsUser();

        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                this.targetUser.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketRemoveSharedUserPersonalFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            targetUser.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketRemoveSharedUserPersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketRemoveSharedUserPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRemoveSharedUserPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
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
                                PacketHandler.sendTo(new PacketFrontierDeleted(currentFrontier.getDimension(), message.frontierID,
                                        true, -1), targetPlayer);
                            }
                        }

                        PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(currentFrontier, player.getId()), currentFrontier, server);

                        currentFrontier.removeChange(FrontierData.Change.Shared);
                    }
                } else {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                }
            }
        }
    }
}
