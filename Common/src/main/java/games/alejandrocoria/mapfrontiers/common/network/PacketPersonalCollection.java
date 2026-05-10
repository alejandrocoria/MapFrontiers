package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalCollection {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_collection");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPersonalCollection> STREAM_CODEC = StreamCodec.ofMember(PacketPersonalCollection::encode, PacketPersonalCollection::new);

    private final CollectionData collection;

    public PacketPersonalCollection(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketPersonalCollection(FriendlyByteBuf buf) {
        this.collection = new CollectionData();

        try {
            if (buf.readableBytes() > 1) {
                this.collection.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to read message for PacketPersonalCollection", t);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            collection.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to write message for PacketPersonalCollection", t);
        }
    }

    public static void handle(PacketContext<PacketPersonalCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().importPersonalCollection(player, message.collection);
            result.dispatchNetworkActions();
        }
    }
}
