package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public class PacketFrontierUpdated {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_updated");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierUpdated> STREAM_CODEC = StreamCodec.ofMember(PacketFrontierUpdated::encode, PacketFrontierUpdated::new);

    private final FrontierData frontier;
    private final Set<FrontierData.Change> changes;
    private int playerID = -1;

    public PacketFrontierUpdated(FrontierData frontier) {
        this.frontier = frontier;
        changes = frontier.getChanges();
    }

    public PacketFrontierUpdated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        changes = frontier.getChanges();
        this.playerID = playerID;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierUpdated(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();
        this.changes = null;

        try {
            if (buf.readableBytes() > 1) {
                this.frontier.fromBytes(buf);
                this.playerID = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontierUpdated: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, changes);
            buf.writeInt(playerID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontierUpdated: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontierUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontierUpdated message = ctx.message();
            MapFrontiersClient.getCommandService().applyFrontierUpdated(message.frontier, message.playerID);
        }
    }
}
