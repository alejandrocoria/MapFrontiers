package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFullFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_full_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFullFrontier> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFullFrontier::encode, PacketFullFrontier::new);

    private final FrontierData frontier;

    public PacketFullFrontier(FrontierData frontier) {
        this.frontier = new FrontierData(frontier);
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFullFrontier(FriendlyByteBuf buf) {
        frontier = new FrontierData();
        if (buf.readableBytes() > 1) {
            frontier.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        frontier.toBytes(buf);
    }

    public static void handle(PacketContext<PacketFullFrontier> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }

            MapFrontiersClient.getOperationService().applyFrontierResync(ctx.message().frontier);
        }
    }
}
