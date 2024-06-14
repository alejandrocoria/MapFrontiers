package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_personal_frontier");

    private final FrontierData frontier;

    public PacketPersonalFrontier() {
        frontier = new FrontierData();
    }

    public PacketPersonalFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public static PacketPersonalFrontier decode(FriendlyByteBuf buf) {
        PacketPersonalFrontier packet = new PacketPersonalFrontier();

        try {
            if (buf.readableBytes() > 1) {
                packet.frontier.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketPersonalFrontier: %s", t));
        }

        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            frontier.toBytes(buf, false);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketPersonalFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketPersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketPersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            SettingsUser playerUser = new SettingsUser(player);
            FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getId());

            if (currentFrontier == null && message.frontier.getPersonal() && message.frontier.getOwner().equals(playerUser)) {
                message.frontier.removeAllUserShared();
                message.frontier.removeChange(FrontierData.Change.Shared);

                FrontiersManager.instance.addPersonalFrontier(message.frontier);
            }
        }
    }
}
