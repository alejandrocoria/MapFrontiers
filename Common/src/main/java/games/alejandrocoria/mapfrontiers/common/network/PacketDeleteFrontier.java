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
public class PacketDeleteFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_delete_frontier");

    private UUID frontierID;

    public PacketDeleteFrontier(UUID frontierID) {
        this.frontierID = frontierID;
    }

    public PacketDeleteFrontier(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
    }

    public static void handle(PacketContext<PacketDeleteFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketDeleteFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().deleteFrontier(player, message.frontierID);
            result.dispatchNetworkActions();
        }
    }
}
