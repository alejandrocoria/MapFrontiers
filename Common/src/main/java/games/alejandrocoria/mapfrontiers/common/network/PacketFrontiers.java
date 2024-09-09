package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketFrontiers {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontiers> STREAM_CODEC = StreamCodec.ofMember(PacketFrontiers::encode, PacketFrontiers::new);

    private final List<FrontierData> globalFrontiers;
    private final List<FrontierData> personalFrontiers;

    public PacketFrontiers() {
        globalFrontiers = new ArrayList<>();
        personalFrontiers = new ArrayList<>();
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
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

    public PacketFrontiers(FriendlyByteBuf buf) {
        globalFrontiers = new ArrayList<>();
        personalFrontiers = new ArrayList<>();

        try {
            if (buf.readableBytes() > 1) {
                int size = buf.readInt();
                for (int i = 0; i < size; ++i) {
                    FrontierData frontier = new FrontierData();
                    frontier.fromBytes(buf);
                    this.addGlobalFrontier(frontier);
                }

                size = buf.readInt();
                for (int i = 0; i < size; ++i) {
                    FrontierData frontier = new FrontierData();
                    frontier.fromBytes(buf);
                    this.addPersonalFrontier(frontier);
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontiers: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeInt(globalFrontiers.size());
            for (FrontierData frontier : globalFrontiers) {
                frontier.toBytes(buf, false);
            }

            buf.writeInt(personalFrontiers.size());
            for (FrontierData frontier : personalFrontiers) {
                frontier.toBytes(buf, false);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontiers: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontiers> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketFrontiers message = ctx.message();
            MapFrontiersClient.setFrontiersFromServer(message.globalFrontiers, message.personalFrontiers);
        }
    }
}
