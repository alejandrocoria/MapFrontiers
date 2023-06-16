package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketFrontiers {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier");

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

    public static PacketFrontiers decode(FriendlyByteBuf buf) {
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

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(globalFrontiers.size());
        for (FrontierData frontier : globalFrontiers) {
            frontier.toBytes(buf, false);
        }

        buf.writeInt(personalFrontiers.size());
        for (FrontierData frontier : personalFrontiers) {
            frontier.toBytes(buf, false);
        }
    }

    public static void handle(PacketContext<PacketFrontiers> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontiers message = ctx.message();
            MapFrontiersClient.setFrontiersFromServer(message.globalFrontiers, message.personalFrontiers);
        }
    }
}
