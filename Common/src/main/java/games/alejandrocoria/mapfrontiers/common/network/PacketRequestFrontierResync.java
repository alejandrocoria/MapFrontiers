package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierResync {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_request_frontier_resync");

    private UUID frontierId = new UUID(0, 0);

    public PacketRequestFrontierResync(UUID frontierId) {
        this.frontierId = frontierId;
    }

    public PacketRequestFrontierResync(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            frontierId = buf.readUUID();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(frontierId);
    }

    public static void handle(PacketContext<PacketRequestFrontierResync> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketRequestFrontierResync message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null || MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService()
                    .requestFrontierResync(player, message.frontierId);
            result.dispatchNetworkActions();
        }
    }
}
