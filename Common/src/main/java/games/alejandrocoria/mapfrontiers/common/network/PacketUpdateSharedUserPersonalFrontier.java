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
public class PacketUpdateSharedUserPersonalFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_shared_user_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateSharedUserPersonalFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketUpdateSharedUserPersonalFrontier::encode, PacketUpdateSharedUserPersonalFrontier::new);

    private UUID frontierID;
    private final SettingsUserShared userShared;

    public PacketUpdateSharedUserPersonalFrontier(UUID frontierID, SettingsUserShared user) {
        this.frontierID = frontierID;
        userShared = user;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketUpdateSharedUserPersonalFrontier(FriendlyByteBuf buf) {
        this.userShared = new SettingsUserShared();

        try {
            if (buf.readableBytes() > 1) {
                this.frontierID = UUIDHelper.fromBytes(buf);
                this.userShared.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketUpdateSharedUserPersonalFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            UUIDHelper.toBytes(buf, frontierID);
            userShared.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketUpdateSharedUserPersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketUpdateSharedUserPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketUpdateSharedUserPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

            if (currentFrontier != null && currentFrontier.getPersonal()) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, playerUser,
                        MapFrontiers.isOPorHost(player), currentFrontier.getOwner())) {
                    SettingsUserShared currentUserShared = currentFrontier.getUserShared(message.userShared.getUser());

                    if (currentUserShared == null) {
                        return;
                    }

                    currentUserShared.setActions(message.userShared.getActions());
                    currentFrontier.addChange(FrontierData.Change.Shared);

                    PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(currentFrontier, player.getId()), currentFrontier, server);
                } else {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                }
            }
        }
    }
}
