package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketDeleteCollection implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_delete_collection");
    public static final CustomPacketPayload.Type<PacketDeleteCollection> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketDeleteCollection> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketDeleteCollection::encode, PacketDeleteCollection::new);

    private UUID collectionId;

    public PacketDeleteCollection(UUID collectionId) {
        this.collectionId = collectionId;
    }

    @Override
    public CustomPacketPayload.Type<PacketDeleteCollection> type() {
        return TYPE;
    }

    public PacketDeleteCollection(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.collectionId = UUIDHelper.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, collectionId);
    }

    public static void handle(PacketContext<PacketDeleteCollection> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketDeleteCollection message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().deleteCollection(player, message.collectionId);
            result.dispatchNetworkActions();
        }
    }
}
