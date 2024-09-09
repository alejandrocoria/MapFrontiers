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
public class PacketSharePersonalFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_share_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSharePersonalFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketSharePersonalFrontier::encode, PacketSharePersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUser targetUser;

    public PacketSharePersonalFrontier() {
        targetUser = new SettingsUser();
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketSharePersonalFrontier(FriendlyByteBuf buf) {
        this.targetUser = new SettingsUser();

        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                this.targetUser.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketSharePersonalFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            targetUser.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketSharePersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketSharePersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketSharePersonalFrontier message = ctx.message();
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

            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(message.targetUser.uuid);
            if (targetPlayer == null) {
                return;
            }

            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (currentFrontier.getOwner().equals(message.targetUser) || currentFrontier.hasUserShared(message.targetUser)) {
                    return;
                }

                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, playerUser,
                        MapFrontiers.isOPorHost(player), currentFrontier.getOwner())) {
                    if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateSettings)) {
                        int shareMessageID = FrontiersManager.instance.addShareMessage(message.targetUser,
                                currentFrontier.getId());

                        currentFrontier.addUserShared(new SettingsUserShared(message.targetUser, true));

                        PacketHandler.sendTo(new PacketPersonalFrontierShared(shareMessageID, playerUser,
                                currentFrontier.getOwner(), currentFrontier.getName1(), currentFrontier.getName2()), targetPlayer);

                        currentFrontier.removeChange(FrontierData.Change.Shared);
                    }
                } else {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                }
            }
        }
    }
}
