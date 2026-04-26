package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketCreateCollection {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_create_collection");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateCollection> STREAM_CODEC = StreamCodec.ofMember(PacketCreateCollection::encode, PacketCreateCollection::new);

    private final CollectionData collection;

    public PacketCreateCollection(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketCreateCollection(FriendlyByteBuf buf) {
        this.collection = new CollectionData();

        try {
            if (buf.readableBytes() > 1) {
                this.collection.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to read message for PacketCreateCollection", t);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            collection.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to write message for PacketCreateCollection", t);
        }
    }

    public static void handle(PacketContext<PacketCreateCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketCreateCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().createCollection(player, message.collection);
            result.dispatchNetworkActions();
        }
    }
}
