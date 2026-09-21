package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierCreated {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_created");

    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierCreated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierCreated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public PacketFrontierCreated(FriendlyByteBuf buf) {
        this.frontier = FrontierData.fromBytes(buf);
        if (buf.readableBytes() > 0) {
            this.playerID = buf.readInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        frontier.toBytes(buf);
        buf.writeInt(playerID);
    }

    public static void handle(PacketContext<PacketFrontierCreated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierCreated message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.getOperationService()
                    .applyFrontierCreated(message.frontier, message.playerID));
        }
    }
}
