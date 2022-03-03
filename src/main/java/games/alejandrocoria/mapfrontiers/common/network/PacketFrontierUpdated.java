package games.alejandrocoria.mapfrontiers.common.network;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

@ParametersAreNonnullByDefault
public class PacketFrontierUpdated {
    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierUpdated() {
        frontier = new FrontierData();
    }

    public PacketFrontierUpdated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierUpdated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public static PacketFrontierUpdated fromBytes(FriendlyByteBuf buf) {
        PacketFrontierUpdated packet = new PacketFrontierUpdated();
        packet.frontier.fromBytes(buf);
        packet.playerID = buf.readInt();
        return packet;
    }

    public static void toBytes(PacketFrontierUpdated packet, FriendlyByteBuf buf) {
        packet.frontier.toBytes(buf);
        buf.writeInt(packet.playerID);
    }

    public static void handle(PacketFrontierUpdated message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFrontierUpdated message, NetworkEvent.Context ctx) {
        FrontierOverlay frontierOverlay = ClientProxy.getFrontiersOverlayManager(message.frontier.getPersonal())
                .updateFrontier(message.frontier);

        if (frontierOverlay != null) {
            MinecraftForge.EVENT_BUS.post(new UpdatedFrontierEvent(frontierOverlay, message.playerID));
        }

        ctx.setPacketHandled(true);
    }
}
