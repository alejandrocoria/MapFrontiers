package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPersonalFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketPersonalFrontier::encode, PacketPersonalFrontier::new);

    private final FrontierData frontier;

    public PacketPersonalFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketPersonalFrontier(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();

        try {
            if (buf.readableBytes() > 1) {
                this.frontier.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketPersonalFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, false);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketPersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getId());

            if (currentFrontier == null && message.frontier.getPersonal() && message.frontier.getOwner().equals(playerUser)) {
                message.frontier.removeAllUserShared();
                message.frontier.removeChange(FrontierData.Change.Shared);

                FrontiersManager.instance.addPersonalFrontier(message.frontier);
            }
        }
    }
}
