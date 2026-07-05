package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_deleted");

    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private UUID frontierID;
    private boolean personal;
    private int playerID = -1;

    public PacketFrontierDeleted(ResourceKey<Level> dimension, UUID frontierID, boolean personal, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.personal = personal;
        this.playerID = playerID;
    }

    public PacketFrontierDeleted(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.personal = buf.readBoolean();
            this.playerID = buf.readInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension.location());
        UUIDHelper.toBytes(buf, frontierID);
        buf.writeBoolean(personal);
        buf.writeInt(playerID);
    }

    public static void handle(PacketContext<PacketFrontierDeleted> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierDeleted message = ctx.message();
            MapFrontiersClient.getOperationService().applyFrontierDeleted(message.dimension, message.frontierID, message.personal);
        }
    }
}
