package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierResync {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_resync");

    private final FrontierData frontier;

    public PacketFrontierResync(FrontierData frontier) {
        this.frontier = new FrontierData(frontier);
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

            MapFrontiersClient.getOperationService().applyFrontierResync(ctx.message().frontier);
        }
    }
}
