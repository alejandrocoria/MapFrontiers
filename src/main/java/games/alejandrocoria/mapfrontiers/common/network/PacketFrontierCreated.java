package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierCreated {
    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierCreated() {
        frontier = new FrontierData();
    }

    public PacketFrontierCreated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierCreated(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public static PacketFrontierCreated fromBytes(FriendlyByteBuf buf) {
        PacketFrontierCreated packet = new PacketFrontierCreated();
        packet.frontier.fromBytes(buf);
        packet.playerID = buf.readInt();
        return packet;
    }

    public static void toBytes(PacketFrontierCreated packet, FriendlyByteBuf buf) {
        packet.frontier.toBytes(buf, false);
        buf.writeInt(packet.playerID);
    }

    @Environment(EnvType.CLIENT)
    public static void handle(PacketFrontierCreated message, Minecraft client) {
        client.execute(() -> {
            FrontierOverlay frontierOverlay = ClientProxy.getFrontiersOverlayManager(message.frontier.getPersonal()).addFrontier(message.frontier);

            if (frontierOverlay != null) {
                ClientProxy.postNewFrontierEvent(frontierOverlay, message.playerID);
            }
        });
    }
}
