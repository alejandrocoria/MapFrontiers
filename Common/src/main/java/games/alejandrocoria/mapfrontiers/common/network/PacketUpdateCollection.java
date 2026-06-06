package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketUpdateCollection {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_update_collection");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateCollection> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketUpdateCollection::encode, PacketUpdateCollection::new);

    private final CollectionData collection;

    public PacketUpdateCollection(CollectionData collection) {
        this.collection = new CollectionData(collection);
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketUpdateCollection(FriendlyByteBuf buf) {
        this.collection = new CollectionData();
        if (buf.readableBytes() > 1) {
            this.collection.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        collection.toBytes(buf);
    }

    public static void handle(PacketContext<PacketUpdateCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketUpdateCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService()
                    .updateCollection(player, message.collection.getId(), message.collection);
            result.dispatchNetworkActions();
        }
    }
}
