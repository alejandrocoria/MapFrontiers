package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontier {
    private final FrontierData frontier;

    public PacketPersonalFrontier() {
        frontier = new FrontierData();
    }

    public PacketPersonalFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public static PacketPersonalFrontier fromBytes(FriendlyByteBuf buf) {
        PacketPersonalFrontier packet = new PacketPersonalFrontier();
        packet.frontier.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketPersonalFrontier packet, FriendlyByteBuf buf) {
        packet.frontier.toBytes(buf, false);
    }

    public static void handle(PacketPersonalFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
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
        });

        context.setPacketHandled(true);
    }
}
