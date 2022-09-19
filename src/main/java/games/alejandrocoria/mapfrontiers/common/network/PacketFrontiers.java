package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketFrontiers {
    private final List<FrontierData> globalFrontiers;
    private final List<FrontierData> personalFrontiers;

    public PacketFrontiers() {
        globalFrontiers = new ArrayList<>();
        personalFrontiers = new ArrayList<>();
    }

    public void addGlobalFrontier(FrontierData frontier) {
        globalFrontiers.add(frontier);
    }

    public void addPersonalFrontier(FrontierData frontier) {
        personalFrontiers.add(frontier);
    }

    public void addGlobalFrontiers(List<FrontierData> frontiers) {
        globalFrontiers.addAll(frontiers);
    }

    public void addPersonalFrontiers(List<FrontierData> frontiers) {
        personalFrontiers.addAll(frontiers);
    }

    public static PacketFrontiers fromBytes(FriendlyByteBuf buf) {
        PacketFrontiers packet = new PacketFrontiers();

        int size = buf.readInt();
        for (int i = 0; i < size; ++i) {
            FrontierData frontier = new FrontierData();
            frontier.fromBytes(buf);
            packet.addGlobalFrontier(frontier);
        }

        size = buf.readInt();
        for (int i = 0; i < size; ++i) {
            FrontierData frontier = new FrontierData();
            frontier.fromBytes(buf);
            packet.addPersonalFrontier(frontier);
        }

        return packet;
    }

    public static void toBytes(PacketFrontiers packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.globalFrontiers.size());
        for (FrontierData frontier : packet.globalFrontiers) {
            frontier.toBytes(buf, false);
        }

        buf.writeInt(packet.personalFrontiers.size());
        for (FrontierData frontier : packet.personalFrontiers) {
            frontier.toBytes(buf, false);
        }
    }

    public static void handle(PacketFrontiers message, Minecraft client) {
        client.execute(() -> {
            ClientProxy.setFrontiersFromServer(message.globalFrontiers, message.personalFrontiers);
        });
    }
}
