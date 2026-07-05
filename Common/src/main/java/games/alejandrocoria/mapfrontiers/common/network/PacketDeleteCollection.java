package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketDeleteCollection {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_delete_collection");

    private UUID collectionId;

    public PacketDeleteCollection(UUID collectionId) {
        this.collectionId = collectionId;
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
