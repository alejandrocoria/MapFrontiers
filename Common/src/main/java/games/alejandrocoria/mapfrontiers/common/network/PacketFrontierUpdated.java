package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierUpdated {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_updated");

    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierUpdated() {
        frontier = new FrontierData();
    }

    public PacketFrontierUpdated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierUpdated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public static PacketFrontierUpdated decode(FriendlyByteBuf buf) {
        PacketFrontierUpdated packet = new PacketFrontierUpdated();

        try {
            if (buf.readableBytes() > 1) {
                packet.frontier.fromBytes(buf);
                packet.playerID = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontierUpdated: %s", t));
        }

        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf);
            buf.writeInt(playerID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontierUpdated: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontierUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontierUpdated message = ctx.message();
            FrontierOverlay frontierOverlay = MapFrontiersClient.getFrontiersOverlayManager(message.frontier.getPersonal())
                    .updateFrontier(message.frontier);

            if (frontierOverlay != null) {
                ClientEventHandler.postUpdatedFrontierEvent(frontierOverlay, message.playerID);
            }
        }
    }
}
