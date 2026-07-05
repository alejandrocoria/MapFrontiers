package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_personal_frontier");

    private final FrontierData frontier;

    public PacketPersonalFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketPersonalFrontier(FriendlyByteBuf buf) {
        this.frontier = new FrontierData();
        if (buf.readableBytes() > 1) {
            this.frontier.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        frontier.toBytes(buf);
    }

    public static void handle(PacketContext<PacketPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().importPersonalFrontier(player, message.frontier);
            result.dispatchNetworkActions();
        }
    }
}
