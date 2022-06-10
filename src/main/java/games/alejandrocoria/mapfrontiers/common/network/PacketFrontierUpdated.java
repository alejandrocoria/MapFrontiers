package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;

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

    public static void handle(PacketFrontierUpdated message, Minecraft client) {
        client.execute(() -> {
            FrontierOverlay frontierOverlay = ClientProxy.getFrontiersOverlayManager(message.frontier.getPersonal())
                    .updateFrontier(message.frontier);

            if (frontierOverlay != null) {
                ClientProxy.postUpdatedFrontierEvent(frontierOverlay, message.playerID);
            }
        });
    }
}
