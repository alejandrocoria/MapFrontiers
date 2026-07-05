package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketChangeFrontierToGlobal {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_change_frontier_to_global");

    private UUID frontierID;
    private Date modified;

    public PacketChangeFrontierToGlobal(UUID frontierID, @Nullable Date modified) {
        this.frontierID = frontierID;
        this.modified = modified;
    }

    public PacketChangeFrontierToGlobal(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            if (buf.readBoolean()) {
                modified = new Date(buf.readLong());
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        if (modified == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(modified.getTime());
        }
    }

    public static void handle(PacketContext<PacketChangeFrontierToGlobal> ctx) {
        PacketChangeFrontierToGlobal message = ctx.message();

        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }
            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getOperationService().changeFrontierToGlobal(player, message.frontierID);
            result.dispatchNetworkActions();
        } else {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            MapFrontiersClient.getOperationService().applyFrontierChangeToGlobal(message.frontierID, message.modified);
        }
    }
}
