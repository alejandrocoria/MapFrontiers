package games.alejandrocoria.mapfrontiers.common.network;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted {
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

    public static PacketFrontierDeleted fromBytes(FriendlyByteBuf buf) {
        PacketFrontierDeleted packet = new PacketFrontierDeleted();
        packet.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.personal = buf.readBoolean();
        packet.playerID = buf.readInt();
        return packet;
    }

    public static void toBytes(PacketFrontierDeleted packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension.location());
        UUIDHelper.toBytes(buf, packet.frontierID);
        buf.writeBoolean(packet.personal);
        buf.writeInt(packet.playerID);
    }

    public static void handle(PacketFrontierDeleted message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFrontierDeleted message, NetworkEvent.Context ctx) {
        boolean deleted = ClientProxy.getFrontiersOverlayManager(message.personal).deleteFrontier(message.dimension,message.frontierID);

        if (deleted) {
            MinecraftForge.EVENT_BUS.post(new DeletedFrontierEvent(message.frontierID));
        }

        ctx.setPacketHandled(true);
    }
}
