package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketFrontierUpdated {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_updated");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierUpdated> STREAM_CODEC = StreamCodec.ofMember(PacketFrontierUpdated::encode, PacketFrontierUpdated::new);

    private UUID frontierId = new UUID(0, 0);
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean personal;
    private FrontierChange change = new FrontierChange();
    private int playerID = -1;

    public PacketFrontierUpdated(UUID frontierId, ResourceKey<Level> dimension, boolean personal, FrontierChange change) {
        this(frontierId, dimension, personal, change, -1);
    }

    public PacketFrontierUpdated(UUID frontierId, ResourceKey<Level> dimension, boolean personal, FrontierChange change, int playerID) {
        this.frontierId = frontierId;
        this.dimension = dimension;
        this.personal = personal;
        this.change = change;
        this.playerID = playerID;
    }

    public PacketFrontierUpdated(FrontierData frontier) {
        this(frontier, -1);
    }

    public PacketFrontierUpdated(FrontierData frontier, int playerID) {
        frontierId = frontier.getId();
        dimension = frontier.getDimension();
        personal = frontier.getPersonal();
        change = FrontierChange.fromFrontierData(frontier, true);
        this.playerID = playerID;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierUpdated(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.frontierId = buf.readUUID();
                this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
                this.personal = buf.readBoolean();
                this.change = new FrontierChange(buf);
                this.playerID = buf.readInt();
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontierUpdated: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeUUID(frontierId);
            buf.writeIdentifier(dimension.identifier());
            buf.writeBoolean(personal);
            change.toBytes(buf);
            buf.writeInt(playerID);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontierUpdated: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontierUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierUpdated message = ctx.message();
            MapFrontiersClient.getOperationService().applyFrontierUpdated(message.dimension, message.frontierId, message.personal, message.change,
                    message.playerID);
        }
    }
}
