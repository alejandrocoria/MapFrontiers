package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_frontier");
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
            frontier.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketPersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().importPersonalFrontier(player, message.frontier);
            result.dispatchNetworkActions();
        }
    }
}
