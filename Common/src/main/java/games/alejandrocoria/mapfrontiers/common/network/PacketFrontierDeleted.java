package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
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

    public PacketFrontierDeleted() {

    }

    public PacketFrontierDeleted(ResourceKey<Level> dimension, UUID frontierID, boolean personal, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.personal = personal;
        this.playerID = playerID;
    }

    public static PacketFrontierDeleted decode(FriendlyByteBuf buf) {
        PacketFrontierDeleted packet = new PacketFrontierDeleted();
        packet.dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.personal = buf.readBoolean();
        packet.playerID = buf.readInt();
        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension.location());
        UUIDHelper.toBytes(buf, frontierID);
        buf.writeBoolean(personal);
        buf.writeInt(playerID);
    }

    public static void handle(PacketContext<PacketFrontierDeleted> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontierDeleted message = ctx.message();
            boolean deleted = MapFrontiersClient.getFrontiersOverlayManager(message.personal).deleteFrontier(message.dimension,message.frontierID);

            if (deleted) {
                ClientEventHandler.postDeletedFrontierEvent(message.frontierID);
            }
        }
    }
}
