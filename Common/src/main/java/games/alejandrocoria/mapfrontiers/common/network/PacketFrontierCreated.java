package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierCreated {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_created");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierCreated> STREAM_CODEC = StreamCodec.ofMember(PacketFrontierCreated::encode, PacketFrontierCreated::new);

    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierCreated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierCreated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierCreated(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();

        try {
            if (buf.readableBytes() > 1) {
                this.frontier.fromBytes(buf);
                this.playerID = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontierCreated: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, false);
            buf.writeInt(playerID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontierCreated: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontierCreated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontierCreated message = ctx.message();
            FrontierOverlay frontierOverlay = MapFrontiersClient.getFrontiersOverlayManager(message.frontier.getPersonal()).addFrontier(message.frontier);

            if (frontierOverlay != null) {
                ClientEventHandler.postNewFrontierEvent(frontierOverlay, message.playerID);
            }
        }
    }
}
