package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierResync implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_resync");
    public static final CustomPacketPayload.Type<PacketFrontierResync> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierResync> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFrontierResync::encode, PacketFrontierResync::new);

    private final FrontierData frontier;

    public PacketFrontierResync(FrontierData frontier) {
        this.frontier = new FrontierData(frontier);
    }

    @Override
    public CustomPacketPayload.Type<PacketFrontierResync> type() {
        return TYPE;
    }

    public PacketFrontierResync(FriendlyByteBuf buf) {
        frontier = new FrontierData();
        if (buf.readableBytes() > 1) {
            frontier.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        frontier.toBytes(buf);
    }

    public static void handle(PacketContext<PacketFrontierResync> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierResync message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.getOperationService().applyFrontierResync(message.frontier));
        }
    }
}
