package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted {
    private RegistryKey<World> dimension = World.OVERWORLD;
    private UUID frontierID;
    private boolean personal;
    private int playerID = -1;

    public PacketFrontierDeleted() {

    }

    public PacketFrontierDeleted(RegistryKey<World> dimension, UUID frontierID, boolean personal, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.personal = personal;
        this.playerID = playerID;
    }

    public static PacketFrontierDeleted fromBytes(PacketBuffer buf) {
        PacketFrontierDeleted packet = new PacketFrontierDeleted();
        packet.dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        packet.frontierID = UUIDHelper.fromBytes(buf);
        packet.personal = buf.readBoolean();
        packet.playerID = buf.readInt();
        return packet;
    }

    public static void toBytes(PacketFrontierDeleted packet, PacketBuffer buf) {
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
